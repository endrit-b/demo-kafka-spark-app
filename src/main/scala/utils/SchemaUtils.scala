package utils

import za.co.absa.abris.avro.read.confluent.SchemaManager

/**
  * I'll keep the Kafka Topics schemas in this utils object - just for demo purposes
  */
object SchemaUtils {

  def getTopicSchemaConfig(topicName: String, registryServiceUrl: String): Map[String, String] = Map(
    SchemaManager.PARAM_SCHEMA_REGISTRY_URL -> registryServiceUrl,
    SchemaManager.PARAM_SCHEMA_REGISTRY_TOPIC -> topicName,
    SchemaManager.PARAM_VALUE_SCHEMA_NAMING_STRATEGY -> "topic.name",
    SchemaManager.PARAM_VALUE_SCHEMA_ID -> "latest")
}
