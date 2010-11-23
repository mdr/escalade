package matt.utils

import javax.swing.SwingUtilities
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.Graphics2D
import java.awt.Image
import java.awt.event.{FocusEvent,FocusAdapter,FocusListener}
import java.awt.event.{MouseAdapter,MouseEvent,MouseListener}
import java.awt.event.{WindowAdapter,WindowEvent,WindowFocusListener}

object SwingUtils {

  def doOnSwingThread(runnable: => Unit) {
    SwingUtilities invokeLater new Runnable() {
      override def run() { runnable }
    }  
  } 

  def doOnSwingThreadAndBlock(runnable: => Unit) {
    SwingUtilities invokeAndWait new Runnable() {
      override def run() { runnable }
    }  
  } 
  
  implicit def graphics2RichGraphics(graphics: Graphics): RichGraphics = new RichGraphics(graphics.asInstanceOf[Graphics2D])
  
  implicit def graphics2Graphics2D(graphics: Graphics): Graphics2D = graphics.asInstanceOf[Graphics2D]
  
  def deriveGraphics(graphics: Graphics)(f: Graphics2D => Unit) {
    val g = graphics.create
    try {
      f(g)
    } finally {
      g.dispose()
    }
  }

  def focusLost(f: FocusEvent => Unit): FocusListener = {
    new FocusAdapter {
      override def focusLost(e: FocusEvent) = f(e)  
    }
  }
  def focusLost(f: => Unit): FocusListener = {
    new FocusAdapter {
      override def focusLost(e: FocusEvent) = f
    }
  }
  def mouseClicked(f: MouseEvent => Any): MouseListener = {
    new MouseAdapter {
      override def mouseClicked(e: MouseEvent) = f(e)  
    }
  }
  def mouseClicked(f: => Any): MouseListener = {
    new MouseAdapter {
      override def mouseClicked(e: MouseEvent) = f
    }
  }
  def windowGainedFocus(f: WindowEvent => Unit): WindowFocusListener = {
    new WindowAdapter {
      override def windowGainedFocus(e: WindowEvent) = f(e)  
    }
  }
  def windowGainedFocus(f: => Any): WindowFocusListener = {
    new WindowAdapter {
      override def windowGainedFocus(e: WindowEvent) = f
    }
  }

  
}

object RichGraphics {
  def getDrawRegionForDrawImageCentered(image: Image, x: Int, y: Int): Rectangle = {
    val width = image.getWidth(null)
    val height = image.getHeight(null)
    val topLeftX = x - width / 2
    val topLeftY = y - height / 2
    new Rectangle(topLeftX, topLeftY, width, height)
  }

}

class RichGraphics(graphics: Graphics2D) {
  
  def drawImage(image: Image, x: Int, y: Int) { graphics.drawImage(image, x, y, null) }
  
  /**
   * Draws the image at the given location, and returns the rectangle that the draw operation affected
   */
  def drawImageCentered(image: Image, x: Int, y: Int): Rectangle = {
    val drawRectangle = RichGraphics.getDrawRegionForDrawImageCentered(image, x, y)
    graphics.drawImage(image, drawRectangle.x, drawRectangle.y, null)
    drawRectangle
  }
  
  def drawRepaintBounds() {
    val bounds = graphics.getClipBounds
    val originalColour = graphics.getColor
    graphics setColor Color.RED
    graphics.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2)
    graphics setColor originalColour
  } 
  
  def fillRect(rect: Rectangle) { graphics.fillRect(rect.x, rect.y, rect.width, rect.height) }
  
  def hitClip(rect: Rectangle) = graphics.hitClip(rect.x, rect.y, rect.width, rect.height)
  
}