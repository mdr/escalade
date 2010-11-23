package matt.utils

import java.awt.image.BufferedImage
import java.awt.Image
import java.awt.Graphics
import javax.swing.ImageIcon;
import java.awt.Color
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.GraphicsDevice
  
object ImageUtils {

  def managedImage(image: Image): BufferedImage = {
    val graphEnv: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
    val graphDevice: GraphicsDevice = graphEnv.getDefaultScreenDevice
    val graphicConf: GraphicsConfiguration = graphDevice.getDefaultConfiguration
    val bufferedImage = graphicConf.createCompatibleImage(image.getWidth(null), image.getHeight(null))
    val g = bufferedImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    bufferedImage
  }
  
  def copyImage(image: Image): BufferedImage = {
    val loadedImage = new ImageIcon(image).getImage();
    val width = loadedImage.getWidth(null)
    val height = loadedImage.getHeight(null)
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    val g = bufferedImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return bufferedImage;
  }

  def tint(image: Image, colour: Color): BufferedImage = {
    val newImage = copyImage(image);
    val g = newImage.createGraphics();
    g setColor colour
    g.fillRect(0, 0, newImage.getWidth, newImage.getHeight)
    g.dispose()
    return newImage;
  }

  def invert(image: Image): BufferedImage = {
    val newImage = copyImage(image);
    for (x <- 0 until newImage.getWidth;
         y <- 0 until newImage.getHeight) {
      val rgb = new Color(newImage.getRGB(x, y))
      val newRgb = new Color(255 - rgb.getRed, 255 - rgb.getGreen, 255 - rgb.getBlue, rgb.getAlpha).getRGB
      newImage.setRGB(x, y, newRgb)
    }
    return newImage;
  }


}
