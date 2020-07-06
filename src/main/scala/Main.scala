import org.apache.log4j.{Level, Logger}
import utils.SessionUtils

object Main {

  /**
    * Spawn the process
    */
  def main(args: Array[String]): Unit = {
    // Set the logging pattern
    Logger.getLogger("org.apache").setLevel(Level.WARN)

    // Create spark session
    val spark = SessionUtils.provideSparkSession()

    // These could be application args but left them here for the demo
    val KAFKA_BROKERS = "localhost:9092"
    val SCHEMA_REGISTRY_SERVICE = "http://localhost:8081"

    // Run the stream aggregator
    new StreamAggregatorDemo(spark, KAFKA_BROKERS, SCHEMA_REGISTRY_SERVICE).executePipeline()
  }
}
