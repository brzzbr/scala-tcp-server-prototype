package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import org.joda.time.DateTime
import org.laborunion.project.hollyshit.clientmsgs.MessageWrapper.Msg
import org.laborunion.project.hollyshit.clientmsgs.{EventMsg, GetEventsMsg, GetStateMsg, MessageWrapper}
import org.laborunion.project.hollyshit.server.PlayRoom.{ClientDisconnected, GetCurrentState, GetEventsFromTime}

import scala.concurrent.duration._

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

  implicit val timeout = Timeout(100 milliseconds)

  // контекст этого актора наблюдает за актором соединения
  // когда актор соединения отвалится, мы получим об этом сообщение
  context watch connection

  override def receive: Receive = {

    // получили сообщение от клиента -- оповестим супервайзера (сервер)
    case Received(data) =>
      log.info(s"Client id: $id, received message from: $remote")
      // парсим сообщение, если не можем распарсить -- валится exception
      // который надо игнорировать, для этого надо корректно настроить
      // супервайзера...
      // TODO: Error kernel pattern по ошибкам парсинга
      val msgWrapper = MessageWrapper.parseFrom(data.toArray)
      msgWrapper.msg match {
        // запрос текущего состояния
        case Msg.GetStateMsg(gsm) => handleGetStateMsg(gsm)

        // запрос событий произошедших с некоего времени
        case Msg.GetEventsMsg(gem) => handleGetEventsMsg(gem)

        // событие с клиента
        case Msg.EventMsg(em) => handleEventMsg(em)

        // пришла какая-то бурда
        case Msg.Empty => // игонрируем
      }

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


  def handleGetStateMsg(gsm: GetStateMsg) = {
    val future = playroom ? GetCurrentState(id, new DateTime(gsm.time))
//    future.onSuccess { case result: PlayRoomState =>
//      // TODO: Перегнать PlayRoomState в ByteString (protobuf)
//      //connection ! Write(result)
//    }
  }

  def handleGetEventsMsg(gem: GetEventsMsg) = {
    val future = playroom ? GetEventsFromTime(id, new DateTime(gem.fromTime), new DateTime(gem.time))
//    future.onSuccess { case result: Events
//
//    }
  }

  def handleEventMsg(em: EventMsg) = {

  }
}
