package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.laborunion.project.hollyshit.events.Respawn
import org.laborunion.project.hollyshit.servermsgs.ServerEventMsg.Event
import org.laborunion.project.hollyshit.servermsgs._

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

  import StateSnapshoter._
  import PlayRoom._
  import context.dispatcher

  val w = 10 seconds
  val n = w div 10

  var idGenerator: Int = 0
  var eventBuffer: Vector[ServerEventMsg] = Vector.empty[ServerEventMsg]
  var currentState: PlayRoomState = PlayRoomState(System.currentTimeMillis, Seq.empty[PlayerStatus])

  // TODO: надо вынести в отдельный класс с различной логикой мерджа событий в зависимости от типа объекта
  val stateSnapshotJob = context.system.scheduler.schedule(w, n) {
    // посылаем самому себе новое состояние сцены
    self ! getCurrentState(currentState, eventBuffer)
  }

  override def postStop(): Unit = {
    stateSnapshotJob.cancel()
    super.postStop()
  }

  override def receive: Receive = {
    // Пришло новое состояние сцены -- надо обновить
    case state: PlayRoomState =>
      currentState = state

    // Пришло событие --> записали в коллекцию событий,
    // выкинули устаревшие, отсортировали по времени, сохранили.
    case event: ServerEventMsg =>
      val threshold = System.currentTimeMillis - w.toMillis
      eventBuffer = (eventBuffer :+ event)
        .filter(_.time > threshold)
        .sortBy(_.time)

    // запрос состояния сцены
    case GetCurrentState => sender ! currentState

    // запрос событи с некоторого времени
    case GetEventsFromTime(time) =>
      val events = eventBuffer.dropWhile(_.time < time)
      sender ! ServerEvents(events)

    // прицепился новый клиент
    case ClientConnected(remote, connection) =>
      val id = generateClientId()
      val handler = context.actorOf(ClientHandler.props(id, remote, connection, self))
      sender ! handler
      self ! ServerEventMsg(id, System.currentTimeMillis, Event.Respawn(
        Respawn(Consts.defaultCoords)))
  }

  def generateClientId(): Int = {
    idGenerator += 1
    idGenerator
  }
}
