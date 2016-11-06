import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.laborunion.project.hollyshit.server.PlayRoom
import org.laborunion.project.hollyshit.server.PlayRoom.{ClientConnected, GetCurrentState}
import org.laborunion.project.hollyshit.servermsgs.{EventMsg, PlayRoomState}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by borisbondarenko on 06.11.16.
  */
class PlayRoomSpec
  extends TestKit(ActorSystem("test"))
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with DefaultTimeout
    with ImplicitSender {

  val id = 100500

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "PlayRoom core functionality (without periodical state snapshoting" should {

    val firstClientProbe = TestProbe()
    val secondClientProbe = TestProbe()

    trait scope {

      case object GetClients
      case object GetEventBuffer

      val playRoom = TestActorRef(new PlayRoom(id) {
        override val delay = 100.seconds
        override def preStart() = ()
        override def postStop() = ()

        override def receive = super.receive orElse {
          case GetClients => sender ! players
          case GetEventBuffer => sender ! eventBuffer
        }

        players ++= Map(315 -> firstClientProbe.ref, 920 -> secondClientProbe.ref)
      })
    }

    "receive new RoomState and return it on demand" in new scope {
      val state = PlayRoomState(100500)
      playRoom ! state
      playRoom ! GetCurrentState
      expectMsg(state)
    }

    "on receive new client message" should {
      "send event to all clients" in new scope {
        val event = EventMsg(315, 920)
        playRoom ! event
        firstClientProbe expectMsg event
        secondClientProbe expectMsg event
      }

      "put event to buffer" in new scope {
        val events = Vector(
          EventMsg(1, System.currentTimeMillis),
          EventMsg(2, System.currentTimeMillis),
          EventMsg(3, System.currentTimeMillis))
        events.foreach(playRoom ! _)
        playRoom ! GetEventBuffer
        expectMsg(events)
      }
    }

    "on connect new client" should {
      "response with current state" in new scope {
        val thirdClient = TestProbe()
        val state = PlayRoomState(100500)
        playRoom ! state
        thirdClient.send(playRoom, ClientConnected(420))
        thirdClient expectMsg state
      }

      "add new client in clients map" in new scope {
        val thirdClient = TestProbe()
        thirdClient.send(playRoom, ClientConnected(420))
        playRoom ! GetClients

        expectMsg {
          Map(315 -> firstClientProbe.ref, 920 -> secondClientProbe.ref, 420 -> thirdClient.ref)
        }
      }

      "respawn new player" in new scope {
        val thirdClient = TestProbe()
        thirdClient.send(playRoom, ClientConnected(420))
        playRoom ! GetEventBuffer

        fishForMessage(2.seconds) {
          case Vector(single) =>
            val e = single.asInstanceOf[EventMsg]
            if (e.objectId == 420 &&
              e.event.isRespawn) true
            else false
        }
      }
    }
  }

  "PlayRoom periodical state snapshoting" should {
    trait scope {
      val playRoom = TestActorRef(new PlayRoom(id) with StabEchoReceiver{
        override val delay: FiniteDuration = 0.seconds
        override def receive: Receive = {
          case state: PlayRoomState => testActor ! state
        }
      })
    }

    "after period send new state to self" in new scope {
      fishForMessage(2.seconds) {
        case s: PlayRoomState => true
      }
    }
  }
}
