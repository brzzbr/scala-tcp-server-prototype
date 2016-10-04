package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import org.joda.time.DateTime
import org.laborunion.project.hollyshit.msgs.{EventMsg, Events, PlayRoomState}
import org.laborunion.project.hollyshit.server.PlayRoom.{ClientDisconnected, GetCurrentState, GetEventsFromTime}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
object Server {

  def props(port: Int): Props = Props(new Server(port))
}

class Server(port: Int) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system
  import context.dispatcher

  val w = 10 seconds
  val n = w div 2

  var idGenerator: Int = 0

  def generateClientId(): Int = {
    idGenerator += 1
    idGenerator
  }

  var clients: Map[Int, ActorRef] = Map.empty[Int, ActorRef]
  var eventBuffer: Vector[EventMsg] = Vector.empty[EventMsg]
  var currentState: PlayRoomState = PlayRoomState(0)

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", port))

  // TODO: раз в N секунд собирать новый state комнаты и посылать сообщение с ним N = W / 2
  val stateSnapshotJob = context.system.scheduler.schedule(n, w) {
    println("AZAZA")
  }

  override def postStop(): Unit = {
    stateSnapshotJob.cancel()
    super.postStop()
  }

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
    // но пока большего и не нужно. Один сервер -- одна комната. Пришло событие от клиента -->
    // записали в коллекцию событий, выкинули устаревшие, отсортировали по времени, сохранили.
    case event@EventMsg(_, _, _) =>
      val treshold = DateTime.now.getMillis - w.toMillis
      eventBuffer = (eventBuffer :+ event)
        .filter(_.time > treshold)
        .sortBy(_.time)

    case GetCurrentState => sender ! currentState

    case GetEventsFromTime(time) =>
      val events = eventBuffer.dropWhile(_.time < time)
      sender ! Events(events)

    // когда клиент отключается, надо бы его выкинуть из мапы клиентов.
    // Это тоже функциональность игровой комнаты, не сервера.
    case ClientDisconnected(id) =>
      log.info(s"Client $id disconnected")
      clients -= id
  }
}