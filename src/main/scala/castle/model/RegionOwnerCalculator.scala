package castle.model

import matt.utils.MaximalElementFinder
import matt.utils.CollectionUtils._

object RegionOwnerCalculator {
  
  /**
   * A view on the game arena needed by RegionOwnerCalculator
   */
  trait IArenaView {

    val rows: Int
  
    val columns: Int
  
    def players: Set[Player]
  
    def wallPoints(player: Player): Set[Point]
  
    def containsWallOwnedByPlayer(point: Point, player: Player): Boolean
  
  }
  
}

/**
 * Calculates who owns what parts of the game map given the current set of walls.  
 */
class RegionOwnerCalculator(private val arenaView: RegionOwnerCalculator.IArenaView) { 
  
  type Grid[X] = Array[Array[X]]
  
  import arenaView._
    
  def maximalRegions: RegionOwnerInfo = {
    var allRegions: Set[Region] = Set.empty
    var regionOwnerMap: Map[Region, Player] = Map.empty
    for (player <- players) {
      val regionsControlledByPlayer = calculateControlledRegions(player)
      for (region <- regionsControlledByPlayer)
        regionOwnerMap += (region -> player)
      allRegions ++= regionsControlledByPlayer
    }
    val maximalRegions = new MaximalElementFinder(new Region.ContainsOrdering).findMaximalElements(allRegions)
    RegionOwnerInfo(map(
      for (maximalRegion <- maximalRegions)
        yield (maximalRegion -> regionOwnerMap( maximalRegion ) ) 
    ))
  }
  

  private val WALL = -1
  private val UNSEEN = 0

  private def initWallGrid(player: Player): Grid[Int] = {
    val wallGrid: Grid[Int] = new Array(rows)
    for (row <- 0 until rows) {
      wallGrid(row) = new Array(columns)
      for (column <- 0 until columns)
        wallGrid(row)(column) = if (containsWallOwnedByPlayer(Point(row, column), player)) WALL else UNSEEN
    }
    wallGrid
  }
  
  /**
   * Sweep across the grid, marking squares with a number indicating a contiguious region.
   * Regions get joined when they are discovered to be connected via the "canonical" map; for example, if 
   * regions 1, 3 and 7 are connected, then the canonical(1)=canonical(3)=canonical(7) = 1.
   */
  private def calculateControlledRegions(player: Player): Set[Region] = {
    val wallGrid = initWallGrid(player)
    var canonical: Map[Int, Int] = Map.empty
    var nextRegionNumber = 1
    for (row <- 0 until rows; column <- 0 until columns) {
      val valueAtPos = wallGrid(row)(column)
      require( valueAtPos <= 0 )
      if (valueAtPos == UNSEEN) {
        val neighbours = lookupNeighbours(row, column, wallGrid, canonical)
        if (neighbours.isEmpty) {
          wallGrid(row)(column) = nextRegionNumber
          canonical += (nextRegionNumber -> nextRegionNumber)
          nextRegionNumber += 1
        } else if (neighbours.size == 1) {
          val neighbour = getOnlyElement(neighbours)
          wallGrid(row)(column) = neighbour
        } else {
          // Join all the neighbouring regions through the canonical map
          val smallest = neighbours.min
          var toAlter: Set[Int] = Set.empty
          for ( (x, y) <-canonical.elements ) 
            if (neighbours contains y ) 
              toAlter += x
          toAlter.foreach( x => canonical += (x -> smallest ) )
          neighbours.foreach( x => canonical += (x -> smallest ) )
          wallGrid(row)(column) = smallest
        }
      }
    }
    // debugPrint(wallGrid, canonical)   
    val pointGroups = calculatePointGroups(wallGrid, canonical)
    val regions = makeNonBoundaryRegions(pointGroups)
    regions
  }
  
  /**
   * Scans the west, northwest, north and northeast neighbours for the canonical regions adjacent to the given location.
   */
  private def lookupNeighbours(row: Int, column: Int, wallGrid: Grid[Int], canonical: Map[Int, Int]): Set[Int] = {
    var neighbours: Set[Int] = Set.empty
    if (row > 0) {
      if (column > 0) {
        val northwest = wallGrid(row - 1)(column - 1)
        if (northwest > 0) neighbours += canonical(northwest) 
      }
      val north = wallGrid(row - 1)(column)
      if (north > 0) neighbours += canonical(north)
      if (column < columns - 1) {
        val northeast = wallGrid(row - 1)(column + 1)
        if (northeast > 0) neighbours += canonical(northeast)
      }
    }
    if (column > 0) {
      val west = wallGrid(row)(column - 1)
      if (west > 0) neighbours += canonical(west)
    }
    neighbours
  }
  
  private def makeNonBoundaryRegions(pointGroups: Map[Int, Set[Point]]): Set[Region] = {
    def isOnBoundary(point: Point) = point.row == 0 || point.row == rows - 1 || point.column == 0 || point.column == columns - 1
    set(pointGroups.values.toList.filter( pointGroup => not( pointGroup exists isOnBoundary  ) ).map(IrregularRegion))
  }
  
  private def calculatePointGroups(wallGrid: Grid[Int], canonical: Map[Int, Int]) = {
    var pointGroups: Map[Int, Set[Point]] = Map()
    for { row <- 0 until rows
          column <- 0 until columns
          groupNum = wallGrid(row)(column)
          if groupNum > 0
          canonicalNum = canonical(groupNum)
    } {
      if (!(pointGroups contains canonicalNum) )
        pointGroups += (canonicalNum -> Set() )
      val updatedPointGroup = pointGroups(canonicalNum) + Point(row, column) 
      pointGroups += (canonicalNum -> updatedPointGroup)
    }
    pointGroups
  }
 
  private def debugPrint(wallGrid: Grid[Int], canonical: Map[Int, Int]) {
    println("Result grid:" )
    for (row <- 0 until rows) {
      for (column <- 0 until columns) {
        val id = wallGrid(row)(column)
        if (id < 0)
          print("#")
        else 
            print(wallGrid(row)(column))
      }
      println()
    }
    println()
    println(canonical)
  } 
  
}
