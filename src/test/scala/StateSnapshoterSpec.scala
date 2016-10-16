import org.laborunion.project.hollyshit.server.Consts._
import org.laborunion.project.hollyshit.server.StateSnapshoter._
import org.laborunion.project.hollyshit.servermsgs.EventMsg.Event
import org.laborunion.project.hollyshit.servermsgs._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by borisbondarenko on 10.10.16.
  */
class StateSnapshoterSpec
  extends FlatSpec
    with Matchers {

  "getCurrentState" should "produce state with empty players on empty events" in {
    // Arrange
    val ps = PlayRoomState(10)
    val ev = Vector.empty
    // Act
    val res = getCurrentState(ps, ev)
    // Assert
    res.time should not be 10
    res.players should have size 0
  }

  it should "produce state with nonempty events on empty previous players state" in {
    // Arrange
    val ps = PlayRoomState(10).withPlayers(Seq(
      PlayerStatus(1, isAlive = true, defaultCoords),
      PlayerStatus(2, isAlive = true, defaultCoords),
      PlayerStatus(3, isAlive = true, defaultCoords)
    ))
    val ev = Vector(
      EventMsg(1, 1).withRespawn(Respawn(defaultCoords)),
      EventMsg(1, 12).withRespawn(Respawn(defaultCoords)),
      EventMsg(2, 13).withRespawn(Respawn(defaultCoords)),
      EventMsg(3, 14).withRespawn(Respawn(defaultCoords)),
      EventMsg(4, 15).withRespawn(Respawn(defaultCoords))
    )
    // Act
    val res = getCurrentState(ps, ev)
    // Assert
    res.time should not be 10
    res.players should have size 4
  }

  "filterEvents" should "filter out out of dates events" in {
    // Arrange
    val ev = Vector(
      EventMsg(1, 1).withRespawn(Respawn(defaultCoords)),
      EventMsg(2, 1).withRespawn(Respawn(defaultCoords)),
      EventMsg(3, 1).withRespawn(Respawn(defaultCoords)),
      EventMsg(4, 10).withRespawn(Respawn(defaultCoords))
    )
    // Act
    val res = filterEvents(ev, 5)
    // Assert
    res should have size 1
    res should contain key 4
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
    getPlayersState(ps.map(p => p.id -> p).toMap, Map.empty) should contain only (ps : _*)
  }

  it should "get update statuses on empty players statuses" in {
    val ev = Vector(
      EventMsg(1, 1).withRespawn(Respawn(PlayerCoords(1, 2, 0.3))),
      EventMsg(2, 1).withRespawn(Respawn(PlayerCoords(2.2, 2.2, -0.1))),
      EventMsg(3, 1).withRespawn(Respawn(PlayerCoords(1.9, 2.1, 0.5)))
    )
    val ps = Seq(
      PlayerStatus(1, isAlive = true, PlayerCoords(1, 2, 0.3)),
      PlayerStatus(2, isAlive = true, PlayerCoords(2.2, 2.2, -0.1)),
      PlayerStatus(3, isAlive = true, PlayerCoords(1.9, 2.1, 0.5))
    )
    getPlayersState(Map.empty, ev.groupBy(_.objectId)) should contain only (ps : _*)
  }

  it should "get updated status for moving player with existing status and collection of events" in {
    val is = Seq(
      PlayerStatus(1, isAlive = true, PlayerCoords(10, 20, 0.0))
    )
    val ev = Vector(
      EventMsg(1, 2).withMove(Move(1, 1, 0.3)),
      EventMsg(1, 3).withMove(Move(-2, -2, 0.0)),
      EventMsg(1, 4).withMove(Move(0, 2, -0.6))
    )
    val ps = Seq(
      PlayerStatus(1, isAlive = true, PlayerCoords(9, 21, -0.3))
    )
    getPlayersState(is.map(p => p.id -> p).toMap, ev.groupBy(_.objectId)) should contain only (ps : _*)
  }

  it should "get updated status for player with no existing status and collection of events" in {
    val ev = Vector(
      EventMsg(1, 1).withRespawn(Respawn(defaultCoords)),
      EventMsg(1, 2).withMove(Move(1, 1, 0.3)),
      EventMsg(1, 3).withMove(Move(-2, -2, 0.0)),
      EventMsg(1, 4).withMove(Move(0, 2, -0.6))
    )
    val ps = Seq(
      PlayerStatus(1, isAlive = true, PlayerCoords(-1, 1, -0.3))
    )
    getPlayersState(Map.empty, ev.groupBy(_.objectId)) should contain only (ps : _*)
  }
}
