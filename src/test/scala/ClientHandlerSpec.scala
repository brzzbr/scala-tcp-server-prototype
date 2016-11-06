import akka.actor.ActorSystem
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import org.laborunion.project.hollyshit.clientmsgs.{CommandMsg, MoveCmd, MsgToClient}
import org.laborunion.project.hollyshit.server.ClientHandler
import org.laborunion.project.hollyshit.server.ClientHandler.Ack
import org.laborunion.project.hollyshit.server.PlayRoom.ClientConnected
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by borisbondarenko on 17.10.16.
  */
class ClientHandlerSpec
  extends TestKit(ActorSystem("test"))
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with DefaultTimeout
    with ImplicitSender {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val testMsgNotInitialized = "notInitialized state"
  val testMsgInitializedUnbuffered = "initialized unbuffered state"
  val testMsgInitializedBuffered = "initialized buffered state"
  val testMsgReceiveMessages = "receive messages"

  "ClientHandler in initial state" should {

    val id: Int = 10
    val connectionProbe = TestProbe()
    val playRoomProbe = TestProbe()

    trait scope {
      val client = TestActorRef(new ClientHandler(id, connectionProbe.ref, playRoomProbe.ref) with StabEchoReceiver {
        override def notInitialized: Receive = stabReceive(testMsgNotInitialized) orElse super.notInitialized
        override def initializedUnbuffered: Receive = stabReceive(testMsgInitializedUnbuffered)
      })
    }

    "send ClientConnected to play room" in new scope {
      playRoomProbe expectMsg ClientConnected(id)
    }

    "be created in notInitialized state" in new scope {
      client ! testMsgNotInitialized
      expectMsg(testMsgNotInitialized)
    }

    "be shut down on PeerClosed message" in new scope {
      val deathWatcher = TestProbe()
      deathWatcher watch client
      client ! PeerClosed
      deathWatcher expectTerminated client
    }
    
    "become initializedNotBuffered on getting PlayRoomState" in new scope {
      client ! PlayRoomState(0, Seq.empty)
      client ! testMsgInitializedUnbuffered
      expectMsg(testMsgInitializedUnbuffered)
    }
  }

  "ClientHandler in unbuffered state" should {

    val data = ByteString("AZAZA")

    trait scope {
      val id: Int = 10
      val connectionProbe = TestProbe()
      val playRoomProbe = TestProbe()
      var isBuffered = false

      val client = TestActorRef(new ClientHandler(id, connectionProbe.ref, playRoomProbe.ref, 10)
          with StabEchoReceiver {

        override def receive: Receive = initializedUnbuffered
        override def notInitialized: Receive = stabReceive(testMsgNotInitialized)
        override def initializedBuffered: Receive = stabReceive(testMsgInitializedBuffered)
        override def receiveMessages: Receive = stabReceive(testMsgReceiveMessages)
        override def buffer(data: ByteString): Boolean = {
          isBuffered = super.buffer(data)
          isBuffered
        }
      })
    }

    "receive messages" in new scope {
      client ! testMsgReceiveMessages
      expectMsg(testMsgReceiveMessages)
    }

    "be shut down on PeerClosed message" in new scope {
      val deathWatcher = TestProbe()
      deathWatcher watch client
      client ! PeerClosed
      deathWatcher expectTerminated client
    }

    "receiving payload" should {

      "send payload to client on receive data" in new scope {
        client ! data
        connectionProbe expectMsg Write(data, Ack)
      }

      "buffer payload to client on receive data" in new scope{
        client ! data
        isBuffered shouldBe true
      }

      "become initializedBuffered on receive data" in new scope {
        client ! data
        client ! testMsgInitializedBuffered
        expectMsg(testMsgInitializedBuffered)
      }

      "become nonInitialized on buffer threshold exceeded" in new scope {
        client ! ByteString("Lorem ipsum dolor sit amen")
        client ! testMsgNotInitialized
        expectMsg(testMsgNotInitialized)
      }
    }
  }

  "ClientHandler in buffered state" should {

    val data = ByteString("ABCD")

    trait scope {
      val id: Int = 10
      val connectionProbe = TestProbe()
      val playRoomProbe = TestProbe()

      val client = TestActorRef(new ClientHandler(id, connectionProbe.ref, playRoomProbe.ref)
          with StabEchoReceiver {

        override def receive: Receive = initializedBuffered
        override def receiveMessages: Receive = stabReceive(testMsgReceiveMessages)
        override def initializedUnbuffered: Receive = stabReceive(testMsgInitializedUnbuffered)
      })
    }

    "receive messages" in new scope {
      client ! testMsgReceiveMessages
      expectMsg(testMsgReceiveMessages)
    }

    "set shutdown on PeerClosed message and shutdown on ongoing Ack" in new scope {
      val deathWatcher = TestProbe()
      deathWatcher watch client
      client ! data
      client ! PeerClosed
      client ! Ack
      deathWatcher expectTerminated client
    }

    "become initializedUnbuffered on Ack with empty buffer" in new scope {
      client ! data
      client ! Ack
      client ! testMsgInitializedUnbuffered
      expectMsg(testMsgInitializedUnbuffered)
    }
  }

  "ClientHandle starting unbuffered --> buffered" should {

    val data1 = ByteString("A")
    val data2 = ByteString("AB")
    val data3 = ByteString("ABC")
    val data = ByteString("ABCD")

    trait scope {
      val id: Int = 10
      val connectionProbe = TestProbe()
      val playRoomProbe = TestProbe()

      val client = TestActorRef(new ClientHandler(id, connectionProbe.ref, playRoomProbe.ref)
        with StabEchoReceiver {

        override def receive: Receive = initializedUnbuffered
        override def receiveMessages: Receive = stabReceive(testMsgReceiveMessages)
      })
    }

    "send data sequentially by Ack" in new scope {
      Seq(data1, data2, data3, data) foreach (client ! _)
      (1 to 3).foreach(_ => client ! Ack)
      connectionProbe expectMsg Write(data1, Ack)
      connectionProbe expectMsg Write(data2, Ack)
      connectionProbe expectMsg Write(data3, Ack)
    }
  }

  "ClientHandle receiving messages" should {
    trait scope {
      val id: Int = 10
      val connectionProbe = TestProbe()
      val playRoomProbe = TestProbe()

      val client = TestActorRef(new ClientHandler(10, connectionProbe.ref, playRoomProbe.ref) with StabEchoReceiver {
        override def receive: Receive = receiveMessages orElse {
          case p: ByteString =>
            val msg = MsgToClient.parseFrom(p.toArray).msg
            testActor ! (msg.event getOrElse msg.state.get)
        }
      })
    }

    "receive CommandMsg data from TCP" in new scope {
      val data = CommandMsg(315920).withMove(MoveCmd(1, 2, 0.1))
      client ! Received(ByteString(data.toByteArray))
      playRoomProbe expectMsg ClientConnected(id)
      playRoomProbe expectMsg ((id, data))
    }

    "receive new PlayRoomState " in new scope {
      val ps = PlayRoomState(100)
      client ! ps
      expectMsg(ps)
    }

    "receive event messages" in new scope {
      val em = EventMsg(10, 20)
      client ! em
      expectMsg(em)
    }
  }
}
