package castle.model

trait IArenaModelContainer {

  def arenaModel: IArenaModel
  
  def addArenaModelListener(listener: IArenaModelListener)

  def removeArenaModelListener(listener: IArenaModelListener)
  
}
