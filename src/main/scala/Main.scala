import akka.actor._

object Main {
  class Node extends Actor {
    def receive = {
      case _ => // nop
    }
  }

  val p = Props[Node]

  def main(args: Array[String]): Unit = {
    val systems = Seq("sys1", "sys2").map(ActorSystem(_))
//    systems.map{ _.actorOf(p, "n1")}

    for {
      sys <- systems
      uri <- Seq("akka://sys1/user/n1", "akka://sys2/user/n1")
    } yield {
      val path = ActorPath.fromString(uri)
      val sel  = sys.actorSelection(path)
      println(s"(in ${sys.name}) resolve($path) -> $sel")
    }

    systems.map(_.shutdown)
  }
}
