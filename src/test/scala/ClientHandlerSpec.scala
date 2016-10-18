import akka.actor.ActorSystem
import akka.io.Tcp.{Bind, CommandFailed, PeerClosed}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.laborunion.project.hollyshit.server.ClientHandler
import org.laborunion.project.hollyshit.server.PlayRoom.ClientConnected
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

  "ClientHandler just created" should {
    trait scope {
      val id: Int = 10
      val connectionProbe = TestProbe()
      val playRoomProbe = TestProbe()
      val testMsg = "are you in notInitialized state"

      val client = TestActorRef(new ClientHandler(id, connectionProbe.ref, playRoomProbe.ref) {
        override def notInitialized: Receive = mockReceive orElse super.notInitialized

        // echo on testMsg to test that actor was created in notInitialized state
        def mockReceive: Receive = {
          case msg: String if msg == testMsg => sender ! msg
        }
      })
    }

    "send ClientConnected to play room" in new scope {
      playRoomProbe expectMsg ClientConnected(id)
    }

    "be created in notInitialized state" in new scope {
      client ! testMsg
      expectMsg(testMsg)
    }

    "be shut down on PeerClosed message" in new scope {
      // Arrange
      val deathWatcher = TestProbe()
      deathWatcher watch client
      // Act
      client ! PeerClosed
      // Assert
      deathWatcher expectTerminated client
    }
  }
}
