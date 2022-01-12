Notes :

# Start the ZooKeeper service
# Note: Soon, ZooKeeper will no longer be required by Apache Kafka.
$ bin/zookeeper-server-start.sh config/zookeeper.properties

# Start the Kafka broker service
$ bin/kafka-server-start.sh config/server.properties

# Create topic
$ bin/kafka-topics.sh --create --partitions 1 --replication-factor 1 --topic quickstart-events --bootstrap-server localhost:9092

# Details on topic
$ bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
Output :
Topic:quickstart-events  PartitionCount:1    ReplicationFactor:1 Configs:
    Topic: quickstart-events Partition: 0    Leader: 0   Replicas: 0 Isr: 0

# Link Used for this project
https://github.com/mrsushil118/Kafka-Avro-scala

# List all topics
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# delete a topic
bin/kafka-topics.sh --topic discount-profile-by-user --bootstrap-server localhost:9092 --delete
bin/kafka-topics.sh --topic discounts --bootstrap-server localhost:9092 --delete

# create topic with cleanup policy compact

bin/kafka-topics.sh --create --partitions 1 --replication-factor 1 --topic discount-profile-by-user --config "cleanup.policy=compact" --bootstrap-server localhost:9092

#GLobal
bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic discounts --config "cleanup.policy=compact" --partitions 1 --replication-factor 1 --create



kafka-console-producer --topic discounts --broker-list localhost:9092 --property parse.key=true --property key.separator=,
profile1,{"profile":"profile1","amount":10}
profile2,{"profile":"profile2","amount":100}
profile3,{"profile":"profile3","amount":40}


bin/kafka-console-producer.sh --topic discount-profile-by-user --broker-list localhost:9092 --property parse.key=true --property key.separator=,
ABC,profile1
XYZ,profile2

bin/kafka-console-producer.sh --topic order-by-user --broker-list localhost:9092 --property parse.key=true --property key.separator=,
ABC,{"orderId":"order1","user":"ABC","products":["iPhone 13","MacBook Pro 15"],"amount":4000.0 }
XYZ,{"orderId":"order2","user":"XYZ","products":["iPhone 11"],"amount":800.0 }

bin/kafka-console-producer.sh --topic payments --broker-list localhost:9092 --property parse.key=true --property key.separator=,
order1,{"orderId":"order1","status":"PAID" }
order2,{"orderId":"order2","status":"PENDING" }

bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic paid-orders