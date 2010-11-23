package castle.model
// TODO: Rename, e.g., PhaseQueries or PhaseInfo
abstract sealed class GamePhase {

  val stageEndTime: Option[Time]  

}


abstract class GameOverPhase extends GamePhase {
  
  val stageEndTime = None

  val winner: Option[Player]

}

abstract class WallBuildPhase extends GamePhase {
  
  def shape(player: Player): Shape
  
  def canPlaceBuildCursor(player: Player, location: Point, orientation: Orientation): Boolean
   
  def canBuild(player: Player, location: Point, orientation: Orientation): Boolean

  def rotateShapeAndAdjust(player: Player, location: Point, orientation: Orientation): (Point, Orientation)
  
  def adjustToBounds(player: Player, location: Point, orientation: Orientation): Point
}

abstract class CannonBuildPhase extends GamePhase {

  def buildPoints(player: Player): Int

  def canPlaceBuildCursor(player: Player, location: Point, cannonType: CannonType): Boolean
   
  def canBuild(player: Player, location: Point, cannonType: CannonType): Boolean

  def cycleToNextCannonAndAdjust(player: Player, location: Point, cannonType: CannonType): (Point, CannonType)

  def adjustToBounds(player: Player, location: Point, cannonType: CannonType): Point

}

abstract class BattlePhase extends GamePhase {

  def canFire(player: Player, currentTime: Time): Boolean
  
}


