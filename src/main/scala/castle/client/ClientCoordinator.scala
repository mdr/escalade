package castle.client

import castle.model._
import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._

class ClientCoordinator(messageSender: IClientMessageSender, initialArena: IArenaModel) 
         extends IClientController with IArenaModelContainer with IServerMessageListener {
 
  
  def doWallBuild(player: Player, location: Point, orientation: Orientation) {
    messageSender sendClientMessage DoWallBuild(player, location, orientation)
  }
  def doCannonBuild(player: Player, location: Point, cannonType: CannonType) {
    messageSender sendClientMessage DoCannonBuild(player, location, cannonType) 
  } 
  
  
           
  def skipPhase() { messageSender sendClientMessage SkipPhase }
  
  def shoot(player: Player, target: Point) { messageSender sendClientMessage Shoot(player, target) }
  
  // Model updates and listeners
  // ---------------------------
    
  private var localArenaModel: IArenaModel = initialArena
  
  private var listeners: Set[IArenaModelListener] = Set.empty
  
  def arenaModel: IArenaModel = localArenaModel
  
  def addArenaModelListener(listener: IArenaModelListener) = { listeners = listeners + listener }

  def removeArenaModelListener(listener: IArenaModelListener) = { listeners = listeners - listener }

  def fireModelChangedEvent(model: IArenaModel, gameEvents: List[GameEvent]) { listeners.foreach( _.modelChanged(model, gameEvents)) }
  
  def setModel(newArenaModel: IArenaModel, gameEvents: List[GameEvent]) = { localArenaModel = newArenaModel; fireModelChangedEvent(newArenaModel, gameEvents) }
  
  def messageFromServer(message: ServerMessage) {
    message match { 
      case ArenaUpdate(rows, columns, gameStage, stageEndTime, playerManager, walls, castles, cannons, projectiles, gameEvents) => 
        setModel( new ArenaModel(rows, columns, gameStage, stageEndTime, playerManager, walls, castles, cannons, projectiles, None ), gameEvents ) 
    } 
  }
  
  private def log(message: String) = println(message)
}
