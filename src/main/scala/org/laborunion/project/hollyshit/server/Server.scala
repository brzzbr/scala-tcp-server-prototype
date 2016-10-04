package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import org.laborunion.project.hollyshit.server.PlayRoom.ClientConnected

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
object Server {

  def props(port: Int): Props = Props(new Server(port))
}

class Server(port: Int) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.{system, dispatcher}

  implicit val timeout = Timeout(500 milliseconds)

  val playRoom = context.actorOf(PlayRoom.props(100500))

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", port))

  override def receive: Receive = {
    // при успешном биндинге к порту просто логгируем этот факт
    case Bound(localAddress) =>
      log.info(s"Server is running on $localAddress")

    // при неудачном биндинге актор незамедлительно останавливает себя
    case CommandFailed(b: Bind) =>
      log.error(s"Failed to bind to ${b.localAddress}")
      context stop self

    // если присоединяется новый клиент -- создаем и регистрируем для него отдельный actor-handler
    case Connected(remote, _) =>
      log.info(s"Client connected: $remote")
      val connection = sender
      (playRoom ? ClientConnected(remote, connection)).onSuccess {
        case handler: ActorRef => connection ! Register(handler)
      }
  }
}