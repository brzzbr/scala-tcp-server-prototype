package org.laborunion.project.hollyshit

import akka.util.ByteString

/**
  * Created by borisbondarenko on 17.09.16.
  */
object PlayRoom {

  case class ClientEvent(clientId: Long, data: ByteString)

  case class ClientDisconnected(clientId: Long)
}

/**
  * Пока у нас нет отдельного актора под игровую комнату.
  * Функции комнаты тащит сервер, т.о. один сервер --> одна комната
  */
class PlayRoom {

}
