package castle.gui

import javax.imageio.ImageIO
import java.io.File

object Images {
   private val GFX_PREFIX = "gfx/" // TODO: switch to classpath resource loading
   
   // Following are all Castle Combat GPL v2+:
   val WALL_IMAGE = loadImage( "wall.png") 
   val MAIN_CASTLE_IMAGE = loadImage( "bigcastle.png" ) 
   val ORDINARY_CASTLE_IMAGE = loadImage( "castle.png" ) 
   val CANT_BUILD_IMAGE = loadImage( "cantbuild.png" )
   val CAN_BUILD_IMAGE = loadImage( "select.png" )
   val CANNON_SHOT = loadImage("shot.png")
   val CROSSHAIR = loadImage("crosshair.png")
   val CANNOT_SHOOT_CROSSHAIR = loadImage("notready.png")
   val EXPLOSION_1 = loadImage("garbage-new.png")
   val EXPLOSION_2 = loadImage("garbage-med.png")
   val EXPLOSION_3 = loadImage("garbage-old.png")
   
   
   val SMALL_CANNON_IMAGES = (1 to 30).map( smallCannonFileName ).map( loadImage )
   private def smallCannonFileName(i: Int) = "cannon00" + ( if (i >= 10) "" else "0" ) + i + ".png"
   val DEFAULT_SMALL_CANNON = SMALL_CANNON_IMAGES(0)
   val DESTROYED_SMALL_CANNON = loadImage("destroyed-cannon1.png")
   
   val LARGE_CANNON_IMAGES = (1 to 30).map( largeCannonFileName ).map( loadImage )
   private def largeCannonFileName(i: Int) = "bigcannon00" + ( if (i >= 10) "" else "0" ) + i + ".png"
   val DEFAULT_LARGE_CANNON = LARGE_CANNON_IMAGES(0)
   val DESTROYED_LARGE_CANNON = loadImage("destroyed-bigcannon1.png")
   
   private def loadImage(name: String) = ImageIO.read(getClass.getClassLoader.getResource(GFX_PREFIX + name)) 

}
