/*package castle.client
import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._
import castle.model._
import matt.utils.RandomUtils
import matt.utils.RandomUtils.randomToRandomUtils
import scala.util.Random

class MockServer extends IClientMessageSender {

  private var currentArenaModel: ArenaModel = ArenaModel.initialModel(30, 40)

  def getArenaModel = currentArenaModel
  
  private val random = new Random
  
  def sendMessage(message: ClientMessage) = {
    message match {
      case DoBuildAtCursor(player) => {  
        val nextShape = random.randomElementOf(StandardShapes.ALL)
        val rotations = random.randomElementOf(0 to 3)
        val rotatedShape = nextShape.rotateRight(rotations)
        val newArenaModel = currentArenaModel.doBuildAtCursor(player, rotatedShape)
        updateArena(newArenaModel) 
      }
      case RotateBuildCursor(player: Player) => updateArena(currentArenaModel.rotateBuildCursor(player)) 

      case MoveBuildCursor(player: Player, direction: Direction) => updateArena( currentArenaModel.moveBuildCursor(player, direction) )

    }
  }

  private def updateArena(newModel: ArenaModel) {
    currentArenaModel = newModel
    sendServerMessage( ArenaUpdate(newModel.rows, newModel.columns, newModel.players, newModel.walls) )
  }
  
  private var listeners: Set[IServerMessageListener] = Set.empty
  def addServerMessageListener(listener: IServerMessageListener) = { listeners += listener }
  def removeServerMessageListener(listener: IServerMessageListener) = { listeners -= listener }
  private def sendServerMessage(message: ServerMessage) = { listeners.foreach( _.messageFromServer(message) ) }
  
}
 */