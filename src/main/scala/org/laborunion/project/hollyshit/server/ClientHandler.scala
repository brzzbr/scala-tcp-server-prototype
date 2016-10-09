package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import org.laborunion.project.hollyshit.clientmsgs.MessageWrapper.Msg
import org.laborunion.project.hollyshit.clientmsgs.{GetEventsMsg, GetStateMsg, MessageWrapper}
import org.laborunion.project.hollyshit.server.PlayRoom._
import org.laborunion.project.hollyshit.servermsgs.{PlayRoomState, ServerEventMsg, ServerEvents}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
object ClientHandler {

  def props(id: Int, remote: InetSocketAddress, connection: ActorRef, playroom: ActorRef): Props =
    Props(new ClientHandler(id, remote, connection, playroom))
}

class ClientHandler(
    id: Int,
    remote: InetSocketAddress,
    connection: ActorRef,
    playroom: ActorRef) extends Actor with ActorLogging {

  import ImplicitEventsConverter._
  import akka.io.Tcp._
  import context.dispatcher

  implicit val timeout = Timeout(50 milliseconds)

  // контекст этого актора наблюдает за актором соединения
  // когда актор соединения отвалится, мы получим об этом сообщение
  context watch connection

  override def receive: Receive = {

    // получили сообщение от клиента -- оповестим супервайзера (комнату)
    case Received(data) =>
      log.info(s"Client id: $id, received message from: $remote")
      // парсим сообщение, если не можем распарсить -- валится exception
      // который надо игнорировать, для этого надо корректно настроить
      // супервайзера...
      val msgWrapper = MessageWrapper.parseFrom(data.toArray)
      msgWrapper.msg match {
        // запрос текущего состояния
        case Msg.GetStateMsg(gsm) => handleGetStateMsg(gsm)

        // запрос событий произошедших с некоего времени
        case Msg.GetEventsMsg(gem) => handleGetEventsMsg(gem)

        // событие с клиента
        case Msg.EventMsg(em) =>
          val event = new ServerEventMsg(
            objectId = id,
            time = em.time,
            event = em.event)
          playroom ! (id, em)

        // пришла какая-то бурда
        case Msg.Empty => // игонрируем
      }

    // соединение было закрыто, можно уведомить игровую комнату
    // и незамедлительно убить актора
    case PeerClosed =>
      log.info(s"Client id: $id, peer: $remote closed")
      context stop self
  }

  def handleGetStateMsg(gsm: GetStateMsg) = {
    val future = playroom ? GetCurrentState
    future.onSuccess { case res: PlayRoomState =>
      connection ! Write(ByteString(res.toByteArray))
    }
  }

  def handleGetEventsMsg(gem: GetEventsMsg) = {
    val future = playroom ? GetEventsFromTime(gem.fromTime)
    future.onSuccess { case res: ServerEvents =>
      connection ! Write(ByteString(res.toByteArray))
    }
  }
}