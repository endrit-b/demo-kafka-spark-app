Kafka-Spark end-to-end Demo App
---

__About the Assignment:__

* Download the Confluent Platform Quick Start and set it up
* Create three topics: users, pageviews and top_pages
* Use the Kafka Connect DataGenConnector to produce data into these topics, using the users_extended and pageview quickstarts
* Use a stream processing engine other than KSQL to develop a processor that:
    - Joins the messages in these two topics on the user id field
    - Uses a 1 minute hopping window with 10 second advances to compute the 10 most viewed pages by viewtime for every value of gender
    - Once per minute produces a message into the top_pages topic that contains the gender, pageid, sum of view time in the latest window and distinct count of user ids in the latest window

Prerequisites
---

To be able to run this app, make sure you have installed the following:
* Scala v2.11.11
* Sbt v1.3.13 (Scala build tool)
* Kafka v0.10+
* Spark 2.4.x
* Use Kafka Connect DataGenConnector to produce data using the users and pageview quickstarts
* Also make sure the output `top_pages` Avro schema looks like below:
    ```json
      {
        "name": "value_top_pages",
        "namespace": "com.mycorp.mynamespace",
        "type": "record",
        "fields": [
          {
            "default": null,
            "name": "gender",
            "type": ["null", "string"]
          },
          {
            "default": null,
            "name": "pageid",
            "type": ["null", "string"]
          },
          {
            "default": null,
            "name": "viewtime_sum",
            "type": ["null", "long"]
          },
          {
            "default": 0,
            "name": "users_count",
            "type": "long"
          }
        ]
      }
    ```


Installation and Usage
---

Once you install the prerequisites, just go ahead and clone the repository, then run commands below (on the project root):
```bash
# Prepare the fat jar for spark-submit
sbt assembly
```

After that run command:
```bash
 spark-submit --class io.endrit.sample.Main target/scala-2.11/demo-kafka-spark-app-assembly-0.0.1.jar
```
