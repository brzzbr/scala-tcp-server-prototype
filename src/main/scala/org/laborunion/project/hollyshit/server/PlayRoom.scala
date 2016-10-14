package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState, PlayerStatus}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
class PlayRoom(playRoomId: Int) extends Actor with ActorLogging {

  import PlayRoom._
  import StateSnapshoter._
  import context.dispatcher

  val w = 10.seconds
  val n = 1.second
  val c = 10.milliseconds

  var clients: Vector[ActorRef] = Vector.empty[ActorRef]

  var eventBuffer: Vector[EventMsg] = Vector.empty[EventMsg]
  var currentState: PlayRoomState = PlayRoomState(System.currentTimeMillis, Seq.empty[PlayerStatus])
  var stateSnapshotJob: Cancellable = _
  var sendEventsJob: Cancellable =_

  // TODO: надо вынести в отдельный класс с различной логикой мерджа событий в зависимости от типа объекта
  override def preStart(): Unit = {
    // посылаем самому себе новое состояние сцены
    stateSnapshotJob = context.system.scheduler.schedule(n, n) {
      self ! getCurrentState(currentState, eventBuffer)
    }

    // посылаем игрокам пачки собыий
    sendEventsJob = context.system.scheduler.schedule(n, c) {

    }
  }

  override def postStop(): Unit = {
    stateSnapshotJob.cancel()
    sendEventsJob.cancel()
    super.postStop()
  }

  override def receive: Receive = {
    // Пришло новое состояние сцены -- надо обновить
    case state: PlayRoomState =>
      currentState = state

    // Пришло событие --> записали в коллекцию событий,
    // выкинули устаревшие, отсортировали по времени, сохранили.
    case event: EventMsg =>
      val threshold = System.currentTimeMillis - w.toMillis
      eventBuffer = (eventBuffer :+ event)
        .filter(_.time > threshold)
        .sortBy(_.time)

    // запрос состояния сцены
    case GetCurrentState => sender ! currentState
  }
}

object PlayRoom {

  def props(playRoomId: Int): Props = Props(new PlayRoom(playRoomId))

  case class ClientConnected(id: Int, connection: ActorRef)

  case object GetCurrentState
}
