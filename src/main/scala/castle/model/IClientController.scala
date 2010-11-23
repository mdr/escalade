package castle.model

trait IClientController {

  def doWallBuild(player: Player, location: Point, orientation: Orientation) 

  def doCannonBuild(player: Player, location: Point, cannonType: CannonType) 

  def skipPhase()

  def shoot(player: Player, target: Point)
  
}
