import org.laborunion.project.hollyshit.events.Respawn
import org.scalatest.{FlatSpec, Matchers}
import org.laborunion.project.hollyshit.server.StateSnapshoter._
import org.laborunion.project.hollyshit.server.Consts._
import org.laborunion.project.hollyshit.servermsgs.ServerEventMsg.Event

/**
  * Created by borisbondarenko on 10.10.16.
  */
class StateSnapshoterSpec
  extends FlatSpec
    with Matchers {

  "getCurrentState" should "produce state with empty players on empty events" in {
    fail
  }

  it should "produce state with nonempty events on empty previous players state" in {
    fail
  }

  it should "filter out out of dates events" in {
    fail
  }

  "updatePlayerStatus" should "should respawn player in Respawn coords" in {
    val res = updatePlayerStatus(defaultPlayer(0), Event.Respawn(Respawn(defaultCoords)))
    res.coords shouldBe defaultCoords
    res.isAlive shouldBe true
  }

  it should "move player with delta coords on Move event" in {
    fail
  }

  it should "ignore events other then Respawn and Move" in {
    fail
  }

  "getPlayersState" should "produce empty seq on empty events and players statuses" in {
    fail
  }

  it should "pass the same statuses in case of empty events" in {
    fail
  }

  it should "get updatet statuses on empty players statuses" in {
    fail
  }

  it should "get updated status for player with existing status and collection of events" in {
    fail
  }

  it should "get updated status for player with no existing status and collection of events" in {
    fail
  }
}
