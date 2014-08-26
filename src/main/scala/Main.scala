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

    val system = ActorSystem("test")
    systems.add(system)

    val c1 = system.actorOf(p, "c1")
    val c2 = system.actorOf(p, "c2")

    val future = c2 ? Create("c21")
    val result = Await.result(future, timeout.duration).asInstanceOf[ActorRef]

    def execute(lookup: ActorRef, name: String) {
      println(s"execute: $name")
      for {
        sel   <- ask(lookup, name).mapTo[ActorSelection]
//        found <- ask(sel, Find(name)).mapTo[Found]
      } yield {
        println(sel)
//        println(found)
      }
    }

//    val lookup = system.actorOf(Props[Lookup], "lookup")
    val lookup = anotherSystemLookup

    for {
      name <- Seq("c1","c2","c2/c21")
      path <- Seq("", "../", "/user/", "akka://test/user/")
    } yield execute(lookup, path + name)

    Thread.sleep(5000)

    for (s <- systems) yield s.shutdown
  }

  def anotherSystemLookup = {
    val system = ActorSystem("another")
    systems.add(system)
    val lookup = system.actorOf(Props[Lookup], "lookup")
    lookup
  }
}
