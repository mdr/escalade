package castle.network.protocol

import castle.model._

object ClientMessages {

  abstract sealed class ClientMessage
  
  case class DoWallBuild(player: Player, location: Point, orientation: Orientation) extends ClientMessage
  
  case class DoCannonBuild(player: Player, location: Point, cannonType: CannonType) extends ClientMessage 
  
  case class Shoot(player: Player, target: Point) extends ClientMessage

  case object SkipPhase extends ClientMessage
  
}
