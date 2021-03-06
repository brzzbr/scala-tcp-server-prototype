package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}

/**
  * Created by borisbondarenko on 17.09.16.
  */
class Server(port: Int) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system

  var idGenerator: Int = 0
  val playRoom = getPlayRoom

  // overridable for tests
  lazy val getTcp = IO(Tcp)
  lazy val getPlayRoom = context.actorOf(PlayRoom.props(100500))
  def getClientHandler(id: Int) = context.actorOf(ClientHandler.props(id, sender, getPlayRoom))
  //----------------------

  getTcp ! Bind(self, new InetSocketAddress("localhost", port))

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
      idGenerator += 1
      sender ! Register(getClientHandler(idGenerator))
  }
}

object Server {

  def props(port: Int): Props = Props(new Server(port))
}