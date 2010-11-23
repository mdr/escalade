package castle.model
import matt.utils.BoneHeadHashCode 

case class IrregularRegion(points: Set[Point]) extends Region  {
  
  def contains( point: Point ) = points contains point

  override def toString = getClass.getSimpleName + "(" + points.toSeq.take(4) + "[" + points.size +"])"
  
  override def hashCode(): Int = points.hashCode
 
  def translate(vector: Point) = Shape(points.map( _ + vector ))
  
}
