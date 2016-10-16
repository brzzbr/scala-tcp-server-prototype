package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event
import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event._
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState, PlayerCoords, PlayerStatus}

/**
  * Created by bbondarenko on 10/10/16.
  *
  * Temporary procedural implementation of up PlayRoomState updating.
  * Should be rewritten as a function of PlayRoomState class.
  *
  * Status folding functions could be implemented through type-classes.
  * Right now there's no necessity due to we have only one obj-type
  * to fold (PlayerStatus)
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

    val pEvents = filterEvents(events, prevState.time)

    val pStatuses = prevState
      .players
      .map(p => p.id -> p)
      .toMap

    PlayRoomState(System.currentTimeMillis, getPlayersState(pStatuses, pEvents))
  }

  /**
    * Фильтруем устаревшие события
    * @param e вектор событий
    * @param t отметка времени предыдуще
    * @return map объекта к событиям по нему
    */
  def filterEvents(e: Vector[EventMsg], t: Long): Map[Int, Vector[EventMsg]] =
    e.filter(_.time > t).groupBy(_.objectId)

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
      val y = s.coords.y + m.dy
      val a = s.coords.a + m.da
      PlayerCoords(x, y, a)
    }
    case _ => s
  }
}
