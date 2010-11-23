package castle.gui

import java.io.File
import matt.utils.Sound
object Sounds {

  private val SFX_PREFIX = "sound/" // TODO: switch to classpath resource loading

  private def loadClip(fileName: String): Sound = {
    new Sound(getClass.getClassLoader.getResource(SFX_PREFIX + fileName))
  }
  
  // Open office
  val APPLAUSE = loadClip("applause.wav")
  
  // Following are all Castle Combat GPL v2+
  val CANNON_FIRE = loadClip("cannon.wav")
  
  // Wesnoth: GPL v2
  val NEW_ROUND = loadClip("bell.wav")
  val BUILD = loadClip("contract.wav")
  
  // ltris, GPL
  val PROJECTILE_HIT = loadClip("explosion.wav")
}
 
