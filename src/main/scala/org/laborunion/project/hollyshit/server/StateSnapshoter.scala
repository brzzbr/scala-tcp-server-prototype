package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event
import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event._
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState, PlayerCoords, PlayerStatus}

/**
  * Created by bbondarenko on 10/10/16.
  */
object StateSnapshoter {

  import Consts._

  // TODO: тут будет много снапшотов по событиям различных типов объектов
  // которые можно неплохо параллелить, прежде всего, фильтруя events по objectType (которого пока нет)
  // потом заворачивать вычисления статусов различных типов объектов в отдельные Future,
  // а в методах типа getPlayersState где идет fold по конкретному объекту, можно зафигачить
  // map --> parallel map и херачить еще быстрее... потом можно подумать над отдельным
  // ForkJoinPool под это дело!
  def getCurrentState(prevState: PlayRoomState, events: Vector[EventMsg]): PlayRoomState = {

    val pEvents = events
      .filter(_.time > prevState.time)
      .groupBy(_.objectId)

    val pStatuses = prevState
      .players
      .map(p => p.id -> p)
      .toMap

    PlayRoomState(System.currentTimeMillis, getPlayersState(pStatuses, pEvents))
  }

  // TODO: вычисление текущих статусов объектов можно запилить через type-class'ы
  // обрати внимание -- этот метод можно обобщить по типу (PlayerStatus)... ну и default
  def getPlayersState(p: Map[Int, PlayerStatus], e: Map[Int, Vector[EventMsg]]): Seq[PlayerStatus] = {
    val pd = p.withDefault(defaultPlayer)
    (p.keySet ++ e.keySet).map { k =>
      val es = e.getOrElse(k, Vector.empty).sortBy(_.time).map(_.event)
      es.foldLeft(pd(k))(updatePlayerStatus)
    }.toSeq
  }

  def updatePlayerStatus(s: PlayerStatus, e: Event): PlayerStatus = e match {
    case Respawn(r) => s.update(
      _.coords := r.coords,
      _.isAlive := true
    )
    case Move(m) => s.withCoords {
      val x = s.coords.x + m.dx
      val y = s.coords.x + m.dy
      val a = s.coords.x + m.da
      PlayerCoords(x, y, a)
    }
    case _ => s
  }
}
