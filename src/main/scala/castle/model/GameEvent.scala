package castle.model

abstract sealed class GameEvent
  
case object NewStage extends GameEvent 
case object GameOver extends GameEvent  
