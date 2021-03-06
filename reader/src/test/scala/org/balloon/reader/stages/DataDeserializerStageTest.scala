package org.balloon.reader.stages

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import org.balloon.data.observatory.{Australia, ObservatoryData}
import org.balloon.data.temperature.{Celsius, Coordinates, Kilometers}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class DataDeserializerStageTest extends TestKit(ActorSystem("DataDeserializerTestSystem")) with FreeSpecLike with Matchers {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val c = Coordinates(Kilometers(10), Kilometers(5))

  "When data" - {
    "is valid deserialize observatory data" in {
      val source = Source.single("2014-10-29T13:34:21|10,5|10|AU")
      val sink = Sink.head[ObservatoryData]
      val flow: Flow[String, ObservatoryData, NotUsed] = Flow[String].via(new DataDeserializerStage)
      val f: Future[ObservatoryData] = source.via(flow).runWith(sink)

      val data = Await.result(f, 2 seconds)

      data should equal(Australia(LocalDateTime.parse("2014-10-29T13:34:21"), c, Celsius(10)))
    }

    "is invalid deserializer omits it" in {
      val source = Source(List("2014-10-29T13:34|10,5|10|AU", "2014-10-29T13:34:21|10,5|10|AU"))
      val flow: Flow[String, ObservatoryData, NotUsed] = Flow[String].via(new DataDeserializerStage)
      val f: Future[List[ObservatoryData]] = source.via(flow).runFold(List[ObservatoryData]())((a, b) => b :: a)

      val data = Await.result(f, 2 seconds)
      data should equal(List(Australia(LocalDateTime.parse("2014-10-29T13:34:21"), c, Celsius(10))))
    }
  }

}
