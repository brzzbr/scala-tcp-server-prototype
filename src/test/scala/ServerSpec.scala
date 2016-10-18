import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Tcp.{Bind, CommandFailed, Connected, Register}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.laborunion.project.hollyshit.server.Server
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
  * Created by borisbondarenko on 17.10.16.
  */
class ServerSpec
  extends TestKit(ActorSystem("test"))
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers
    with DefaultTimeout
    with ImplicitSender {

  val port = 1005

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  trait scope {
    var testId: Int = 0
    val tcpProbe = TestProbe()
    val playRoomProbe = TestProbe()
    val clientHandlerProbe = TestProbe()

    val server = TestActorRef(new Server(port){
      override lazy val getTcp: ActorRef = tcpProbe.ref
      override lazy val getPlayRoom: ActorRef = playRoomProbe.ref
      override def getClientHandler(id: Int): ActorRef = { testId = idGenerator; clientHandlerProbe.ref }
    })
  }

  "Server" should "bind to port on start" in new scope {
    tcpProbe expectMsg Bind(server, new InetSocketAddress("localhost", port))
  }

  it should "shutdown on unsuccessful binding" in new scope {
    // Arrange
    val deathWatcher = TestProbe()
    deathWatcher watch server
    // Act
    server ! CommandFailed(Bind(tcpProbe.ref, null, 0, null, pullMode = false))
    // Assert
    deathWatcher expectTerminated server
  }

  it should "create new client handler and register it on new client connection" in new scope {
    // Act
    server ! Connected(null, null)
    // Assert
    expectMsg(Register(clientHandlerProbe.ref))
  }

  it should "increment id on new client connections" in new scope {
    // Pre-Assert
    testId shouldBe 0
    // Act
    server ! Connected(null, null)
    server ! Connected(null, null)
    server ! Connected(null, null)
    // Assert
    testId shouldBe 3
  }
}
