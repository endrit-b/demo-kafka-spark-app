
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{approx_count_distinct, col, dense_rank, expr, rank, struct, sum, window}
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.expressions.Window
import utils.{SchemaUtils, SessionUtils}
import za.co.absa.abris.avro.functions.{from_confluent_avro, to_confluent_avro}

import scala.concurrent.duration._

class StreamAggregatorDemo(spark: SparkSession, kafkaBrokers: String, schemaRegistryURL: String) {

  /**
    * Merges and executes a spark transformation job on two kafka streams
    * and outputs the result to another kafka topic-sink
    */
  def executePipeline(): Unit = {

    // Kafka stream providers
    val kafkaStreamsProvider = SessionUtils.provideKafkaSourceStream(spark, kafkaBrokers)(_)
    // Start reading events from kafka topics: 'pageviews'
    val pageViewsStream: DataFrame = kafkaStreamsProvider(Constants.PAGE_VIEWS_TOPIC)
      .transform(this.projectStreamDataFrame(Constants.PAGE_VIEWS_TOPIC))

    // Start reading events from kafka topics: 'users'
    val usersStream: DataFrame = kafkaStreamsProvider(Constants.USERS_TOPIC)
      .transform(this.projectStreamDataFrame(Constants.USERS_TOPIC))

    // Merge pageviews and users streams and calculate window-based metrics
    val mergedStreams = this.joinAndAggregateStreams(pageViewsStream, usersStream)

    // Star pipeline - handle micro-batches and write results to the kafka sink
    mergedStreams.writeStream
      .outputMode(OutputMode.Append())
      .foreachBatch{this.microBatchHandler}
      // Once per minute produces a message into the top_pages topic that contains
      // the gender, pageid, sum of view time in the latest window and distinct count of user ids in the latest window
      .trigger(Trigger.ProcessingTime(1.minute))
      .start()
      .awaitTermination()
  }

  /**
    * Parses Avro data to a Spark dataframe and sets the watemarking for the given stream
    */
  def projectStreamDataFrame(topic: String)(dataFrame: DataFrame): DataFrame = {
    // Get Avro schema for Kafka topic from registry service
    val pageViewsConfig =  SchemaUtils.getTopicSchemaConfig(topic, schemaRegistryURL)
    val newTimestampColName = topic + "_" +  Constants.TIMESTAMP
    val newUserIdColName = topic + "_" +  Constants.USER_ID

    // Here we read and parse Avro schema to a Spark Data Frame
    // Also rename some columns to topic specific naming
    // Lastly, I define watermarking - a Spark feature used for handling late data - for this demo I set it to 10 seconds
    dataFrame
      .select(col(Constants.TIMESTAMP), from_confluent_avro(col(Constants.VALUE), pageViewsConfig).as(Constants.EVENT))
      .select(Constants.TIMESTAMP,"event.*")
      .withColumnRenamed(Constants.TIMESTAMP, newTimestampColName)
      .withColumnRenamed(Constants.USER_ID, newUserIdColName)
      .withWatermark(newTimestampColName, "10 seconds")
  }

  /**
    * Merge pageviews and users streams and calculate window based metrics
    */
  def joinAndAggregateStreams(pageViewsStream: DataFrame, usersStream: DataFrame): DataFrame = {

    // Here we define the join condition based on user id
    // I also add the range (time interval) constraints to decide how long data will
    // be preserved in Spark state in correlation to the other stream.
    val joinCondition = s"""
      | ${Constants.PGV_USER_ID} = ${Constants.USERS_USER_ID} AND
      | ${Constants.USERS_TIMESTAMP} >= ${Constants.PGV_TIMESTAMP} - interval 20 seconds AND
      | ${Constants.USERS_TIMESTAMP} <= ${Constants.PGV_TIMESTAMP} + interval 20 seconds
      """.stripMargin

    pageViewsStream
      // Left join on pageviews stream
      .join(usersStream, expr(joinCondition), "left_outer")
      .groupBy(
        col(Constants.GENDER), col(Constants.PAGE_ID),
        // Use a 1 minute hopping window with 10 seconds sliding (advances)
        window(col(Constants.PGV_TIMESTAMP), "1 minutes", "10 seconds"))
      // Calculate these metrics per window, gender, and pageid -
      // we find the ranking a bit later since spark doesn't allow consequent aggregations in the same stream
      .agg(sum(Constants.VIEW_TIME).as(Constants.VIEW_TIME_SUM), approx_count_distinct(Constants.PGV_USER_ID).as(Constants.USERS_COUNT))
      // Get all columns, plus unbox window start/end time interval
      .select("*", "window.*").drop("window")
  }

  /**
    * We pass this method as a handler to Spark's 'foreachBatch(...)' function to do the final step of
    * making sure the batch contains only data from latest window,
    * then we find the top 10 most view pages and count's  by gender
    * @return
    */
  def microBatchHandler: (DataFrame, Long) => Unit = (microBatchDF: DataFrame, microBatchId: Long) => {
      microBatchDF
        .withColumn(Constants.WINDOW_RANK, rank().over(Window.partitionBy(Constants.GENDER, Constants.PAGE_ID).orderBy(col("end").desc)))
        .where(col(Constants.WINDOW_RANK).equalTo(1))
        // Find the 10 most viewed pages by viewtime for every value of gender
        .withColumn(Constants.GENDER_RANK, dense_rank().over(Window.partitionBy(Constants.GENDER).orderBy(col(Constants.VIEW_TIME_SUM).desc)))
        .where(col(Constants.GENDER_RANK).leq(10))
        .sort(col(Constants.GENDER), col(Constants.VIEW_TIME_SUM).desc, col(Constants.GENDER_RANK).desc)
        .select(Constants.OUTPUT_COLUMNS.map(col):_*)
        .transform(dataToAvroFormat)
        .write
          // Finally write to Kafka
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaBrokers)
          .option("topic", Constants.TOP_PAGES_TOPIC)
          .save()
  }

  def dataToAvroFormat(dataFrame: DataFrame): DataFrame = {
    // Fetch Avro schema for top_ages Kafka topic
    val topPagesConfig =  SchemaUtils.getTopicSchemaConfig(Constants.TOP_PAGES_TOPIC, schemaRegistryURL)
    val allColumns = struct(dataFrame.columns.head, dataFrame.columns.tail: _*)
    dataFrame.select(to_confluent_avro(allColumns, topPagesConfig) as 'value)
  }

}

