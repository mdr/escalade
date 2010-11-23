package castle.model
import Iterable.{min, max}

trait Region extends Iterable[Point] {

  def points: Set[Point]
  
  def contains( point: Point ): Boolean
  
  def iterator = points.iterator
  
  def contains(anotherRegion: Region): Boolean = anotherRegion.points.subsetOf(points)
  
  def intersects(anotherRegion: Region): Boolean = !anotherRegion.points.intersect(points).isEmpty
     
  def translate(vector: Point): Region

  def width = maxMinusMin(_.row)
  
  def height = maxMinusMin(_.column)
   
  private def maxMinusMin(f: Point => Int) = points.map(f).max - points.map(f).min + 1
 
  def intersects(that: Shape) = points exists (that.points.contains)

}

object Region {
  
  class ContainsOrdering extends PartialOrdering[Region] {

    def tryCompare(region1: Region, region2: Region) = 
      if (region1.points == region2.points)
        Some(0)
      else if (region1.points.subsetOf(region2.points))
        Some(-1)
      else if (region2.points.subsetOf(region1.points))
         Some(1)
      else
         None

    def lteq(region1: Region, region2: Region): Boolean = region1.points.subsetOf(region2.points)
    
    override def equiv(region1: Region, region2: Region): Boolean = region1.points == region2.points  
  } 
  
}
