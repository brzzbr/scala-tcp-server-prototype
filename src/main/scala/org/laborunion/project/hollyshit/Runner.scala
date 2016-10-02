package org.laborunion.project.hollyshit

import akka.actor.ActorSystem
import org.laborunion.project.hollyshit.client.Client
import org.laborunion.project.hollyshit.server.Server

import scala.io.StdIn

/**
  * Created by borisbondarenko on 18.09.16.
  */
object Runner extends App {

  val system = ActorSystem()
  val log = system.log

  val server = system.actorOf(Server.props(3159), "test-server")
  val clientA = system.actorOf(Client.props("localhost", 3159), "a-client")
  val clientB = system.actorOf(Client.props("localhost", 3159), "b-client")
  val clientC = system.actorOf(Client.props("localhost", 3159), "c-client")
  log.info(s"PRESS ENTER TO STOP...")

  Thread.sleep(2000)

  clientA ! "A message"
  clientB ! "B message"
  clientC ! "C message"

  StdIn.readLine()
  system.terminate()
  log.info(s"ACTOR SYSTEM TERMINATED")
}
