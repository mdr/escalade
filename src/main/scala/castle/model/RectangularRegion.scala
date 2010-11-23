package castle.model
import matt.utils.CollectionUtils._ 
import matt.utils.BoneHeadHashCode 

case class RectangularRegion(val topLeft: Point, val bottomRight: Point) extends Iterable[Point] with Region with BoneHeadHashCode {

  def contains( point: Point ) = {
    implicit def pointToPartialOrder(point: Point): PartiallyOrdered[Point] = point match {
      case Point(thisRow, thisColumn) => new PartiallyOrdered[Point] {
        def tryCompareTo[B >: Point <% PartiallyOrdered[B]](otherPoint: B) = { otherPoint match { 
          case Point(thatRow, thatColumn) => 
            if (point == otherPoint) Some(0)
            else if (thisRow <= thatRow && thisColumn <= thatColumn) Some(-1)
            else if (thisRow >= thatRow && thisColumn >= thatColumn) Some(1)
            else None
        }}	
      }		
    }
    point >= topLeft && point <= bottomRight
    
  }
  
  def points: Set[Point] = {
    set(
      for (row <- topLeft.row to bottomRight.row; column <- topLeft.column to bottomRight.column)
         yield Point(row, column)
      )
  }
  
  def translate(vector: Point): RectangularRegion = RectangularRegion(topLeft + vector, bottomRight + vector)

}

object RectangularRegion {
  
  // Constructs a square region of the given size with the top left corner at the given point
  def squareAt(point: Point, size: Int) = RectangularRegion(point, point + Point(size -1, size -1))
}