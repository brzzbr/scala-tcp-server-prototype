package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import org.laborunion.project.hollyshit.server.ClientHandler.Send
import org.laborunion.project.hollyshit.server.PlayRoom.{ClientDisconnected, ClientEvent}

/**
  * Created by borisbondarenko on 17.09.16.
  */
object Server {

  def props(port: Int): Props = Props(new Server(port))
}

class Server(port: Int) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system

  var idGenerator: Long = 0L

  def generateClientId(): Long = {
    idGenerator += 1
    idGenerator
  }

  var clients: Map[Long, ActorRef] = Map.empty[Long, ActorRef]

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
      val id = generateClientId()
      val handler = context.actorOf(ClientHandler.props(id, remote, connection, self))
      clients += id -> handler
      context watch handler
      connection ! Register(handler)

    // Сервер пока несет на себе функции игровой комнаты. Да -- некрасиво, да -- неправильно,
    // но пока большего и не нужно. Один сервер -- одна комната. Пришло событие от одного клиента -->
    // широковещательно отстрелили его на всех остальных
    case ClientEvent(id, data) =>
      log.info(s"Client $id has sent some data")
      clients
        .filterKeys(_ != id)
        .values
        .foreach(_ ! Send(data))

    // когда клиент отключается, надо бы его выкинуть из мапы клиентов.
    // Это тоже функциональность игровой комнаты, не сервера.
    case ClientDisconnected(id) =>
      log.info(s"Client $id disconnected")
      clients -= id
  }
}