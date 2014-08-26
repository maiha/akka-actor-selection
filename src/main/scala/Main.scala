import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object UnixFileSystemLike {
  case class AddUser(name: String)
  case class Who(name: String)
  case class Iam(name: String)
  case class UserNotFound(name: String)

  def resolve(context: ActorRefFactory, name: String, m: Any): Unit = {
    AbstractUserResolver(context).resolve(name, m)
    RelativeUserResolver(context).resolve(name, m)
    CurrentUserResolver(context).resolve(name, m)
  }
  
  abstract class UserResolver(context: ActorRefFactory) {
    def resolve(name: String, msg: Any) = {
      val sel = getSelection(name)
      def debug(buf: Any) = println(s"${getClass.getSimpleName}#resolve($name)$buf")

      implicit val timeout = Timeout(1.second)
      sel.resolveOne().onComplete {
        case Success(ref) =>
          debug(s"found: ${ref.path} [${ref.getClass.getSimpleName}]")
          sel.ask(msg).onComplete {
            case Success(m) => debug(m)
            case Failure(m) => debug(m)
          }
        case Failure(ex)  => debug(ex)
      }
    }
    protected def getSelection(name: String): ActorSelection
  }

  case class AbstractUserResolver(context: ActorRefFactory) extends UserResolver(context) {
    def getSelection(name: String) = context.actorSelection(s"/user/home/$name")
  }

  case class RelativeUserResolver(context: ActorRefFactory) extends UserResolver(context) {
    def getSelection(name: String) = context.actorSelection(s"../home/$name")
  }

  case class CurrentUserResolver(context: ActorRefFactory) extends UserResolver(context) {
    def getSelection(name: String) = context.actorSelection(name)
  }

  class Etc extends Actor {
    override def receive: Receive = {
      case m @ Who(name) => resolve(context, name, m)
      case _ => Iam("/etc")
    }
  }

  class Home extends Actor {
    override def receive: Receive = {
      case m @ Who(name) => resolve(context, name, m)
      case AddUser(name) => context.actorOf(Props(new User(name)), name)
    }
  }

  class User(name: String) extends Actor {
    override def receive: Receive = {
      case m @ Who(name) => sender() ! Iam(name)
    }
  }
}

object Main {
  import UnixFileSystemLike._

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("test")
    val etc    = system.actorOf(Props[Etc] , name = "etc")
    val home   = system.actorOf(Props[Home], name = "home")

    // setup
    val name = "maiha"
    home ! AddUser(name)

    // lookup user by given resolver
    etc  ! Who(name)
    home ! Who(name)

    // teardown
    system.shutdown
  }
}
