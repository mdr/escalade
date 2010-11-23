package castle.model

import matt.utils.BoneHeadHashCode
import matt.utils.CollectionUtils._

case class Player(number: Int) {
  override def hashCode(): Int = number // TODO: Workaround for stable-across-serialization hashCode bug
}

abstract sealed case class ArenaObject(val location: Point, val size: Int) {
  require(location.row >= 0 && location.column >= 0 && size > 0)
  val region = RectangularRegion(location, location + Point(size - 1, size - 1))
}

case class Wall(override val location: Point, player: Player) extends ArenaObject(location, 1) {
  override def hashCode(): Int = player.hashCode + location.hashCode * 37 // TODO: Workaround for stable-across-serialization hashCode bug
}

case class Castle(override val location: Point) extends ArenaObject(location, 2) {
  override def hashCode(): Int = location.hashCode
}

case class Cannon(
  override val location: Point,
  player: Player,
  cannonType: CannonType,
  hitPoints: Int,
  reloadingUntil: Option[Time]) extends ArenaObject(location, cannonType.size) {

  require(hitPoints >= 0 && hitPoints <= cannonType.hitPoints)
  require(size > 0)

  private val RELOAD_TIME = Milliseconds(3000)

  val destroyed = hitPoints == 0

  def canFire(currentTime: Time) = not(destroyed) && not(isReloading(currentTime))

  private def isReloading(currentTime: Time) = {
    reloadingUntil match {
      case Some(reloadingUntilTime) => currentTime < reloadingUntilTime
      case None => false
    }
  }

  def damage(damagePoints: Int): Cannon = Cannon(location, player, cannonType, Math.max(0, hitPoints - damagePoints), reloadingUntil)

  def fire(currentTime: Time): Cannon = {
    require(not(destroyed) && reloadingUntil.forall(currentTime.>=))
    Cannon(location, player, cannonType, hitPoints, Some(currentTime + RELOAD_TIME))
  }

  def setOwner(newPlayer: Player): Cannon = Cannon(location, newPlayer, cannonType, hitPoints, reloadingUntil)

}

case class Projectile(origin: Point, destination: Point, startTime: Time, hitTime: Time) {
  require(hitTime >= startTime)
}

abstract sealed case class CannonType(val cost: Int, val size: Int, val hitPoints: Int) {
  require(cost >= 0); require(size >= 1); require(hitPoints >= 1)

  def regionIfBuildAt(location: Point) = RectangularRegion.squareAt(location, size)
}
case object SmallCannonType extends CannonType( /* cost = */ 1, /* size = */ 2, /* hitPoints = */ 4)
case object LargeCannonType extends CannonType( /* cost = */ 3, /* size = */ 3, /* hitPoints = */ 8)

trait IArenaModel {

  def rows: Int

  def columns: Int

  def getGamePhaseInfo: GamePhase

  def objects: Set[ArenaObject]

  val projectiles: Set[Projectile]

  def getOwner(point: Point): Option[Player]

  def isAlive(player: Player): Boolean

  def allPlayers: Set[Player]

}

// TODO: Consider moving phase specific operations to a phase-specific type 
trait IEditableArenaModel extends IArenaModel {

  def endPhase(currentTime: Time): (IEditableArenaModel, List[GameEvent])

  def buildWalls(player: Player, location: Point, orientation: Orientation, nextShape: Shape): IEditableArenaModel

  def buildCannon(player: Player, location: Point, cannonType: CannonType): IEditableArenaModel

  def shoot(player: Player, target: Point, currentTime: Time): IEditableArenaModel

  def update(currentTime: Time): Option[(IEditableArenaModel, List[GameEvent])]

}
