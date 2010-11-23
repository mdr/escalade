package castle.model

import scala.util.Random
import Option._
import scala.collection.{ mutable, immutable }
import matt.utils.CollectionUtils._
import matt.utils.RandomUtils._
import matt.utils.MaximalElementFinder
import matt.utils.TimeUtils._
import matt.utils.BoneHeadHashCode
import matt.utils.CollectionUtils._

object ArenaModel {

  val SHOT_SPEED = 6.0
  val STAGE_DURATION = Seconds(15)

  def initialModel(rows: Int, columns: Int, startTime: Time) = {
    val playerManager = PlayerManager(Map(
      Player(1) -> PlayerInfo(true, StandardShapes.LINE, 2),
      Player(2) -> PlayerInfo(true, StandardShapes.LINE, 2)))
    //      Player(3) -> PlayerInfo(true, StandardShapes.LINE, 2)

    val (castle1, walls1) = makeHomeFort(Player(1), Point(10, 10))
    val (castle2, walls2) = makeHomeFort(Player(2), Point(20, 20))
    //    val (cas	tle3, walls3) = makeHomeFort(Player(3), Point(7, 24))
    //    val castles = Set(castle1, castle2, castle3, Castle(Point(25, 7)), Castle(Point(17, 9)))
    //    val walls = walls1 ++ walls2 ++ walls3 
    val castles = Set(castle1, castle2, Castle(Point(25, 7)), Castle(Point(17, 9)))
    val walls = walls1 ++ walls2
    val cannons = Set.empty
    val projectiles = Set() //      Projectile(Point(15, 15), Point(7, 7), Time.now(), Time.now() + Seconds(10))

    val firstStageEndTime = startTime + STAGE_DURATION
    new ArenaModel(rows, columns, CannonBuildStage, Some(firstStageEndTime), playerManager, walls, castles, cannons, projectiles, None)
  }

  private def makeHomeFort(player: Player, centre: Point): (Castle, Set[Wall]) = {
    val castle = Castle(centre)
    val topLeft = Point(centre.row - 3, centre.column - 3)
    val bottomRight = Point(centre.row + 4, centre.column + 4)
    val walls: Set[Wall] = wallSquare(player, topLeft, bottomRight)
    (castle, walls)
  }

  private def wallSquare(player: Player, topLeft: Point, bottomRight: Point): Set[Wall] = {
    val horizontalWalls = for {
      column <- topLeft.column to bottomRight.column
      point <- Set(Point(topLeft.row, column), Point(bottomRight.row, column))
    } yield Wall(point, player)
    val verticalWalls = for {
      row <- topLeft.row to bottomRight.row
      point <- Set(Point(row, topLeft.column), Point(row, bottomRight.column))
    } yield Wall(point, player)
    horizontalWalls ++ verticalWalls
  }

}

abstract sealed class GameStage
case object WallBuildStage extends GameStage
case object CannonBuildStage extends GameStage
case object BattleStage extends GameStage
case object GameOverStage extends GameStage

class ArenaModel( // TODO: Make private
  val rows: Int,
  val columns: Int,
  val gameStage: GameStage,
  val stageEndTimeOption: Option[Time],
  val playerManager: PlayerManager, // TODO: Make this private
  val walls: Set[Wall], // TODO: Make this private
  val castles: Set[Castle], // TODO: Make this private
  val cannons: Set[Cannon], // TODO: Make this private
  val projectiles: Set[Projectile],
  private val regionOwnerInfoOption: Option[RegionOwnerInfo]) extends IEditableArenaModel {

  type Grid[X] = Array[Array[X]]

  require(rows >= StandardShapes.largestWidthOrHeight and columns >= StandardShapes.largestWidthOrHeight)
  // require( arenaObjectsAreNonOverlapping ) // Too slow
  // Check stageEndTimeOption is not None() inappropriately, or move to game stage

  private def arenaObjectsAreNonOverlapping: Boolean = {
    val intersectionChecks = for {
      object1 <- objects
      object2 <- objects
      if object1 != object2
    } yield (object1.region intersects object2.region)
    not(or(intersectionChecks))
  }

  private class ArenaView(
    val rows: Int,
    val columns: Int,
    val players: Set[Player],
    private val walls: Set[Wall]) extends RegionOwnerCalculator.IArenaView {
    def wallPoints(player: Player): Set[Point] =
      for (Wall(location, wallPlayer) <- walls; if player == wallPlayer)
        yield location

    def containsWallOwnedByPlayer(point: Point, player: Player) = walls contains Wall(point, player)
  }

  private lazy val regionOwnerInfo: RegionOwnerInfo = getRegionOwnerInfo
  private lazy val ownerGrid: Grid[Option[Player]] = calculateOwnerGrid(regionOwnerInfo)

  // TODO: Debug-only method
  def debug_getRegionOwnerInfo = regionOwnerInfo

  private def getRegionOwnerInfo = {
    regionOwnerInfoOption match {
      case Some(regionOwnerInfo) => regionOwnerInfo
      case None => {
        val arenaView = new ArenaView(rows, columns, playerManager.allPlayers, walls)
        new RegionOwnerCalculator(arenaView).maximalRegions
      }
    }
  }

  def getOwner(point: Point): Option[Player] = ownerGrid(point.row)(point.column)

  private def calculateOwnerGrid(regionOwnerInfo: RegionOwnerInfo): Grid[Option[Player]] = {
    val grid = new Array[Array[Option[Player]]](rows)
    for (row <- 0 until rows) {
      grid(row) = new Array[Option[Player]](columns)
      for (column <- 0 until columns)
        grid(row)(column) = None
    }
    for ((region, player) <- regionOwnerInfo; Point(row, column) <- region)
      grid(row)(column) = Some(player)
    grid
  }

  private def getObjectAt(point: Point): Option[ArenaObject] = iterableToOption(objects.filter(_.region.contains(point)))

  private def isInBounds(point: Point) = point.row >= 0 and point.row < rows and
    point.column >= 0 and point.column < columns

  lazy val objects: Set[ArenaObject] = walls ++ castles ++ cannons

  private val innerBoundary: Region = {
    var boundaryPoints: Set[Point] = Set.empty
    for (row <- 0 until rows)
      boundaryPoints ++= Set(Point(row, 0), Point(row, columns - 1))
    for (column <- 0 until columns)
      boundaryPoints ++= Set(Point(0, column), Point(rows - 1, column))
    IrregularRegion(boundaryPoints)
  }

  private def intersectsAnObject(region: Region) = objects.map(_.region).exists(region.intersects)
  private def intersectsAnEnemyRegion(region: Region, player: Player) = region.exists(point => getOwner(point).isNeither(Some(player), None))

  private abstract trait AbstractPhase {

    val stageEndTime = ArenaModel.this.stageEndTimeOption

  }

  private def buildShapeInSitu(player: Player, location: Point, orientation: Orientation) = {
    require(gameStage == WallBuildStage)
    playerManager.currentBuildShape(player).orient(orientation).translate(location)
  }

  private def neighbours(point: Point) = point.neighbours.filter(isInBounds)

  // That is, either adjacent to a player's wall or is inside a region by the player
  private def isConnectedToOwnProperty(region: Region, player: Player) = {
    val wallPoints = walls.filter(_.player == player).map(_.location)
    region.exists(point => getOwner(point) == Some(player)) or
      region.exists(point => not(point.neighbours.intersect(wallPoints).isEmpty))
  }

  def getGamePhaseInfo: GamePhase = {
    gameStage match {
      case GameOverStage => new GameOverPhase {
        val winner: Option[Player] = iterableToOption(playerManager.allPlayers.filter(isAlive))
      }
      case WallBuildStage => new WallBuildPhase with AbstractPhase {
        def shape(player: Player) = playerManager.currentBuildShape(player)

        def canPlaceBuildCursor(player: Player, location: Point, orientation: Orientation): Boolean = {
          buildShapeInSitu(player, location, orientation).forall(isInBounds)
        }

        def canBuild(player: Player, location: Point, orientation: Orientation): Boolean = {
          val buildRegion = buildShapeInSitu(player, location, orientation)
          canPlaceBuildCursor(player, location, orientation) and
            isAlive(player) and
            not(intersectsAnObject(buildRegion)) and
            not(intersectsAnEnemyRegion(buildRegion, player)) and
            isConnectedToOwnProperty(buildRegion, player)
        }

        def rotateShapeAndAdjust(player: Player, location: Point, orientation: Orientation): (Point, Orientation) = {
          val newOrientation = orientation.next
          val newLocation = adjustToBounds(player, location, newOrientation)
          (newLocation, newOrientation)
        }

        def adjustToBounds(player: Player, location: Point, orientation: Orientation): Point = {
          val buildRegion = buildShapeInSitu(player, location, orientation)
          location + getAdjustToMoveRegionInBounds(buildRegion)
        }
      }
      case CannonBuildStage => new CannonBuildPhase with AbstractPhase {

        def buildPoints(player: Player) = playerManager.buildPoints(player)

        def canPlaceBuildCursor(player: Player, location: Point, cannonType: CannonType): Boolean = {
          cannonType.regionIfBuildAt(location).forall(isInBounds)
        }

        def canBuild(player: Player, location: Point, cannonType: CannonType): Boolean = {
          val buildRegion = cannonType.regionIfBuildAt(location)
          isAlive(player) and
            canPlaceBuildCursor(player, location, cannonType) and
            playerManager.buildPoints(player) >= cannonType.cost and
            not(intersectsAnObject(buildRegion)) and
            not(intersectsAnEnemyRegion(buildRegion, player)) and
            buildRegion.forall(point => getOwner(point) == Some(player)) // .exists would do... 
        }

        def cycleToNextCannonAndAdjust(player: Player, location: Point, cannonType: CannonType): (Point, CannonType) = {
          val newCannonType = advanceCannonType(cannonType)
          val newLocation = adjustToBounds(player, location, newCannonType)
          (newLocation, newCannonType)
        }

        def adjustToBounds(player: Player, location: Point, cannonType: CannonType): Point = {
          location + getAdjustToMoveRegionInBounds(cannonType.regionIfBuildAt(location))
        }

      }

      case BattleStage => new BattlePhase with AbstractPhase {

        def canFire(player: Player, currentTime: Time) = ArenaModel.this.canFire(player, currentTime)

      }
    }
  }

  private def advanceCannonType(cannonType: CannonType) = cannonType match {
    case SmallCannonType => LargeCannonType
    case LargeCannonType => SmallCannonType
  }

  def buildWalls(player: Player, location: Point, orientation: Orientation, nextShape: Shape): ArenaModel = {
    require(gameStage == WallBuildStage)
    require(getGamePhaseInfo.asInstanceOf[WallBuildPhase].canBuild(player, location, orientation))
    val buildShape = buildShapeInSitu(player, location, orientation)
    val newWalls = walls ++ buildShape.map(Wall(_, player))
    val newPlayerManager = playerManager.setCurrentBuildShape(player, nextShape)
    val arenaView = new ArenaView(rows, columns, set(newPlayerManager.allPlayers), newWalls)
    time("Wall removal") {
      val regionOwnerInfo = new RegionOwnerCalculator(arenaView).maximalRegions
      val wallPoints = newWalls.map(_.location)
      val regionPoints = regionOwnerInfo.allRegions.flatMap(x => x)
      val locationsOfWallsToRemove = regionPoints intersect wallPoints
      val newWalls2 = newWalls.filter(wall => not(locationsOfWallsToRemove contains wall.location))
      val newOwnerGrid = calculateOwnerGrid(regionOwnerInfo)
      val newCannons = cannons.map(cannon => {
        newOwnerGrid(cannon.location.row)(cannon.location.column) match {
          case Some(regionOwner) if regionOwner != cannon.player => cannon.setOwner(regionOwner)
          case _ => cannon
        }
      })
      new ArenaModel(rows, columns, gameStage, stageEndTimeOption, newPlayerManager, newWalls2, castles, newCannons, projectiles, Some(regionOwnerInfo))
    }
  }

  private def getAdjustToMoveRegionInBounds(shape: Region, location: Point): Point = {
    getAdjustToMoveRegionInBounds(shape.translate(location))
  }

  private def getAdjustToMoveRegionInBounds(region: Region): Point = {
    val largestRow = region.map(_.row).max
    val smallestRow = region.map(_.row).min
    val rowAdjust = if (smallestRow < 0) -smallestRow else if (largestRow >= rows) -(largestRow - (rows - 1)) else 0
    val largestColumn = region.map(_.column).max
    val smallestColumn = region.map(_.column).min
    val columnAdjust = if (smallestColumn < 0) -smallestColumn else if (largestColumn >= columns) -(largestColumn - (columns - 1)) else 0
    Point(rowAdjust, columnAdjust)
  }

  private def playerDead(player: Player) = not(isAlive(player))

  def endPhase(currentTime: Time): (ArenaModel, List[GameEvent]) = {
    require(gameStage != GameOverStage)
    var newPlayerManager = playerManager
    if (gameStage != BattleStage) {
      for (player <- playerManager.allPlayers)
        if (not(ownsACastle(player)))
          newPlayerManager = newPlayerManager.playerDied(player)
    }
    val alivePlayers = newPlayerManager.allPlayers.filter(newPlayerManager.isAlive)
    val newStage = if (alivePlayers.size < 2)
      GameOverStage
    else
      advance(gameStage)

    for (player <- newPlayerManager.allPlayers) {
      newStage match {
        case CannonBuildStage => {
          newPlayerManager = newPlayerManager.addBuildPoints(player, castleIncomeBuildPoints(player))
        }
        case _ => ()
      }
    }
    val (newStageEndTime, events) =
      if (newStage == GameOverStage)
        (None, List(GameOver))
      else
        (Some(currentTime + ArenaModel.STAGE_DURATION), List(NewStage))

    (new ArenaModel(rows, columns, newStage, newStageEndTime, newPlayerManager, walls, castles, cannons, projectiles, Some(regionOwnerInfo)), events)
  }

  private def advance(gameStage: GameStage): GameStage = {
    gameStage match {
      case WallBuildStage => CannonBuildStage
      case CannonBuildStage => BattleStage
      case BattleStage => WallBuildStage
      case GameOverStage => error("Should not advance from GameOverStage")
    }
  }

  private def castleIncomeBuildPoints(player: Player): Int = {
    val playerRegions = regionOwnerInfo.regionsOwnedBy(player)
    val allPlayerPoints: Set[Point] = playerRegions.flatMap(x => x)
    castles.filter(castle => allPlayerPoints.contains(castle.location)).size
  }

  def isAlive(player: Player): Boolean = playerManager.isAlive(player)

  private def ownsACastle(player: Player): Boolean = regionOwnerInfo.regionsOwnedBy(player).exists(containsACastle(_))

  private def containsACastle(region: Region): Boolean = castles.exists(castle => castle.region intersects region)

  def buildCannon(player: Player, location: Point, cannonType: CannonType): ArenaModel = {
    require(gameStage == CannonBuildStage)
    require(getGamePhaseInfo.asInstanceOf[CannonBuildPhase].canBuild(player, location, cannonType))
    val newCannon = new Cannon(location, player, cannonType, cannonType.hitPoints, None)
    val newCannons = cannons + newCannon
    val newPlayerManager = playerManager.subtractBuildPoints(player, cannonType.cost)
    // TODO: Abridge stage if nobody can build anywhere
    new ArenaModel(rows, columns, gameStage, stageEndTimeOption, newPlayerManager, walls, castles, newCannons, projectiles, Some(regionOwnerInfo))
  }

  def allPlayers: Set[Player] = playerManager.allPlayers

  def update(currentTime: Time): Option[(ArenaModel, List[GameEvent])] = {
    var newModelOption: Option[ArenaModel] = None
    var wallsToRemove: Set[Wall] = Set.empty
    var cannonDamage: List[(Cannon, Int)] = List()
    var projectilesToRemove: Set[Projectile] = Set.empty
    var gameEvents: List[GameEvent] = List()
    for {
      projectile@Projectile(_, destination, _, hitTime) <- projectiles
      if (currentTime >= hitTime)
    } {
      projectilesToRemove = projectilesToRemove + projectile
      for (arenaObject <- getObjectAt(destination)) {
        arenaObject match {
          case wall: Wall => wallsToRemove = wallsToRemove + wall
          case cannon: Cannon if not(cannon.destroyed) => cannonDamage = (cannon -> 1) :: cannonDamage
          case _ =>
        }
      }
    }
    if (not(wallsToRemove.isEmpty) or not(cannonDamage.isEmpty) or not(projectilesToRemove.isEmpty))
      newModelOption = Some(this.removeWalls(wallsToRemove)
        .damageCannons(makeMapFromPairs(cannonDamage, (x: Int, y: Int) => x + y)).removeProjectiles(projectilesToRemove))

    val endStageEarly = gameStage match {
      case CannonBuildStage => playerManager.totalBuildPoints == 0
      case _ => false
    }
    val endStageOnTime = stageEndTimeOption.exists(currentTime >= _)

    if (endStageEarly or endStageOnTime) {
      val (newModel, newEvents) = newModelOption.getOrElse(this).endPhase(currentTime)
      newModelOption = Some(newModel)
      gameEvents = newEvents ++ gameEvents
    }
    if (not(gameEvents.isEmpty))
      newModelOption = Some(newModelOption.getOrElse(this))
    newModelOption.map(newModel => (newModel, gameEvents))

  }

  private def removeWalls(wallsToRemove: Set[Wall]): ArenaModel = {
    new ArenaModel(rows, columns, gameStage, stageEndTimeOption, playerManager, walls -- wallsToRemove, castles, cannons, projectiles, None)
  }
  private def removeProjectiles(projectilesToRemove: Set[Projectile]): ArenaModel = {
    new ArenaModel(rows, columns, gameStage, stageEndTimeOption, playerManager, walls, castles, cannons, projectiles -- projectilesToRemove, Some(regionOwnerInfo))
  }

  private def damageCannons(cannonDamage: Map[Cannon, Int]): ArenaModel = {
    var newCannons = cannons -- cannonDamage.keys
    for ((cannon, damage) <- cannonDamage)
      newCannons = newCannons + cannon.damage(damage)
    new ArenaModel(rows, columns, gameStage, stageEndTimeOption, playerManager, walls, castles, newCannons, projectiles, Some(regionOwnerInfo))
  }

  def shoot(player: Player, target: Point, currentTime: Time): ArenaModel = {
    val availableCannons = fireableCannons(player, currentTime)
    val firingCannon = getArbitraryElement(availableCannons)
    val newCannon = firingCannon.fire(currentTime)
    val newCannons = cannons - firingCannon + newCannon
    // TODO: Compute journey time for constant speed
    val origin = firingCannon.location
    val distance = origin.distanceTo(target)
    val shotDuration = Milliseconds((1000 * distance / ArenaModel.SHOT_SPEED).toInt)
    val r = new scala.util.Random() // TODO: Remove

    val extraProjectiles = firingCannon.cannonType match {
      //      case LargeCannonType => set(for (deltaRow <- -2 to 2; deltaColumn <- -2 to 2; if Math.abs((deltaRow + deltaColumn) % 2) == 1) 
      //                                    yield Projectile(Point(r.nextInt() % rows, r.nextInt % columns), target + Point(deltaRow, deltaColumn), currentTime, currentTime + shotDuration)) 
      //      case LargeCannonType => set(for (deltaRow <- -3 to 3; deltaColumn <- -3 to 3) 
      //                                    yield Projectile(Point(r.nextInt() % rows, r.nextInt % columns), target + Point(deltaRow, deltaColumn), currentTime, currentTime + shotDuration)) 
      /*      case LargeCannonType => set(for (x <- 1 to 50) 
                                    yield Projectile(Point(Math.abs(r.nextInt()) % rows, Math.abs(r.nextInt % columns)), 
                                                     target + Point((Math.abs(r.nextInt()) % 6) - 3, (Math.abs(r.nextInt() % 6) - 3)), 
                                                                    currentTime, currentTime + shotDuration))
                                                                                      */
      /*      case LargeCannonType => set(for (row <- 0 until rows; column <- 0 until columns) 
                                    yield Projectile(Point(Math.abs(r.nextInt()) % rows, Math.abs(r.nextInt % columns)), 
                                                     Point(row, column), 
                                                                    currentTime, currentTime + Seconds(2))) 
*/
      case LargeCannonType => set(for (deltaRow <- 0 to 2; deltaColumn <- 0 to 2)
        yield Projectile(origin + Point(deltaRow, deltaColumn), target, currentTime, currentTime + shotDuration))
      case SmallCannonType => Set.empty + Projectile(origin, target, currentTime, currentTime + shotDuration)
    }
    val newProjectiles = projectiles ++ extraProjectiles
    new ArenaModel(rows, columns, gameStage, stageEndTimeOption, playerManager, walls, castles, newCannons, newProjectiles, Some(regionOwnerInfo))
  }

  private def cannonsOwnedBy(player: Player): Set[Cannon] = cannons.filter(_.player == player)

  private def fireableCannons(player: Player, currentTime: Time) = cannonsOwnedBy(player).filter(cannon => cannon.canFire(currentTime))

  private def canFire(player: Player, currentTime: Time): Boolean = isAlive(player) and not(fireableCannons(player, currentTime).isEmpty)

}
