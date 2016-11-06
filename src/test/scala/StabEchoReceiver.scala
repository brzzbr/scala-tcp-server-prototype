import akka.actor.Actor

/**
  * Created by borisbondarenko on 22.10.16.
  */
trait StabEchoReceiver { self: Actor =>
  def stabReceive(m: String): Receive = {
    case msg: String if msg == m => sender ! m
  }
}

trait HeyActor extends Actor {

  import HeyActor._

  override def receive: Receive = {
    case WhatsMyState => sender ! "azaza"
    case msg@_ => self ! msg
  }
}

object HeyActor {
  case object WhatsMyState
}