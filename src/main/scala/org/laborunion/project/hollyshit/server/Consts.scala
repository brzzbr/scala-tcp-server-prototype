package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.servermsgs.{PlayerCoords, PlayerStatus}

/**
  * Created by bbondarenko on 10/10/16.
  */
object Consts {

  val defaultCoords = PlayerCoords(0, 0, 0)

  val defaultPlayer: Int => PlayerStatus = k =>
    PlayerStatus(
      id = k,
      isAlive = false,
      coords = defaultCoords)
}
