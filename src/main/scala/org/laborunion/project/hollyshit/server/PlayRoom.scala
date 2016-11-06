package org.laborunion.project.hollyshit.server

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState, PlayerStatus, Respawn}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
class PlayRoom(playRoomId: Int) extends Actor with ActorLogging {

  import PlayRoom._
  import Consts._
  import StateSnapshoter._

  import scala.concurrent.ExecutionContext.Implicits.global

  val delay = 5.seconds
  val period = 1.second

  var players: Map[Int, ActorRef] = Map.empty[Int, ActorRef]

  var eventBuffer: Vector[EventMsg] = Vector.empty[EventMsg]
  var currentState: PlayRoomState = PlayRoomState(System.currentTimeMillis, Seq.empty[PlayerStatus])
  var stateSnapshotJob: Cancellable = _

  // TODO: надо вынести в отдельный класс с различной логикой мерджа событий в зависимости от типа объекта
  override def preStart(): Unit = {
    // посылаем самому себе новое состояние сцены
    stateSnapshotJob = context.system.scheduler.schedule(delay, period) {
      self ! getCurrentState(currentState, eventBuffer)
    }
  }

  override def postStop(): Unit = stateSnapshotJob.cancel()

  override def receive: Receive = {
    // Пришло новое состояние сцены -- надо обновить
    case state: PlayRoomState =>
      currentState = state

    // Пришло событие --> заслали всем клиентам записали в коллекцию событий,
    // выкинули устаревшие, отсортировали по времени, сохранили.
    case event: EventMsg =>
      players.values.foreach(_ ! event)
      val threshold = System.currentTimeMillis - delay.toMillis
      eventBuffer = (eventBuffer :+ event)
        .filter(_.time > threshold)
        .sortBy(_.time)

    // запрос состояния сцены
    case GetCurrentState => sender ! currentState

    // пришел новый клиент, запишем егоб отправим состояние
    // комнаты и сгенерим событие респауна
    case ClientConnected(clientId) =>
      players += clientId -> sender
      sender ! currentState
      self ! EventMsg(clientId, System.currentTimeMillis)
        .withRespawn(Respawn(defaultCoords))
  }
}

object PlayRoom {

  def props(playRoomId: Int): Props = Props(new PlayRoom(playRoomId))

  case class ClientConnected(clientId: Int)

  case object GetCurrentState
}
