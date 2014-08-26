import akka.actor._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  case class Create(name: String)
  case class Find(msg: String)
  case class Found(msg: String)

  val p = Props[Node]

  class Node extends Actor {
    def receive = {
      case Create(name) => sender() ! context.actorOf(p, name)
      case Find(msg)    => sender() ! Found(msg)
    }
  }

  class Lookup extends Actor {
    def receive = {
      case name: String => sender() ! context.actorSelection(name)
    }
  }

  val systems = scala.collection.mutable.Set[ActorSystem]()

  def main(args: Array[String]): Unit = {

    implicit val timeout = Timeout(1.second)

    val systems = Seq("sys1", "sys2").map(ActorSystem(_))
    val c1s     = systems.map{_.actorOf(p, "c1")}

    val p1 = systems(0).actorOf(p, "p1")

    for {
      sys <- systems
      uri <- Seq("akka://sys1/user/n1", "akka://sys2/user/n1")
    } yield {
      val path = ActorPath.fromString(uri)
      val sel  = sys.actorSelection(path)
      println(s"(in ${sys.name}) resolve($path) -> $sel")
    }

//    val future = c2 ? Create("c21")
//    val result = Await.result(future, timeout.duration).asInstanceOf[ActorRef]

    def execute(ref: ActorRef, name: String) {
      println(s"[${ref.getClass.getSimpleName}]$ref: execute($name)")
      for {
        sel   <- ask(ref, name).mapTo[ActorSelection]
//        found <- ask(sel, Find(name)).mapTo[Found]
      } yield {
        println(sel)
//        println(found)
      }
    }

    for {
      ref  <- Seq(p1) // c1s
      name <- Seq("c1","c2")
      path <- Seq("", "../", "/user/", "akka://sys1/user/")
    } yield execute(ref, path + name)

    Thread.sleep(5000)

    systems.map(_.shutdown)
  }
}
