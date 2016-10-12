package org.laborunion.project.hollyshit.server

import org.laborunion.project.hollyshit.clientmsgs.CommandMsg.Cmd
import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event
import org.laborunion.project.hollyshit.servermsgs.Move

/**
  * Created by bbondarenko on 10/9/16.
  */
object ImplicitEventsConverter {

  implicit def toServerEventMsg(ev: Cmd): Event = ev match {
    case Cmd.Move(mc) =>
      val me = Move(mc.dx, mc.dy, mc.da)
      Event.Move(me)
    case _ => throw new Exception("this event could not be sent by client")
  }
}
