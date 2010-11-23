package castle.network.protocol
import castle.model._

object ServerMessages {

  abstract sealed class ServerMessage
  
  case class ArenaUpdate(
    rows: Int, 
    columns: Int, 
    gameStage: GameStage,
    stageEndTime: Option[Time],
    players: PlayerManager, 
    walls: Set[Wall], 
    castles: Set[Castle], 
    cannons: Set[Cannon],
    projectiles: Set[Projectile],
    events: List[GameEvent]
  ) extends ServerMessage
  
}
