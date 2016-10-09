package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.laborunion.project.hollyshit.events.{PlayerCoords, Respawn}
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

  import PlayRoom._
  import context.dispatcher

  var idGenerator: Int = 0
  val w = 10 seconds
  val n = w div 10
  val g = 10 milliseconds

  val defaultCoords = PlayerCoords(0, 0, 0)

  var eventBuffer: Vector[ServerEventMsg] = Vector.empty[ServerEventMsg]
  var currentState: PlayRoomState = PlayRoomState(System.currentTimeMillis, Seq.empty[PlayerStatus])

  // TODO: надо вынести в отдельный класс с различной логикой мерджа событий в зависимости от типа объекта
  val stateSnapshotJob = context.system.scheduler.schedule(n, w) {
    val mapOfEvents = eventBuffer.groupBy(_.objectId)
    val playersStatus = currentState.players.map(x => x.id -> x).toMap
    val pStatuses = mapOfEvents.map { case(k, v) =>
      // если игрока не было, а события по нему есть, надо его создать
      val curState = playersStatus.getOrElse(k,
        PlayerStatus(
          id = k,
          isAlive = false,
          coords = defaultCoords))

      v.sortBy(_.time).foldLeft(curState) { (s, e) =>
        e.event match {

          case Event.Respawn(r) => s.withCoords(r.coords).withIsAlive(true)

          case Event.Move(m) => s.withCoords {
            val x = s.coords.x + m.dx
            val y = s.coords.x + m.dy
            val a = s.coords.x + m.da
            PlayerCoords(x, y, a)
          }

          case _ => s
        }
      }
    }.toSeq

    // посылаем самому себе новое состояние сцены
    self ! PlayRoomState(System.currentTimeMillis, pStatuses)
  }

  override def postStop(): Unit = {
    stateSnapshotJob.cancel()
    super.postStop()
  }

  override def receive: Receive = {
    // Пришло новое состояние сцены -- надо обновить
    case state: PlayRoomState => currentState = state

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
        Respawn(defaultCoords)))
  }

  def generateClientId(): Int = {
    idGenerator += 1
    idGenerator
  }
}
