package utils

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * This object contains utility functions
  * that are destined to provide for a local demo run of the application
  */
object SessionUtils {
  /**
    * A method that provides a local spark session
    * @return Spark Session object
    */
  def provideSparkSession(): SparkSession = {
    val session = SparkSession
      .builder().master("local[*]")
      .appName("KafkaSparkStreamAggregator")
      .getOrCreate()

    // I'm reducing the default shuffle partitions of 200 to 2
    // to optimize the processing time of the micro-batches
    session.conf.set("spark.sql.shuffle.partitions", 2)
    // Go with the pace of the faster stream
    session.conf.set("spark.sql.streaming.multipleWatermarkPolicy", "max")
    session
  }

  /**
    * A method that provides a spark-kafka source stream
    * @param spark  spark session
    * @param kafkaBrokers comma-separated list of kafka brokers we want to connect to
    * @param topicName the kafka source topic name
    * @return a Spark Streaming DataFrame
    */
  def provideKafkaSourceStream(spark: SparkSession, kafkaBrokers:  String)(topicName: String): DataFrame = {
    spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBrokers)
      .option("subscribe", topicName)
      .load()
  }
}
