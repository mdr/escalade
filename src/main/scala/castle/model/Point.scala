package castle.model

import matt.utils.CollectionUtils._

case class Point(row: Int, column: Int) {
  
  def this(pair: (Int, Int)) =  this(pair._1, pair._2) 
  
   // Have a different class from Point to represent vectors?
   def +(other: Point) = Point(row + other.row, column + other.column)
   
   def +(direction: Direction) = Point(row + direction.rowDelta, column + direction.columnDelta)
   
   def rotateRight = Point(column, -row)
   
   def neighbours: Set[Point] = for {
     neighbourRow <- set(row - 1 to row + 1)
     neighbourColumn <- column - 1 to column + 1
     neighbourPoint = Point(neighbourRow,  neighbourColumn)
     if neighbourPoint != this
   } yield neighbourPoint
   
   override def hashCode = row + 37 * column
   
   def distanceTo(other: Point): Double = {
     def square(x: Int) = x * x
     Math.sqrt(square(this.row - other.row) + square(this.column - other.column))
   }
   
}
