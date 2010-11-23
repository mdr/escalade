package castle.gui
import castle.model.Time

trait ICurrentTimeGetter {

  def currentTime(): Time
  
}
