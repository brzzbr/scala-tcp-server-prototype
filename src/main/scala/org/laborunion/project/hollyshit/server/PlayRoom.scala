package org.laborunion.project.hollyshit.server

import akka.util.ByteString
import org.joda.time.DateTime

/**
  * Created by borisbondarenko on 17.09.16.
  */
object PlayRoom {

  case class ClientEvent(clientId: Long, data: ByteString)

  case class ClientDisconnected(clientId: Long)

  case class GetCurrentState(clientId: Long, cleintTime: DateTime)

  case class GetEventsFromTime(clientId: Long, fromTime: DateTime, clientTime: DateTime)
}

/**
  * Пока у нас нет отдельного актора под игровую комнату.
  * Функции комнаты тащит сервер, т.о. один сервер --> одна комната
  */
class PlayRoom {

}
