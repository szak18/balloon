package org.balloon.generator

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Keep, Merge, RunnableGraph}
import akka.util.ByteString
import org.balloon.data.observatory.ObservatoryData
import org.balloon.generator.connection.Connection

import scala.concurrent.{ExecutionContext, Future}

// TODO tests
// TODO progress bar
case class Generator(observatories: List[String])(implicit system: ActorSystem, materializer: ActorMaterializer) {
  private implicit val ec: ExecutionContext = system.dispatcher
  private val connectionsF: Int => List[Connection] = n => observatories.map(ob => Connection.to(ob, n))

  def run(numberOfData: Int, fileName: Path): Future[IOResult] = {
    val n = numberOfData / observatories.length
    val connections = connectionsF(n)
    val serializer = Flow[ObservatoryData].map(d => d.serialize)
    val writer = Flow[String].mapAsync(5)(s => Future(ByteString(s + "\n"))).toMat(FileIO.toPath(fileName))(Keep.right)

    val g = RunnableGraph.fromGraph(GraphDSL.create(writer) { implicit b =>
      sink =>
        import GraphDSL.Implicits._

        val merge = b.add(Merge[String](connections.length))

        connections.map(_.channel).map(ch => ch ~> serializer ~> merge)
        merge ~> sink

        ClosedShape
    })

    g.run()
  }
}

// TODO remove this
//compile group: 'com.lihaoyi', name: 'ammonite_2.12.3', version: '1.0.2'
//ammonite.Main(predefCode = "repl.frontEnd() = ammonite.repl.FrontEnd.JLineUnix\nimport org.balloon.data.temperature.{Celsius, Fahrenheit, Kelvin}").run(
//)
