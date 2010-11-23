package castle.model

import matt.utils.CollectionUtils._
import matt.utils.BoneHeadHashCode

case class PlayerInfo(
    alive: Boolean,
    currentBuildShape: Shape, 
    buildPoints: Int
  ) extends BoneHeadHashCode {
  
  require( buildPoints >= 0)
  def addBuildPoints(points: Int): PlayerInfo = PlayerInfo(alive, currentBuildShape, buildPoints + points)
  def subtractBuildPoints(points: Int): PlayerInfo = addBuildPoints(-points)
  def setCurrentBuildShape(newBuildShape: Shape): PlayerInfo = PlayerInfo(alive, newBuildShape,  buildPoints)
  def playerDied = PlayerInfo(false, currentBuildShape, buildPoints)
}

case class PlayerManager(playerMap: Map[Player, PlayerInfo]) extends Iterable[(Player, PlayerInfo)] {

  def allPlayers: Set[Player] = playerMap.keySet
  def currentBuildShape(player: Player): Shape = playerMap(player).currentBuildShape
  def isAlive(player: Player) = playerMap(player).alive
  def buildPoints(player: Player) = playerMap(player).buildPoints
  private def update(player: Player, updateFn: PlayerInfo => PlayerInfo) = PlayerManager(playerMap + (player -> updateFn(playerMap(player))))
  
  def addBuildPoints(player: Player, points: Int) = update(player, _.addBuildPoints(points))
  def subtractBuildPoints(player: Player, points: Int) = update(player, _.subtractBuildPoints(points))
  def setCurrentBuildShape(player: Player, newBuildShape: Shape) = update(player, _.setCurrentBuildShape(newBuildShape))
  def playerDied(player: Player) = update(player, _.playerDied)
  def iterator = playerMap.iterator

  def totalBuildPoints = allPlayers.toList.map(buildPoints).sum
  
}
