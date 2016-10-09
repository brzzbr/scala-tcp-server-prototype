package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.laborunion.project.hollyshit.clientmsgs.ClientEventMsg
import org.laborunion.project.hollyshit.servermsgs.{PlayRoomState, ServerEventMsg, ServerEvents}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
object PlayRoom {

  case class ClientConnected(remoted: InetSocketAddress, connection: ActorRef)

  case object GetCurrentState

  case class GetEventsFromTime(fromTime: Long)

  def props(playRoomId: Int): Props = Props(new PlayRoom(playRoomId))
}

class PlayRoom(playRoomId: Int) extends Actor with ActorLogging {

  import PlayRoom._
  import context.dispatcher

  var idGenerator: Int = 0
  val w = 10 seconds
  val n = w div 4
  val g = 10 milliseconds

  var eventBuffer: Vector[ServerEventMsg] = Vector.empty[ServerEventMsg]
  var currentState: PlayRoomState = _ //PlayRoomState(0)

  // TODO: раз в N секунд собирать новый state комнаты и посылать сообщение с ним N = W / 4
  val stateSnapshotJob = context.system.scheduler.schedule(n, w) {
    val mapOfEvents = eventBuffer.groupBy(_.objectId)
    mapOfEvents.map { case(k, v) =>
      //v.sortBy(_.time).foldLeft()
    }
  }

  // TODO: игровой server-side цикл
  val gameCycleJob = context.system.scheduler.schedule(g, g) {

  }

  override def postStop(): Unit = {
    //stateSnapshotJob.cancel()
    gameCycleJob.cancel()
    super.postStop()
  }

  override def receive: Receive = {
    // Пришло событие от клиента --> записали в коллекцию событий,
    // выкинули устаревшие, отсортировали по времени, сохранили.
    case (id: Int, clientEvent: ClientEventMsg) =>
      val threshold = System.currentTimeMillis - w.toMillis
      val event = new ServerEventMsg(
        objectId = id,
        time = clientEvent.time,
        event = clientEvent.event)
      eventBuffer = (eventBuffer :+ event)
        .filter(_.time > threshold)
        .sortBy(_.time)

    case GetCurrentState => sender ! currentState

    case GetEventsFromTime(time) =>
      val events = eventBuffer.dropWhile(_.time < time)
      sender ! ServerEvents(events)

    case ClientConnected(remote, connection) =>
      val id = generateClientId()
      val handler = context.actorOf(ClientHandler.props(id, remote, connection, self))
      sender ! handler
  }

  def generateClientId(): Int = {
    idGenerator += 1
    idGenerator
  }
}
