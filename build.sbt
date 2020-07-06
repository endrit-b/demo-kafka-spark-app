import sbt.Keys.version

lazy val root = (project in file(".")).
  settings(
    name := "demo-kafka-spark-app",
    version := "0.0.1",
    organization := "io.endrit.sample",
    scalaVersion := "2.11.11",
    scalacOptions += "-target:jvm-1.8",
    mainClass in Compile := Some("io.endrit.sample.Main")
  )

val sparkVersion = "2.4.6"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  // For this demo, I'm adding this utility just as a quick way
  // to handle avro schema parsing from kafka confluent-standard
  "za.co.absa" %% "abris" % "3.2.1"
)

// Esure the keeping of this Jackson version for Spark
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7"
)


assemblyMergeStrategy in assembly := {
  case "META-INF/services/org.apache.spark.sql.sources.DataSourceRegister" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}