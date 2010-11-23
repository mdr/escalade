package castle.model
import matt.utils.TimeUtils._
object RegionOwnerCalculatorTiming  extends Application {

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
    for (i <- 1 to 20) 
      new RegionOwnerCalculator(arena).maximalRegions

    time("A hundred region owner calculations") {
      for (i <- 1 to 100) 
        new RegionOwnerCalculator(arena).maximalRegions
    }
  
  private def wallArena(diagram: String) = new WallArena(diagram)
  private def region(diagram: String): (Region, Player) = RegionParser.parse(diagram)
  private def regions(diagrams: String*) = Map(diagrams.map(region):_*)

}
