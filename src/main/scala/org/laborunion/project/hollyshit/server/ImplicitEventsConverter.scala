package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.clientmsgs.ClientEventMsg
import org.laborunion.project.hollyshit.servermsgs.ServerEventMsg

/**
  * Created by bbondarenko on 10/9/16.
  */
object ImplicitEventsConverter {

  implicit def toServerEventMsg(ev: ClientEventMsg.Event): ServerEventMsg.Event = ev match {
    case ClientEventMsg.Event.Move(move) => ServerEventMsg.Event.Move(move)
    case _ => throw new Exception("this event could not be sent by client")
  }
}
