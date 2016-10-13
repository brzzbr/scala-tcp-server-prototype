import org.laborunion.project.hollyshit.server.Consts._
import org.laborunion.project.hollyshit.server.StateSnapshoter._
import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event
import org.laborunion.project.hollyshit.servermsgs.{Move, PlayerCoords, PlayerStatus, Respawn}
import org.scalatest.{FlatSpec, Matchers}

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
    val res = updatePlayerStatus(defaultPlayer(0), Event.Move(Move(1, 2, 0.3)))
    res.coords shouldBe PlayerCoords(1, 2, 0.3)
  }

  "getPlayersState" should "produce empty seq on empty events and players statuses" in {
    getPlayersState(Map.empty, Map.empty) shouldBe Seq.empty
  }

  it should "pass the same statuses in case of empty events" in {
    val ps = Seq(
      PlayerStatus(1, isAlive = true, PlayerCoords(1, 2, 0.3)),
      PlayerStatus(2, isAlive = false, PlayerCoords(2.2, 2.2, -0.1)),
      PlayerStatus(3, isAlive = true, PlayerCoords(1.9, 2.1, 0.5))
    )
    getPlayersState(ps.map(p => p.id -> p).toMap, Map.empty) shouldBe ps
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
