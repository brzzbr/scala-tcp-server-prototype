package org.laborunion.project.hollyshit.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.util.ByteString
import org.laborunion.project.hollyshit.clientmsgs.MessageWrapper
import org.laborunion.project.hollyshit.clientmsgs.MessageWrapper.Msg
import org.laborunion.project.hollyshit.servermsgs.EventMsg

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 17.09.16.
  */
class ClientHandler(
    id: Int,
    remote: InetSocketAddress,
    connection: ActorRef,
    playroom: ActorRef) extends Actor with ActorLogging {

  import ClientHandler._
  import akka.io.Tcp._

  var stateSnapshotJob: Cancellable = _

  override def preStart(): Unit = {
    super.preStart()
    stateSnapshotJob =
      context.system.scheduler.schedule(period, period, self, Heartbeat)
  }

  override def postStop(): Unit = {
    stateSnapshotJob.cancel()
    super.postStop()
  }

  // контекст этого актора наблюдает за актором соединения
  // когда актор соединения отвалится, мы получим об этом сообщение
  context watch connection

  override def receive: Receive = {

    case Received(data) =>
      handleClientCommand(data)

    case Heartbeat =>
      connection ! Write(ByteString.empty)

    case e:EventMsg =>
      connection ! Write(ByteString(e.toByteArray))

    // соединение было закрыто, можно уведомить игровую комнату
    // и незамедлительно убить актора
    case PeerClosed =>
      log.info(s"Client id: $id, peer: $remote closed")
      context stop self
  }

  def handleClientCommand(data: ByteString): Unit = {
    val msgWrapper = MessageWrapper.parseFrom(data.toArray)
    msgWrapper.msg match {
      // запрос текущего состояния
      case Msg.GetStateMsg(gsm) =>
        connection ! Write(ByteString(currentState.toByteArray))

      // команда с клиента
      case Msg.CmdMsg(cm) =>
        playroom ! (id, cm)

      // пришла какая-то бурда
      case Msg.Empty => // игонрируем
    }
  }
}

object ClientHandler {

  val period = 10.seconds

  def props(id: Int, remote: InetSocketAddress, connection: ActorRef, playroom: ActorRef): Props =
    Props(new ClientHandler(id, remote, connection, playroom))

  case object Heartbeat
}