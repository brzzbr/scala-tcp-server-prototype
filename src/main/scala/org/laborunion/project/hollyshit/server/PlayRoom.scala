package org.laborunion.project.hollyshit.server

import akka.actor.{Actor, ActorLogging}
import org.joda.time.DateTime

/**
  * Created by borisbondarenko on 17.09.16.
  */
object PlayRoom {

  case class ClientDisconnected(clientId: Int)

  case object GetCurrentState

  case class GetEventsFromTime(fromTime: Long)
}

/**
  * Пока у нас нет отдельного актора под игровую комнату.
  * Функции комнаты тащит сервер, т.о. один сервер --> одна комната
  */
class PlayRoom(id: Int) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
