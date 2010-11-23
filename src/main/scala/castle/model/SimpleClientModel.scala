/*
package castle.model
import matt.utils.RandomUtils
import matt.utils.RandomUtils.randomToRandomUtils
import scala.util.Random

class SimpleClientModel extends IClientModel {
  
  private val random = new Random

  var currentArenaModel: IEditableArenaModel = ArenaModel.initialModel(30, 40)
  var modelListeners: Set[IArenaModelListener] = Set.empty
  
  def arenaModel: IEditableArenaModel = currentArenaModel
  
  def doBuildAtCursor(player: Player) {
    val nextShape = random.randomElementOf(StandardShapes.ALL)
    val rotations = random.randomElementOf(List(0, 1, 2, 3))
    val rotatedShape = nextShape.rotateRight(rotations)
    val newArenaModel = currentArenaModel.doBuildAtCursor(player, rotatedShape)
    currentArenaModel = newArenaModel
    fireModelChangedEvent()
  }

  def rotateBuildCursor(player: Player) {
    val newArenaModel = currentArenaModel.rotateBuildCursor(player)
    currentArenaModel = newArenaModel
    fireModelChangedEvent()
  } 

  def moveBuildCursor(player: Player, direction: Direction) {
    val newArenaModel = currentArenaModel.moveBuildCursor(player, direction)
    currentArenaModel = newArenaModel
    fireModelChangedEvent()
  } 


  def addArenaModelListener(listener: IArenaModelListener) {
    modelListeners += listener
  }
  
  private def fireModelChangedEvent() {
    modelListeners.foreach( _.modelChanged(currentArenaModel) )
  }
  
}
*/