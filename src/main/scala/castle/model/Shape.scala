package castle.model

import matt.utils.BoneHeadHashCode 
import matt.utils.CollectionUtils._ 
import Iterable.{min, max}

case class Orientation(val rotations: Int) {
  require(rotations >= 0 and rotations < 4)
  
  def next = Orientation((rotations + 1) % 4)
}

case class Shape(override val points: Set[Point]) extends IrregularRegion(points) {
  
  def orient(orientation: Orientation): Shape = rotateRight(orientation.rotations) 
  
  def rotateRight = Shape( points.map( _.rotateRight ) )

  def rotateRight(rotations: Int): Shape = {
    require(rotations >= 0)
    var resultShape = this
    for (i <- 1 to (rotations % 4))
      resultShape = resultShape.rotateRight
    resultShape
  }
   
}

object Shape {
  
   def apply(points: Point*): Shape = Shape(Set(points: _*))
   def makeFromPairs(points: (Int, Int)*): Shape = Shape(Set(points: _*).map(pair => new Point(pair)))
   
}    
object StandardShapes {
  
  //  2|
  //  1|
  //  0|    #  #  #
  // -1|
  // -2|
  //   ---------------
  //    -2 -1  0  1  2 
  val LINE = Shape.makeFromPairs( (0, -1), (0, 0), (0, 1) )
   
  //  2|
  //  1|
  //  0|    #  #  #
  // -1|          #
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val L = Shape.makeFromPairs( (0, -1), (0, 0), (0, 1), (1, 1 ) )
   
   
  //  2|
  //  1|
  //  0|    #  #  #
  // -1|    #        
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val MIRROR_L = Shape.makeFromPairs( (0, -1), (0, 0), (0, 1), (-1, 1 ) )

  //  2|
  //  1|
  //  0|    #  #  #
  // -1|    #     #  
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val U = Shape.makeFromPairs( (0, -1), (0, 0), (0, 1), (1, -1), (1, 1) )
   
  //  2|
  //  1|
  //  0|       #   
  // -1|             
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val DOT = Shape.makeFromPairs( (0, 0) )

  //  2|
  //  1|       #   
  //  0|    #  #  #
  // -1|             
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val T = Shape.makeFromPairs((0, -1), (0, 0), (0, 1), (1, 0))

  //  2|
  //  1|
  //  0|    #  #   
  // -1|       #  #
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val Z = Shape.makeFromPairs( (0, -1), (0, 0), (-1, 0), (-1, 1 ) )

  //  2|
  //  1|
  //  0|       #  #
  // -1|    #  #   
  // -2|  
  //   ---------------
  //    -2 -1  0  1  2 
  val S = Shape.makeFromPairs( (-1, -1), (-1, 0), (0, 0), (0, 1 ) )

  
  val ALL = Set(LINE, L, MIRROR_L, U, DOT, T, Z, S)

  val largestWidthOrHeight = ALL.map(shape => Math.max(shape.width, shape.height)).max
   
}

   
