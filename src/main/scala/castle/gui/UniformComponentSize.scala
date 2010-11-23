package castle.gui

import javax.swing.JComponent
import java.awt.Dimension

trait UniformComponentSize extends JComponent {
  this: Any {
    def componentSize: Dimension
  } =>
  
  override def getPreferredSize = componentSize
  override def getMinimumSize = componentSize
  override def getMaximumSize = componentSize
  
}
