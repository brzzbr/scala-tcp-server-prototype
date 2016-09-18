package org.laborunion.project.hollyshit

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.ByteString
import org.laborunion.project.hollyshit.PlayRoom.{ClientDisconnected, ClientEvent}

/**
  * Created by borisbondarenko on 17.09.16.
  */
object ClientHandler {

  case class Send(data: ByteString)

  def props(id: Long, remote: InetSocketAddress, connection: ActorRef, playroom: ActorRef): Props =
    Props(new CleintHandler(id, remote, connection, playroom))
}

class CleintHandler(
    id: Long,
    remote: InetSocketAddress,
    connection: ActorRef,
    playroom: ActorRef) extends Actor with ActorLogging {

  import ClientHandler._
  import akka.io.Tcp._

  // контекст этого актора наблюдает за актором соединения
  // когда актор соединения отвалится, мы получим об этом сообщение
  context watch connection

  override def receive: Receive = {

    // получили сообщение от клиента -- оповестим супервайзера (сервер)
    case Received(data) =>
      log.info(s"Client id: $id, received message from: $remote")
      playroom ! ClientEvent(id, data)

    // надо послать сообщение клиенту
    case Send(data) =>
      log.info(s"Client $id, send data to $remote")
      connection ! Write(data)

    // соединение было закрыто, можно уведомить игровую комнату
    // и незамедлительно убить актора
    case PeerClosed =>
      log.info(s"Client id: $id, peer: $remote closed")
      playroom ! ClientDisconnected(id)
      context stop self
  }
}
