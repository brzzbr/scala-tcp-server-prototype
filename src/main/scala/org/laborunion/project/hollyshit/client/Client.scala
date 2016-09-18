package org.laborunion.project.hollyshit.client

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

/**
  * Created by borisbondarenko on 18.09.16.
  */
object Client {

  def props(host: String, port: Int) =
    Props(classOf[Client], host, port)
}

class Client(host: String, port: Int)
  extends Actor
    with ActorLogging {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Connect(new InetSocketAddress(host, port))

  override def receive: Receive = {

    case CommandFailed(c: Connect) =>
      log.error(s"Failed to connect to ${c.remoteAddress}")
      context stop self

    case c @ Connected(remote, _) =>
      log.info(s"Connected to $remote")
      val connection = sender
      connection ! Register(self)
      context become connected(connection)
  }

  def connected(connection: ActorRef): Receive = {

    case data: String =>
      connection ! Write(ByteString(data))

    case CommandFailed(w: Write) =>
      log.error(s"Failed to write")

    case Received(data) =>
      log.info(s"Received a message: ${data.utf8String}")

    case _: ConnectionClosed =>
      log.info("Connection closed")
      context stop self
  }
}
