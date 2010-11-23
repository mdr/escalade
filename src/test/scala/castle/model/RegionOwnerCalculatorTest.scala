package castle.model

import castle.model.RegionOwnerCalculator.IArenaView

import matt.utils.CollectionUtils._
import matt.utils.TimeUtils._
import org.scalatest.FunSuite

object RegionOwnerCalculatorTest2 extends Application {
  (new RegionOwnerCalculatorTest).execute()
} 

class RegionOwnerCalculatorTest extends FunSuite {
  
  test("Player 2 areas nested in a player 1 area") {
    val arena = wallArena("""
    +--------------------------------------+
    |                                      |
    | 1111111111111111111111111111111111   |
    | 1                                1   |
    | 1    22222222222222    222222222 1   |
    | 11   2 222        2    2       2 1   |
    |  1   2 2 2        2    222222222 1   |
    |  1   2 222       22 222          1   |
    | 11   2222222222222  2 2    1111111   |
    | 1                   222    1         |
    | 1111111111111111111111111111         |
    +--------------------------------------+
    """)

    val expectedRegions: Map[Region, Player] = regions("""
    +--------------------------------------+
    |                                      |
    | ##################################   |
    | #11111111111111111111111111111111#   |
    | #11111111111111111111111111111111#   |
    | ##1111111111111111111111111111111#   |
    |  #1111111111111111111111111111111#   |
    |  #1111111111111111111111111111111#   |
    | ##1111111111111111111111111#######   |
    | #11111111111111111111111111#         |
    | ############################         |
    +--------------------------------------+
    """)

    val actualRegions = new RegionOwnerCalculator(arena).maximalRegions
    assert (expectedRegions === actualRegions)
  }

  test("Player 2 areas and a player 1 area nested in a player 1 area") {
    val arena = wallArena(""" 
    +--------------------------------------+
    |                                      |
    | 1111111111111111111111111111111111   |
    | 1                                1   |
    | 1    22222222222222    222222222 1   |
    | 11   2 222        2    2       2 1   |
    |  1   2 2 2        2    222222222 1   |
    |  1   2 222       22 111          1   |
    | 11   2222222222222  1 1    1111111   |
    | 1                   111    1         |
    | 1111111111111111111111111111         |
    +--------------------------------------+
    """)

    val expectedRegions: Map[Region, Player] = regions("""
    +--------------------------------------+
    |                                      |
    | ##################################   |
    | #11111111111111111111111111111111#   |
    | #11111111111111111111111111111111#   |
    | ##1111111111111111111111111111111#   |
    |  #1111111111111111111111111111111#   |
    |  #111111111111111111###1111111111#   |
    | ##111111111111111111# #1111#######   |
    | #1111111111111111111###1111#         |
    | ############################         |
    +--------------------------------------+
    """, """
    +--------------------------------------+
    |                                      |
    | ##################################   |
    | #                                #   |
    | #                                #   |
    | ##                               #   |
    |  #                               #   |
    |  #                  ###          #   |
    | ##                  #1#    #######   |
    | #                   ###    #         |
    | ############################         |
    +--------------------------------------+
    """)

    val actualRegions = new RegionOwnerCalculator(arena).maximalRegions
    assert (expectedRegions === actualRegions)
  }
  
  test("Player owns entire arena") {
    val arena = wallArena(""" 
    +--------------------------------------+
    |11111111111111111111111111111111111111|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |11111111111111111111111111111111111111|
    +--------------------------------------+
    """)

    val expectedRegions: Map[Region, Player] = regions("""
    +--------------------------------------+
    |######################################|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |######################################|
    +--------------------------------------+
    """)

    val actualRegions = new RegionOwnerCalculator(arena).maximalRegions
    assert (expectedRegions === actualRegions)
  }

  test("Algorithm buster") {
    val arena = wallArena("""
    +--------------------------------------+
    |11111111111111111111111111111111111111|
    |1  1         1         1        1    1|
    |1  1         1         1             1|
    |1  1         1        1 1            1|
    |1  1                                 1|
    |1  11111111111111111111111111111111111|
    |1                                    1|
    |1                                    1|
    |1                                    1|
    |11111111111111111111111111111111111111|
    +--------------------------------------+
    """)

    val expectedRegions: Map[Region, Player] = regions("""
    +--------------------------------------+
    |######################################|
    |#  #111111111#111111111#11111111#1111#|
    |#  #111111111#111111111#1111111111111#|
    |#  #111111111#11111111#1#111111111111#|
    |#  #111111111111111111111111111111111#|
    |#  ###################################|
    |#                                    #|
    |#                                    #|
    |#                                    #|
    |######################################|
    +--------------------------------------+
    ""","""
    +--------------------------------------+
    |######################################|
    |#11#         #         #        #    #|
    |#11#         #         #             #|
    |#11#         #        # #            #|
    |#11#                                 #|
    |#11###################################|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |#111111111111111111111111111111111111#|
    |######################################|
    +--------------------------------------+
    """)

    val actualRegions = new RegionOwnerCalculator(arena).maximalRegions
    assert (expectedRegions === actualRegions)
  }
  
  private def wallArena(diagram: String) = new WallArena(diagram)
  private def region(diagram: String): (Region, Player) = RegionParser.parse(diagram)
  private def regions(diagrams: String*) = Map(diagrams.map(region):_*)
                                               
}


class TestDiagram(diagram: String) {
  
  private val lines = {
     val firstPlus = diagram.indexOf("+")
     require (firstPlus > 0)
     val secondPlus = diagram.indexOf("+", firstPlus + 1)
     require (secondPlus > 0)
     val lineLength = secondPlus - firstPlus - 1
     var continue = true
     var lines: List[String] = List()
     var index = secondPlus + 1
     while (continue) {
       val lineStartIndex = diagram.indexOf("|", index)
       if (lineStartIndex == - 1)
         continue = false
       else {
         val lineEndIndex = diagram.indexOf("|", lineStartIndex + 1)
         val line = diagram.slice(lineStartIndex + 1, lineEndIndex)
         require (line.length == lineLength, "line " + line + " of length " + line.length + " not of required length " + lineLength)
         lines = line :: lines 
         index = lineEndIndex + 1
       }
     }
     lines.reverse
   }

  val rows = lines.length
  
  val columns = getArbitraryElement(lines).length

  private val ownerGrid: Array[Array[Option[Player]]] = {
    val grid = new Array[Array[Option[Player]]](rows)
    for (row <- 0 until rows)
      grid(row) = new Array[Option[Player]](columns)
    for (row <- 0 until rows; column <- 0 until columns)
      grid(row)(column) = innerOwner(row, column)
    grid
  }

  private def innerOwner(row: Int, column: Int): Option[Player] = {
    val pointCharacter = lines(row).substring(column, column + 1)
    if (pointCharacter matches "[1-9]")
      Some(Player(Integer.parseInt(pointCharacter)))
    else
      None
  }	

  def owner(row: Int, column: Int) = ownerGrid(row)(column)
  
  val players: Set[Player] = {
     set(for { 
       row <- 0 until rows
       column <- 0 until columns
       player <- owner(row, column)
     } yield player)
   }

  
}

object RegionParser  {
  
  def parse(diagramString: String): (Region, Player) = {
    val diagram: TestDiagram = new TestDiagram(diagramString)
    val region = regionPoints(diagram)
    val owner = getOnlyElement(owners(diagram))
    (region, owner)
  }
  
  private def regionPoints(diagram: TestDiagram): Region = {
    IrregularRegion(set(for { 
      row <- 0 until diagram.rows
      column <- 0 until diagram.columns
      x <- diagram.owner(row, column)
    } yield Point(row, column)))
  }
  
  private def owners(diagram: TestDiagram): Set[Player] = 
    set(for { 
      row <- 0 until diagram.rows
      column <- 0 until diagram.columns
      owner <- diagram.owner(row, column)
    } yield owner)
}

class WallArena(diagramString: String) extends RegionOwnerCalculator.IArenaView {

  private val diagram: TestDiagram = new TestDiagram(diagramString)
  
  val rows = diagram.rows
   
  val columns = diagram.columns
         
  def players = diagram.players
  
  def wallPoints(player: Player): Set[Point] = 
    set(for { 
      row <- 0 until rows
      column <- 0 until columns
      ownerPlayer <- diagram.owner(row, column)
      if (player == ownerPlayer)
    } yield Point(row, column))
  
  def containsWallOwnedByPlayer(point: Point, player: Player): Boolean = 
    diagram.owner(point.row, point.column) match {
      case Some(player2) => player == player2
      case None => false
    }
}
