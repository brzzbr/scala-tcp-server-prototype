package org.laborunion.project.hollyshit.server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.Event
import akka.util.ByteString
import org.laborunion.project.hollyshit.clientmsgs.{CommandMsg, MsgToClient}
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState}

import scala.collection._

/**
  * Created by borisbondarenko on 17.09.16.
  */
class ClientHandler(
    id: Int,
    connection: ActorRef,
    playroom: ActorRef,
    storageThreshold: Int = ClientHandler.defStorageThreshold) extends Actor with ActorLogging {

  import PlayRoom._
  import ClientHandler._
  import akka.io.Tcp._

  val storage: mutable.Queue[ByteString] = mutable.Queue.empty
  var stored = 0

  var isClosing: Boolean = false

  var msgId: Long = 0L
  def getMsgId: Long = { msgId += 1; msgId }

  // контекст этого актора наблюдает за актором соединения
  // когда актор соединения отвалится, мы получим об этом сообщение
  context watch connection

  // перед стартом шлем привет комнате, что готовы работать
  // в ответ комната шлет состояние
  override def preStart(): Unit = {
    super.preStart()
    playroom ! ClientConnected(id)
  }

  // по умолчанию, хэндлер клиента не инициализирован и
  // ожидает от комнаты лишь игрового состояни
  override def receive: Receive = notInitialized

  // получив игровое состояние переводим хэндлер
  // инициализированного состояния и шлем себе сообщение
  // с текущим состоянием сцены
  def notInitialized: Receive = {
    case s:PlayRoomState =>
      context become initializedUnbuffered
      self ! s

    case PeerClosed => context stop self

    case _ => // ignore
  }

  // прием команд от клиента и посыл ему состояний и событий
  def receiveMessages: Receive = {
    case Received(data) =>
      val command = CommandMsg.parseFrom(data.toArray)
      playroom ! (id, command)

    case s:PlayRoomState =>
      val msg = new MsgToClient(getMsgId).withState(s)
      self ! ByteString(msg.toByteArray)

    case e:EventMsg =>
      val msg = new MsgToClient(getMsgId).withEvent(e)
      self ! ByteString(msg.toByteArray)
  }

  // рабочее состояние актора с прямой отправкой сообщения (буфер пуст)
  def initializedUnbuffered: Receive = receiveMessages orElse {
    case payLoad: ByteString  => writeToClient(payLoad)
    case PeerClosed           => context stop self
  }

  // рабочее состояние актора когда используется буфер
  def initializedBuffered: Receive = receiveMessages orElse {
    case payLoad: ByteString  => buffer(payLoad)
    case Ack                  => acknowledge()
    case PeerClosed           => isClosing = true
  }

  def writeToClient(data: ByteString): Unit = {
    if (buffer(data)) {
      connection ! Write(data, Ack)
      context.become(initializedBuffered, discardOld = false)
    }
    else context become notInitialized
  }

  def buffer(data: ByteString): Boolean = {
    storage enqueue data
    stored += data.size
    if (stored > storageThreshold) {
      storage.clear()
      stored = 0
      false
    } else true
  }

  def acknowledge(): Unit = {
    require(storage.nonEmpty, "storage was empty")
    val msg = storage.dequeue()
    stored -= msg.size

    if (storage.isEmpty) {
      if (isClosing) context stop self
      else context.become(initializedUnbuffered, discardOld = false)
    }
    else connection ! Write(storage.front, Ack)
  }
}

object ClientHandler {

  val defStorageThreshold = 65536

  def props(id: Int, connection: ActorRef, playroom: ActorRef): Props =
    Props(new ClientHandler(id, connection, playroom))

  case object Ack extends Event
}