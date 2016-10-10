package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.events.PlayerCoords
import org.laborunion.project.hollyshit.servermsgs.ServerEventMsg.Event
import org.laborunion.project.hollyshit.servermsgs.{PlayRoomState, PlayerStatus, ServerEventMsg}

/**
  * Created by bbondarenko on 10/10/16.
  */
object StateSnapshooter {

  def getCurrentState(prevState: PlayRoomState, events: Vector[ServerEventMsg]): PlayRoomState = {
    val mapOfEvents = events.filter(_.time > prevState.time).groupBy(_.objectId)
    val playersStatus = prevState.players.map(x => x.id -> x).toMap.withDefault { k =>
      PlayerStatus(
        id = k,
        isAlive = false,
        coords = Consts.defaultCoords)
    }

    val pStatuses = mapOfEvents.map { case (k, v) =>
      // если игрока не было, а события по нему есть, надо его создать
      val curState = playersStatus.getOrElse(k,
        PlayerStatus(
          id = k,
          isAlive = false,
          coords = Consts.defaultCoords))

      v.sortBy(_.time).foldLeft(curState)(foldPlayerStatus)
    }.toSeq

    PlayRoomState(System.currentTimeMillis, pStatuses)
  }

  def foldPlayerStatus(s: PlayerStatus, e: ServerEventMsg): PlayerStatus = e.event match {
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
