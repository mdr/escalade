package castle.model

trait IArenaModelListener { 

  def modelChanged(newArenaModel: IArenaModel, gameEvents: List[GameEvent])
  
}
