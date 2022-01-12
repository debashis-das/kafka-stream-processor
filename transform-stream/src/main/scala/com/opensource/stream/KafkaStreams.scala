package com.opensource.stream

import com.opensource.stream.KafkaStreams.Topics._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.kstream.{GlobalKTable, JoinWindows}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{KStream, KTable}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.scala.ImplicitConversions._

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Properties

object KafkaStreams {

  object Domain {
    type UserId = String
    type Profile = String
    type Product = String
    type OrderId = String
    type Status = String

    case class Order(orderId: OrderId,userId: UserId, products: List[Product], amount: Double)
    case class Discount(profile: Profile,amount: Double)
    case class Payment(orderId: OrderId, status: Status)
  }

  object Topics {
    val OrderByUser = "order-by-user"
    val DiscountProfilesByUser = "discount-profile-by-user"
    val Discounts = "discounts"
    val Orders = "orders"
    val Payments = "payments"
    val PaidOrders = "paid-orders"
  }

  // source = emit elements
  // flow = transforms elements along the way (e.g. map)
  // sink = "ingests" elements

  import Domain._
  implicit def serde[A >: Null : Decoder : Encoder]: Serde[A] = {
    val serializer = (a: A) => a.asJson.noSpaces.getBytes()
    val deserializer = (bytes: Array[Byte]) => {
      val string = new String(bytes)
      decode[A](string).toOption
    }

    Serdes.fromFn[A](serializer, deserializer)
  }

  def main(args: Array[String]): Unit = {
    //topology
    var builder = new StreamsBuilder()

    // KStream
    val usersOrdersStream: KStream[UserId, Order] = builder.stream[UserId, Order](OrderByUser)

    // KTable - is distributed
    val userPofilesTable: KTable[UserId, Profile] = builder.table[UserId, Profile](DiscountProfilesByUser)

    // GlobalKTable - copied to all the nodes (should store few values)
    val discountProfilesGTable: GlobalKTable[Profile,Discount] = builder.globalTable[Profile, Discount](Discounts)

    // KStream transformation: filter, map, mapvalues
    val expensiveOrders = usersOrdersStream.filter { (userId, order) =>
      order.amount > 1000
    }

    val listsOfProducts = usersOrdersStream.mapValues { order =>
      order.products
    }

    val productsStream = usersOrdersStream.flatMapValues(_.products)

    // join
    val ordersWithUserProfiles = usersOrdersStream.join(userPofilesTable) { (order, profile) =>
      (order, profile)
    }

    val discountedOrdersStream = ordersWithUserProfiles.join(discountProfilesGTable) (
      { case (userId, (order, profile)) => profile },
      { case ((order, profile), discount) => order.copy(amount = order.amount - discount.amount)}
    )

    // pick another identifier
    val ordersStream = discountedOrdersStream.selectKey((userId, order) => order.orderId)
    val paymentsStream = builder.stream[OrderId, Payment](Payments)

    val joinWindow = JoinWindows.of(Duration.of(5, ChronoUnit.MINUTES))
    val joinOrderPayments = (order:Order, payment: Payment) => if (payment.status == "PAID") Option(order) else Option.empty[Order]

    val ordersPaid = ordersStream.join(paymentsStream)(joinOrderPayments, joinWindow)
      .flatMapValues(maybeOrder => maybeOrder.toIterable)

    //sink
    ordersPaid.to(PaidOrders)

    val topology = builder.build()

    val props = new Properties
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-application-test")
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.stringSerde.getClass)

//    println(topology.describe())
    val application = new KafkaStreams(topology, props)
    application.start()
    println("Application Started !!!")
  }

  //Generate commands for create topic
  def createTopics(): Unit = {
    List(
      "order-by-user", "discount-profile-by-user", "discounts", "orders", "payments", "paid-orders"
    ).foreach{ topic =>
      println(s"bin/kafka-topics.sh --create --partitions 1 --replication-factor 1 --topic ${topic} --bootstrap-server localhost:9092")
    }
  }

}
