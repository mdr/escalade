package castle.server

import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._
import castle.model._
import matt.utils.RandomUtils
import matt.utils.RandomUtils.randomToRandomUtils
import scala.util.Random
import java.util.concurrent.locks.ReentrantLock
import matt.utils.ThreadUtils._

trait IClientMessageHandler {
  
  def doWallBuild(player: Player, location: Point, orientation: Orientation)

  def doCannonBuild(player: Player, location: Point, cannonType: CannonType) 
  
  def shoot(player: Player, target: Point)
  
  def skipPhase()
  
}

object ServerCoordinator {
   private val UPDATES_PER_SECOND = 25
   private val TICK = Milliseconds(1000 / UPDATES_PER_SECOND)
}

class ServerCoordinator(messageSender: IServerMessageSender) extends IClientMessageListener {

  private var currentArenaModel: ArenaModel = ArenaModel.initialModel(30, 40, Time.now())
  // private var currentArenaModel: ArenaModel = ArenaModel.initialModel(40, 70, Time.now())

  private val mutex = new ReentrantLock
  
  def getArenaModel = currentArenaModel
  
  private val random = new Random
 
  doInNewThread {
    while (true) {
      Thread.sleep(ServerCoordinator.TICK.ms)
      withLock(mutex) {
        val currentTime = Time.now()
        for ((newArenaModel, gameEvents) <- currentArenaModel.update(currentTime))
          updateArena(newArenaModel, gameEvents)
      }
    }
  }
  
  def messageFromClient(message: ClientMessage) = {
    withLock(mutex) {
      val clientMessageHandler = getClientMessageHandler(currentArenaModel.getGamePhaseInfo)
      import clientMessageHandler._
      message match {
        
        case DoWallBuild(player, location, orientation) => doWallBuild(player, location, orientation)

        case DoCannonBuild(player, location, cannonType) => doCannonBuild(player, location, cannonType) 
        
        case Shoot(player, target) => shoot(player, target)
        
        case SkipPhase => skipPhase()
      }
    }
  }

  private def getClientMessageHandler(gamePhase: GamePhase) = {
    gamePhase match {
      case buildPhase: WallBuildPhase => new WallBuildPhaseClientMessageHandler(buildPhase)
      case buildPhase: CannonBuildPhase => new CannonBuildPhaseClientMessageHandler(buildPhase)
      case battlePhase: BattlePhase => new BattlePhaseClientMessageHandler(battlePhase)
    }
  }
  
  private def updateArena(newModel: ArenaModel) {
    updateArena(newModel, List())
  }
  private def updateArena(newModel: ArenaModel, gameEvents: List[GameEvent]) {
    currentArenaModel = newModel
    messageSender broadcastServerMessage ArenaUpdate(newModel.rows, newModel.columns, newModel.gameStage, newModel.stageEndTimeOption, 
                                                newModel.playerManager, newModel.walls, newModel.castles, newModel.cannons, newModel.projectiles, gameEvents)
  }

  abstract class AbstractClientMessageHandler extends IClientMessageHandler {
    
    def skipPhase() { 
      val (newModel, events) = currentArenaModel.endPhase(Time.now())
      updateArena ( newModel, events) 
    }
    
    def doWallBuild(player: Player, location: Point, orientation: Orientation) { }

    def doCannonBuild(player: Player, location: Point, cannonType: CannonType)  { }

    def shoot(player: Player, target: Point) { }
  }
  
  class WallBuildPhaseClientMessageHandler(buildPhase: WallBuildPhase) extends AbstractClientMessageHandler {

    override def doWallBuild(player: Player, location: Point, orientation: Orientation) { 
      if (buildPhase.canBuild(player, location, orientation)) {
        val randomShape = makeRandomShape()
        val newArenaModel = currentArenaModel.buildWalls(player, location, orientation, randomShape)
        updateArena(newArenaModel) 
      }
    }
    
    private def makeRandomShape() = {
      val randomShape = random.randomElementOf(StandardShapes.ALL)
      val rotations = random.randomElementOf(0 to 3)
      randomShape.rotateRight(rotations)
    }
    
  }

  class CannonBuildPhaseClientMessageHandler(buildPhase: CannonBuildPhase) extends AbstractClientMessageHandler {
  
     override def doCannonBuild(player: Player, location: Point, cannonType: CannonType)  {
       if (buildPhase.canBuild(player, location, cannonType))
        updateArena(currentArenaModel.buildCannon(player, location, cannonType))
     }
    
  }

  class BattlePhaseClientMessageHandler(battlePhase: BattlePhase) extends AbstractClientMessageHandler {

    override def shoot(player: Player, target: Point) {
      val currentTime = Time.now() 
      if (battlePhase.canFire(player, currentTime))
        updateArena( currentArenaModel.shoot(player, target, currentTime) )
    }
  }
  
}