package castle.gui

import javax.swing.JPanel
import javax.swing.JFrame
import java.awt.{ Point => _, List => _, _ }
import java.awt.image.BufferedImage
import java.io.File
import castle.model._
import java.lang.Math
import java.awt.event._

import ArenaCanvas._
import matt.utils.ImageUtils
import matt.utils.SwingUtils._
import matt.utils.RichGraphics
import matt.utils.CollectionUtils._
import matt.utils.TimeUtils.time

class ArenaCanvas(
  arenaModelContainer: IArenaModelContainer,
  clientController: IClientController,
  currentTimeGetter: ICurrentTimeGetter,
  firstControlledPlayer: Option[Player],
  secondControlledPlayer: Option[Player]) extends JPanel with UniformComponentSize {

  require(firstControlledPlayer != secondControlledPlayer || firstControlledPlayer == None)

  arenaModelContainer.addArenaModelListener(
    new IArenaModelListener {
      def modelChanged(model: IArenaModel, gameEvents: List[GameEvent]) = {
        doOnSwingThread {
          model.getGamePhaseInfo match {
            case buildPhase: WallBuildPhase => {
              var newClientState = clientState
              for (player <- controlledPlayers) {
                val newLocation = buildPhase.adjustToBounds(player, newClientState.buildLocation(player), newClientState.orientation(player))
                newClientState = newClientState.moveBuildCursor(player, newLocation)
              }
              clientState = newClientState
            }
            case buildPhase: CannonBuildPhase => {
              var newClientState = clientState
              for (player <- controlledPlayers) {
                val newLocation = buildPhase.adjustToBounds(player, newClientState.buildLocation(player), newClientState.cannonType(player))
                newClientState = newClientState.moveBuildCursor(player, newLocation)
              }
              clientState = newClientState
            }
            case battlePhase: BattlePhase =>
            case gameOverPhase: GameOverPhase =>
          }
          for (gameEvent <- gameEvents) {
            gameEvent match {
              case NewStage => Sounds.NEW_ROUND.play()
              case GameOver => Sounds.APPLAUSE.play()
            }
          }
          repaint(arenaRectangle)
        } // do on swing thread
      }
    })

  protected def componentSize = {
    var width = getInsets.left + columns * BLOCK_SIZE + getInsets.right
    var height = getInsets.top + rows * BLOCK_SIZE + getInsets.bottom
    new Dimension(width, height)
  }

  case class DebugOptions(showGrid: Boolean, showClip: Boolean, showRegions: Boolean) {
    def toggleShowGrid = DebugOptions(not(showGrid), showClip, showRegions)
    def toggleShowClip = DebugOptions(showGrid, not(showClip), showRegions)
    def toggleShowRegions = DebugOptions(showGrid, showClip, not(showRegions))
  }
  var debugOptions = DebugOptions(false, false, false)

  private val rows = arenaModelContainer.arenaModel.rows
  private val columns = arenaModelContainer.arenaModel.columns

  private val controlledPlayers = set(firstControlledPlayer ++ secondControlledPlayer)

  class BattlePhaseKeyPressHandler(battlePhase: BattlePhase, clientController: IClientController) extends AbstractKeyPressHandler(clientController) {

    override def primary(player: Player) = {
      val targetPoint = clientState.crossHairsPoint(player)
      if (battlePhase.canFire(player, currentTimeGetter.currentTime)) {
        clientController.shoot(player, targetPoint)
        Sounds.CANNON_FIRE.play()
      }
    }

  }

  type KeyCode = Int
  private var depressedKeys: Set[KeyCode] = Set.empty
  addFocusListener(focusLost { depressedKeys = Set.empty })

  addKeyListener(
    new KeyAdapter {
      override def keyPressed(e: KeyEvent) = {
        val keyHandler = phaseKeyPressHandler()
        val keyCode = e.getKeyCode
        depressedKeys += keyCode
        import keyHandler._
        keyCode match {
          case KeyBindings.firstPlayerUp => firstControlledPlayer foreach up
          case KeyBindings.firstPlayerDown => firstControlledPlayer foreach down
          case KeyBindings.firstPlayerLeft => firstControlledPlayer foreach left
          case KeyBindings.firstPlayerRight => firstControlledPlayer foreach right
          case KeyBindings.firstPlayerPrimary => firstControlledPlayer foreach primary
          case KeyBindings.firstPlayerSecondary => firstControlledPlayer foreach secondary

          case KeyBindings.secondPlayerUp => secondControlledPlayer foreach up
          case KeyBindings.secondPlayerDown => secondControlledPlayer foreach down
          case KeyBindings.secondPlayerLeft => secondControlledPlayer foreach left
          case KeyBindings.secondPlayerRight => secondControlledPlayer foreach right
          case KeyBindings.secondPlayerPrimary => secondControlledPlayer foreach primary
          case KeyBindings.secondPlayerSecondary => secondControlledPlayer foreach secondary

          case KeyBindings.skipPhase => skip()
          case KeyEvent.VK_G => if (e.isControlDown) { debugOptions = debugOptions.toggleShowGrid; repaint() }
          case KeyEvent.VK_R => if (e.isControlDown) { debugOptions = debugOptions.toggleShowClip; repaint() }
          case KeyEvent.VK_T => if (e.isControlDown) { debugOptions = debugOptions.toggleShowRegions; repaint() }
          case _ =>
        }
      }
      override def keyReleased(e: KeyEvent) { depressedKeys -= e.getKeyCode }
    })

  private def phaseKeyPressHandler(): IKeyPressHandler = {
    arenaModelContainer.arenaModel.getGamePhaseInfo match {
      case buildPhase: WallBuildPhase => new WallBuildPhaseKeyPressHandler(buildPhase, clientController)
      case buildPhase: CannonBuildPhase => new CannonBuildPhaseKeyPressHandler(buildPhase, clientController)
      case battlePhase: BattlePhase => new BattlePhaseKeyPressHandler(battlePhase, clientController)
      case gameOverPhase: GameOverPhase => new GameOverKeyPressHandler()
    }
  }

  trait IKeyPressHandler {

    def up(player: Player)

    def down(player: Player)

    def left(player: Player)

    def right(player: Player)

    def primary(player: Player)

    def secondary(player: Player)

    def skip()

    def escape()

  }

  abstract class AbstractKeyPressHandler(clientController: IClientController) extends IKeyPressHandler {

    def up(player: Player) = ()

    def down(player: Player) = ()

    def left(player: Player) = ()

    def right(player: Player) = ()

    def primary(player: Player) {}

    def secondary(player: Player) {}

    def skip() = clientController.skipPhase()

    def escape() {}

  }

  class GameOverKeyPressHandler extends IKeyPressHandler {

    def up(player: Player) = ()

    def down(player: Player) = ()

    def left(player: Player) = ()

    def right(player: Player) = ()

    def primary(player: Player) {}

    def secondary(player: Player) {}

    def skip() = {}

    def escape() {}

  }

  abstract class BuildPhaseKeyPressHandler(buildPhase: GamePhase, clientController: IClientController)
    extends AbstractKeyPressHandler(clientController) {

    override def up(player: Player) = attemptToMoveBuildCursor(player, North)

    override def down(player: Player) = attemptToMoveBuildCursor(player, South)

    override def left(player: Player) = attemptToMoveBuildCursor(player, West)

    override def right(player: Player) = attemptToMoveBuildCursor(player, East)

    def attemptToMoveBuildCursor(player: Player, direction: Direction)

  }

  class WallBuildPhaseKeyPressHandler(buildPhase: WallBuildPhase, clientController: IClientController)
    extends BuildPhaseKeyPressHandler(buildPhase, clientController) {

    def attemptToMoveBuildCursor(player: Player, direction: Direction) {
      val newLocation = clientState.buildLocation(player) + direction
      val orientation = clientState.orientation(player)
      if (buildPhase.canPlaceBuildCursor(player, newLocation, orientation)) {
        clientState = clientState.moveBuildCursor(player, direction)
        repaint(arenaRectangle)
      }
    }

    override def primary(player: Player) = {
      val location = clientState.buildLocation(player)
      val orientation = clientState.orientation(player)
      if (clientState.canBuild(buildPhase, player)) {
        Sounds.BUILD.play()
        clientController.doWallBuild(player, location, orientation)
        repaint(arenaRectangle)
      }
    }

    override def secondary(player: Player) = {
      val location = clientState.buildLocation(player)
      val currentOrientation = clientState.orientation(player)
      val (newLocation, newOrientation) = buildPhase.rotateShapeAndAdjust(player, location, currentOrientation)
      clientState = clientState.setBuildOrientation(player, newOrientation).moveBuildCursor(player, newLocation)
      repaint(arenaRectangle)
    }

  }
  class CannonBuildPhaseKeyPressHandler(buildPhase: CannonBuildPhase, clientController: IClientController)
    extends BuildPhaseKeyPressHandler(buildPhase, clientController) {
    def attemptToMoveBuildCursor(player: Player, direction: Direction) {
      val newLocation = clientState.buildLocation(player) + direction
      val cannonType = clientState.cannonType(player)
      if (buildPhase.canPlaceBuildCursor(player, newLocation, cannonType)) {
        clientState = clientState.moveBuildCursor(player, newLocation)
        repaint(arenaRectangle)
      }
    }

    override def primary(player: Player) = {
      val location = clientState.buildLocation(player)
      val cannonType = clientState.cannonType(player)
      if (buildPhase.canBuild(player, location, cannonType)) {
        clientController.doCannonBuild(player, location, cannonType)
        Sounds.BUILD.play()
      }
    }

    override def secondary(player: Player) = {
      val location = clientState.buildLocation(player)
      val cannonType = clientState.cannonType(player)
      val (newLocation, newCannonType) = buildPhase.cycleToNextCannonAndAdjust(player, location, cannonType)
      clientState = clientState.setCannonType(player, newCannonType).moveBuildCursor(player, newLocation)
      repaint(arenaRectangle)
    }

  }

  private case class ClientPlayerInfo(crossHairsLocation: (Int, Int), buildLocation: Point, buildOrientation: Orientation, cannonType: CannonType) {
    def setCrossHairsLocation(newLocation: (Int, Int)) = ClientPlayerInfo(newLocation, buildLocation, buildOrientation, cannonType)
    def setBuildLocation(newBuildLocation: Point) = ClientPlayerInfo(crossHairsLocation, newBuildLocation, buildOrientation, cannonType)
    def moveBuildLocation(direction: Direction) = ClientPlayerInfo(crossHairsLocation, buildLocation + direction, buildOrientation, cannonType)
    def setBuildOrientation(newBuildOrientation: Orientation) = ClientPlayerInfo(crossHairsLocation, buildLocation, newBuildOrientation, cannonType)
    def setCannonType(newCannonType: CannonType) = ClientPlayerInfo(crossHairsLocation, buildLocation, buildOrientation, newCannonType)
  }

  object ExplosionPhase extends Enumeration {
    val NEW = Value
    val MEDIUM = Value
    val OLD = Value
  }

  private class Explosion(val location: Point, startTime: Time) {
    def getPhase(currentTime: Time): Option[ExplosionPhase.Value] = {
      if (currentTime < startTime + Milliseconds(250))
        Some(ExplosionPhase.NEW)
      else if (currentTime < startTime + Milliseconds(500))
        Some(ExplosionPhase.MEDIUM)
      else if (currentTime < startTime + Milliseconds(750))
        Some(ExplosionPhase.OLD)
      else
        None
    }

    def isActive(currentTime: Time) = getPhase(currentTime) != None
  }

  private class ClientState(
    private val playerInfoMap: Map[Player, ClientPlayerInfo],
    val explosions: Set[Explosion]) {
    require(playerInfoMap.keySet == controlledPlayers, "PlayerInfoMap = " + playerInfoMap.keys + ", controlledPlayers = " + controlledPlayers)

    def buildLocation(player: Player) = playerInfoMap(player).buildLocation

    def orientation(player: Player) = playerInfoMap(player).buildOrientation

    def cannonType(player: Player) = playerInfoMap(player).cannonType

    def moveBuildCursor(player: Player, location: Point) = updatePlayer(player, _.setBuildLocation(location))
    def moveBuildCursor(player: Player, direction: Direction) = updatePlayer(player, _.moveBuildLocation(direction))
    def moveCrossHairs(player: Player, newPosition: (Int, Int)) = updatePlayer(player, _.setCrossHairsLocation(newPosition))
    def setBuildOrientation(player: Player, orientation: Orientation) = updatePlayer(player, _.setBuildOrientation(orientation))
    def setCannonType(player: Player, newCannonType: CannonType) = updatePlayer(player, _.setCannonType(newCannonType))

    private def updatePlayer(player: Player, playerInfoUpdater: ClientPlayerInfo => ClientPlayerInfo) = {
      val newPlayerInfoMap = playerInfoMap + (player -> (playerInfoUpdater(playerInfoMap(player))))
      new ClientState(newPlayerInfoMap, explosions)
    }

    def canBuild(buildPhase: WallBuildPhase, player: Player) = buildPhase.canBuild(player, buildLocation(player), orientation(player))
    def buildShapeInSitu(buildPhase: WallBuildPhase, player: Player) = {
      buildPhase.shape(player).orient(orientation(player)).translate(buildLocation(player))
    }
    def crossHairsPoint(player: Player): Point = {
      require(player containedIn controlledPlayers)
      val (x, y) = playerInfoMap(player).crossHairsLocation
      Point(y / BLOCK_SIZE, x / BLOCK_SIZE)
    }

    def crossHairsPosition(player: Player): (Int, Int) = playerInfoMap(player).crossHairsLocation

    def buildPosition(player: Player): Point = buildLocation(player)

    def addExplosion(explosion: Explosion) = new ClientState(playerInfoMap, explosions + explosion)
    def removeExplosions(explosionsToRemove: Set[Explosion]) = new ClientState(playerInfoMap, explosions -- explosionsToRemove)
  }

  private var clientState: ClientState = new ClientState(
    {
      val initialPlayerInfo = ClientPlayerInfo((210, 210), Point(20, 20), Orientation(0), SmallCannonType)
      map(controlledPlayers.map(_ -> initialPlayerInfo))
    }, Set.empty)

  private def crossHairsTopLeft(x: Int, y: Int) = (x - Images.CROSSHAIR.getWidth / 2, y - Images.CROSSHAIR.getHeight / 2)
  private def crossHairsBottomRight(x: Int, y: Int) = (x + Images.CROSSHAIR.getWidth / 2, y + Images.CROSSHAIR.getHeight / 2)

  private case class LastPaintInfo(projectileDrawAreas: Map[Projectile, Rectangle], secondsRemainingOption: Option[Int])

  private var lastPaintInfo: Option[LastPaintInfo] = None

  private val arenaRectangle = new Rectangle(0, 0, columns * BLOCK_SIZE, rows * BLOCK_SIZE)

  override def paintComponent(givenGraphics: Graphics) {
    time("Paint") {
      val currentTime = currentTimeGetter.currentTime
      val model = arenaModelContainer.arenaModel
      // println("Objects: " + model.objects.toList.length)
      val canvasWidth = getWidth - getInsets.right - getInsets.left
      val canvasHeight = getHeight - getInsets.bottom - getInsets.top

      deriveGraphics(givenGraphics) { g =>
        g.translate(getInsets.left, getInsets.top)
        implicit val implicitGraphics = g
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.clipRect(0, 0, canvasWidth, canvasHeight)
        val clip = g.getClipBounds

        time("Paint: background") {
          val rightBorderRegion = clip.intersection(new Rectangle(columns * BLOCK_SIZE, 0, canvasWidth - columns * BLOCK_SIZE, canvasHeight))
          val bottomBorderRegion = clip.intersection(new Rectangle(0, rows * BLOCK_SIZE, canvasWidth, canvasHeight - rows * BLOCK_SIZE))
          g setColor Color.BLACK

          if (g hitClip rightBorderRegion)
            g fillRect rightBorderRegion
          if (g hitClip bottomBorderRegion)
            g fillRect bottomBorderRegion

          g setColor BACKGROUND_COLOUR
          g fillRect arenaRectangle

          for {
            row <- 0 until rows
            column <- 0 until columns
            owner <- model.getOwner(Point(row, column))
          } {
            g setColor PLAYER_COLOURS(owner)
            g.fillRect(column * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE)
          }
        } // time

        if (debugOptions.showGrid)
          drawGrid()

        if (debugOptions.showRegions) { // TODO: Keep this?
          val regions = model.asInstanceOf[ArenaModel].debug_getRegionOwnerInfo.allRegions
          for ((region, index: Int) <- regions.toList.zipWithIndex; regionNum = (index + 1).toString; Point(row, column) <- region) {
            val newFont = g.getFont.deriveFont(16.0f)
            val fontMetrics = g getFontMetrics newFont
            val fontHeight = fontMetrics.getHeight
            val fontRenderContext = fontMetrics.getFontRenderContext
            val stringBounds = newFont.getStringBounds(regionNum, fontRenderContext)
            val (width, height) = (stringBounds.getWidth.intValue, stringBounds.getHeight.intValue)
            g setFont newFont
            val x = column * BLOCK_SIZE + BLOCK_SIZE / 2 - width / 2
            val y = row * BLOCK_SIZE + BLOCK_SIZE / 2 + fontHeight / 4
            g setColor Color.BLACK
            g.drawString(regionNum.toString, x + 2, y + 2)
            g setColor Color.WHITE
            g.drawString(regionNum.toString, x, y)

          }
        }

        time("Paint: arena objects") {
          for (arenaObject <- model.objects)
            drawArenaObject(arenaObject)
        } // time

        for (explosion <- clientState.explosions) {
          explosion.getPhase(currentTime) match {
            case Some(ExplosionPhase.NEW) => drawTileImage(Images.EXPLOSION_1, explosion.location)
            case Some(ExplosionPhase.MEDIUM) => drawTileImage(Images.EXPLOSION_2, explosion.location)
            case Some(ExplosionPhase.OLD) => drawTileImage(Images.EXPLOSION_3, explosion.location)
            case None =>
          }
        }

        var projectileDrawAreas: Map[Projectile, Rectangle] = Map.empty
        time("Paint: projectiles") {
          for (projectile <- model.projectiles; if currentTime <= projectile.hitTime) {
            val (x, y) = getProjectileDrawPoint(projectile, currentTime)
            val drawRectangle = g.drawImageCentered(Images.CANNON_SHOT, x, y)
            projectileDrawAreas = projectileDrawAreas + (projectile -> drawRectangle)
          }
        } // time

        val secondsRemainingOption: Option[Int] = getSecondsRemaining(model, currentTime)
        for (secondsRemaining <- secondsRemainingOption) {
          val newFont = g.getFont.deriveFont(28.0f)
          val fontMetrics = g getFontMetrics newFont
          val fontHeight = fontMetrics.getHeight
          val fontRenderContext = fontMetrics.getFontRenderContext

          g setFont newFont
          val secondsRemainingString = secondsRemaining.toString
          val stringBounds = newFont.getStringBounds(secondsRemainingString, fontRenderContext)
          val (width, height) = (stringBounds.getWidth.intValue, stringBounds.getHeight.intValue)
          val x = 20 // TODO: Constantise
          val y = 25
          g setColor Color.BLACK
          g.drawString(secondsRemainingString, x + 3, y + 3)
          g setColor Color.WHITE
          g.drawString(secondsRemainingString, x, y)
        }

        time("Paint: phase cursor") {
          model.getGamePhaseInfo match { // TODO: Show other players' cursors
            case buildPhase: WallBuildPhase => {
              for (player <- controlledPlayers; if model.isAlive(player)) {
                val buildLocation = clientState.buildLocation(player)
                val orientation = clientState.orientation(player)
                val canBuild = buildPhase.canBuild(player, buildLocation, orientation)
                val image = if (canBuild) PLAYER_CAN_BUILD_IMAGES(player) else Images.CANT_BUILD_IMAGE
                val buildShapeInSitu = clientState.buildShapeInSitu(buildPhase, player)
                for (point <- buildShapeInSitu)
                  drawTileImage(image, point)
              }
            }
            case buildPhase: CannonBuildPhase => {
              for (player <- controlledPlayers; if model.isAlive(player)) {
                val buildLocation = clientState.buildLocation(player)
                val cannonType = clientState.cannonType(player)
                val canBuild = buildPhase.canBuild(player, buildLocation, cannonType)
                drawCannonBuildCursor(buildLocation, canBuild, cannonType, buildPhase.buildPoints(player))
              }
            }
            case battlePhase: BattlePhase => {
              for (player <- controlledPlayers; if model.isAlive(player)) {
                val (x, y) = clientState.crossHairsPosition(player)
                val (imageX, imageY) = crossHairsTopLeft(x, y)
                val image = if (battlePhase.canFire(player, currentTime)) Images.CROSSHAIR else Images.CANNOT_SHOOT_CROSSHAIR
                g.drawImage(image, imageX, imageY)
              }
            }
            case gameOverPhase: GameOverPhase => {
              val winnerOption = gameOverPhase.winner
              val newFont = g.getFont.deriveFont(32.0f)
              val fontMetrics = g getFontMetrics newFont
              val fontHeight = fontMetrics.getHeight
              val fontRenderContext = fontMetrics.getFontRenderContext

              g setFont newFont
              val gameOverMessage = winnerOption match {
                case Some(player) => "Player " + player.number + " has won!"
                case None => "Draw!"
              }
              val stringBounds = newFont.getStringBounds(gameOverMessage, fontRenderContext)
              val (width, height) = (stringBounds.getWidth.intValue, stringBounds.getHeight.intValue)
              val x = arenaRectangle.width / 2 - width / 2 // TODO: Constantise
              val y = arenaRectangle.height / 2
              g setColor Color.BLACK
              g.drawString(gameOverMessage, x + 3, y + 3)
              g setColor Color.WHITE
              g.drawString(gameOverMessage, x, y)

            }
          }
        } // time

        time("Paint: repaint bounds") {
          if (debugOptions.showClip)
            g.drawRepaintBounds()
        } // time

        lastPaintInfo = Some(LastPaintInfo(projectileDrawAreas, secondsRemainingOption))
      }
    } // Time	
  }

  private def drawGrid()(implicit g: Graphics2D) {
    g setColor GRID_COLOUR
    for (row <- 0 to rows)
      g.drawLine(0, row * BLOCK_SIZE, columns * BLOCK_SIZE, row * BLOCK_SIZE)
    for (column <- 0 until columns)
      g.drawLine(column * BLOCK_SIZE, 0, column * BLOCK_SIZE, rows * BLOCK_SIZE)
  }

  private def getSecondsRemaining(model: IArenaModel, currentTime: Time): Option[Int] = {
    val stageEndTimeOption = model.getGamePhaseInfo.stageEndTime
    stageEndTimeOption.map { stageEndTime =>
      Math.max(0, stageEndTime.secondsDifference(currentTime).seconds)
    }
  }

  private def getProjectileDrawPoint(projectile: Projectile, currentTime: Time): (Int, Int) = {
    val Projectile(origin, destination, startTime, hitTime) = projectile
    val timeSoFar = currentTime - startTime
    val totalTime = hitTime - startTime
    require(totalTime.ms > 0)
    val fractionOfJourney: Double = Math.max(0, Math.min(1.0, timeSoFar.ms.doubleValue / totalTime.ms))
    val (originX, originY) = canvasCoordinates(origin)
    val (destinationX, destinationY) = canvasCoordinates(destination)
    val interpolatedX = originX + fractionOfJourney * (destinationX - originX)
    val interpolatedY = originY + fractionOfJourney * (destinationY - originY)
    return (interpolatedX.intValue, interpolatedY.intValue)
  }

  private def drawArenaObject(arenaObject: ArenaObject)(implicit g: Graphics2D) {
    arenaObject match {
      case Wall(location, player) => {
        drawTileImage(PLAYER_WALL_IMAGES(player), location)
      }
      case Castle(location) => {
        drawTileImage(Images.ORDINARY_CASTLE_IMAGE, location)
      }
      case cannon@Cannon(location, player, cannonType, _, _) => { // TODO: Draw correctly pointing cannon
        cannonType match {
          case SmallCannonType => {
            if (cannon.destroyed)
              drawTileImage(Images.DESTROYED_SMALL_CANNON, location)
            else
              drawTileImage(Images.DEFAULT_SMALL_CANNON, location)
          }
          case LargeCannonType => {
            if (cannon.destroyed)
              drawTileImage(Images.DESTROYED_LARGE_CANNON, location)
            else
              drawTileImage(Images.DEFAULT_LARGE_CANNON, location)
          }
        }
      }
    }
  }

  private def drawCannonBuildCursor(cursorLocation: Point, canBuild: Boolean, cannonType: CannonType, buildPoints: Int)(implicit g: Graphics2D) {
    val imageToDraw = cannonType match {
      case SmallCannonType => if (canBuild) Images.DEFAULT_SMALL_CANNON else SMALL_CANNON_CANNOT_BUILD_IMAGE
      case LargeCannonType => if (canBuild) Images.DEFAULT_LARGE_CANNON else LARGE_CANNON_CANNOT_BUILD_IMAGE
    }
    drawTileImage(imageToDraw, cursorLocation)

    val cannonSize = cannonType.size
    val newFont = g.getFont.deriveFont(22.0f)
    val fontMetrics = g getFontMetrics newFont
    val fontHeight = fontMetrics.getHeight
    val fontRenderContext = fontMetrics.getFontRenderContext
    val stringBounds = newFont.getStringBounds(buildPoints.toString, fontRenderContext)
    val (width, height) = (stringBounds.getWidth.intValue, stringBounds.getHeight.intValue)
    g setFont newFont
    val x = cursorLocation.column * BLOCK_SIZE + (cannonSize * BLOCK_SIZE / 2) - width / 2
    val y = cursorLocation.row * BLOCK_SIZE + (cannonSize * BLOCK_SIZE / 2) + fontHeight / 4
    g setColor Color.BLACK
    g.drawString(buildPoints.toString, x + 4, y + 4)
    g setColor Color.WHITE
    g.drawString(buildPoints.toString, x, y)
  }

  private def isControlledByClient(player: Player) = firstControlledPlayer == Some(player) || secondControlledPlayer == Some(player)

  private def drawTileImage(image: BufferedImage, point: Point)(implicit g: Graphics2D) {
    val clip = g.getClipBounds
    val x = point.column * BLOCK_SIZE
    val y = point.row * BLOCK_SIZE
    if (g.hitClip(x, y, image.getWidth, image.getHeight))
      g.drawImage(image, x, y)
  }

  /**
   * Pixel coordinates of the center of the square corresponding to the given point.
   */
  private def canvasCoordinates(point: Point): (Int, Int) = (point.column * BLOCK_SIZE + BLOCK_SIZE / 2, point.row * BLOCK_SIZE + BLOCK_SIZE / 2)

  private def canvasCoordinatesTopLeft(point: Point): (Int, Int) = (point.column * BLOCK_SIZE, point.row * BLOCK_SIZE)

  private class RefreshMonitor extends Runnable {
    def run = {
      var nextTickTime = currentTimeGetter.currentTime + TICK
      var previousTickTimeOption: Option[Time] = None
      while (true) { // TODO: Mechanism for finishing this thread
        doOnSwingThreadAndBlock {
          val currentTime = currentTimeGetter.currentTime
          val model = arenaModelContainer.arenaModel
          def hasOccurredSinceLastTick(time: Time) = currentTime > time and previousTickTimeOption.forall(_ < time)

          for (projectile <- model.projectiles; if hasOccurredSinceLastTick(projectile.hitTime)) {
            Sounds.PROJECTILE_HIT.play()
            clientState = clientState.addExplosion(new Explosion(projectile.destination, projectile.hitTime))
          }

          if (model.getGamePhaseInfo.isInstanceOf[BattlePhase]) {
            for (player <- controlledPlayers) {
              val playerBindings = KeyBindings(player)
              val (x, y) = clientState.crossHairsPosition(player)
              val delta = CROSS_HAIR_MOVE_SPEED * (if (depressedKeys.contains(playerBindings.secondary)) 2 else 1)
              val minX = Images.CROSSHAIR.getWidth / 2
              val minY = Images.CROSSHAIR.getHeight / 2
              val maxX = columns * BLOCK_SIZE - Images.CROSSHAIR.getWidth / 2
              val maxY = rows * BLOCK_SIZE - Images.CROSSHAIR.getHeight / 2
              val newY = Math.max(minX, Math.min(maxY, y + {
                if (depressedKeys.contains(playerBindings.up))
                  -delta
                else if (depressedKeys.contains(playerBindings.down))
                  delta
                else
                  0
              }))
              val newX = Math.max(minY, Math.min(maxX, x + {
                if (depressedKeys.contains(playerBindings.left))
                  -delta
                else if (depressedKeys.contains(playerBindings.right))
                  delta
                else
                  0
              }))
              if ((newX, newY) != (x, y)) {
                clientState = clientState.moveCrossHairs(player, (newX, newY))
                def repaintShortcut {
                  val minX = Math.min(x, newX)
                  val minY = Math.min(y, newY)
                  val (x1, y1) = crossHairsTopLeft(minX, minY)
                  val maxX = Math.max(x, newX)
                  val maxY = Math.max(y, newY)
                  val (x2, y2) = crossHairsBottomRight(maxX, maxY)
                  ArenaCanvas.this.repaint(x1 + getInsets.left, y1 + getInsets.top, x2 - x1, y2 - y1)
                }
                repaintShortcut
              }

            }
          }
          var explosionsToRemove: Set[Explosion] = Set.empty
          for (explosion <- clientState.explosions) {
            val (x, y) = canvasCoordinatesTopLeft(explosion.location)
            ArenaCanvas.this.repaint(x + getInsets.left, y + getInsets.top, BLOCK_SIZE, BLOCK_SIZE)
            if (not(explosion.isActive(currentTime)))
              explosionsToRemove += explosion
          }
          clientState = clientState.removeExplosions(explosionsToRemove)

          for (info <- lastPaintInfo; (projectile, drawRectangle) <- info.projectileDrawAreas) {
            ArenaCanvas.this.repaint(drawRectangle.x + getInsets.left - 2, drawRectangle.y + getInsets.top - 2, drawRectangle.width + 4, drawRectangle.height + 4)
          }
          // Assumption here: paintComponent() method triggered by the scheduled to repaint() will have close enough currentTime for it to
          // have the right clip for the projectile's position
          for (projectile <- model.projectiles; if currentTime < projectile.hitTime) {
            val (x, y) = getProjectileDrawPoint(projectile, currentTime)
            val drawRectangle = RichGraphics.getDrawRegionForDrawImageCentered(Images.CANNON_SHOT, x, y)
            ArenaCanvas.this.repaint(drawRectangle.x + getInsets.left - 2, drawRectangle.y + getInsets.top - 2, drawRectangle.width + 4, drawRectangle.height + 4)
          }
          // Seconds counter: TODO -- more sane coordinates
          val secondsRemainingOption = getSecondsRemaining(model, currentTime)
          if (lastPaintInfo.exists(x => x.secondsRemainingOption != secondsRemainingOption))
            ArenaCanvas.this.repaint(20, 0, 70, 50)

          previousTickTimeOption = Some(currentTime)
        } // end do on swing thread
        var continueSleepLoop = true
        while (continueSleepLoop) {
          val currentTime = currentTimeGetter.currentTime
          if (currentTime < nextTickTime) {
            val difference = nextTickTime.millisecondsDifference(currentTime)
            Thread.sleep(difference.ms)
          } else {
            continueSleepLoop = false
            var lost = -1
            while (nextTickTime <= currentTime) {
              nextTickTime = nextTickTime + TICK
              lost += 1
            }
            if (lost > 0)
              println("Client: skipped " + lost + " frames")
          }
        }
      }
    }
  }
  new Thread(new RefreshMonitor()).start()
  requestFocusInWindow()
}

trait IPlayerBindings {
  type KeyCode = Int
  val up: KeyCode
  val down: KeyCode
  val left: KeyCode
  val right: KeyCode
  val primary: KeyCode
  val secondary: KeyCode
}
object KeyBindings {

  def apply(player: Player) = {
    player match {
      case Player(1) => new IPlayerBindings {
        val up = firstPlayerUp
        val down = firstPlayerDown
        val left = firstPlayerLeft
        val right = firstPlayerRight
        val primary = firstPlayerPrimary
        val secondary = firstPlayerSecondary
      }
      case Player(2) => new IPlayerBindings {
        val up = secondPlayerUp
        val down = secondPlayerDown
        val left = secondPlayerLeft
        val right = secondPlayerRight
        val primary = secondPlayerPrimary
        val secondary = secondPlayerSecondary
      }
    }
  }
  val firstPlayerUp = KeyEvent.VK_UP
  val firstPlayerDown = KeyEvent.VK_DOWN
  val firstPlayerLeft = KeyEvent.VK_LEFT
  val firstPlayerRight = KeyEvent.VK_RIGHT
  val firstPlayerPrimary = KeyEvent.VK_ENTER
  val firstPlayerSecondary = KeyEvent.VK_SHIFT

  val secondPlayerUp = KeyEvent.VK_E
  val secondPlayerDown = KeyEvent.VK_D
  val secondPlayerLeft = KeyEvent.VK_S
  val secondPlayerRight = KeyEvent.VK_F
  val secondPlayerPrimary = KeyEvent.VK_Q
  val secondPlayerSecondary = KeyEvent.VK_A

  val skipPhase = KeyEvent.VK_BACK_SPACE
  val escape = KeyEvent.VK_ESCAPE
  val pause = KeyEvent.VK_P
}

object ArenaCanvas {
  private val BLOCK_SIZE = 20

  private val BACKGROUND_COLOUR = new Color(255, 255, 220)
  private val GRID_COLOUR = new Color(100, 100, 100, 30)
  private val PLAYER_1_COLOUR = new Color(100, 200, 100, 200)
  private val PLAYER_2_COLOUR = new Color(100, 100, 200, 200)
  private val PLAYER_3_COLOUR = new Color(200, 200, 100, 200)

  private val PLAYER_1_WALL_IMAGE = ImageUtils.tint(Images.WALL_IMAGE, PLAYER_1_COLOUR)
  private val PLAYER_2_WALL_IMAGE = ImageUtils.tint(Images.WALL_IMAGE, PLAYER_2_COLOUR)
  private val PLAYER_3_WALL_IMAGE = ImageUtils.tint(Images.WALL_IMAGE, PLAYER_3_COLOUR)

  private val PLAYER_1_CAN_BUILD_IMAGE = ImageUtils.tint(Images.CAN_BUILD_IMAGE, PLAYER_1_COLOUR)
  private val PLAYER_2_CAN_BUILD_IMAGE = ImageUtils.tint(Images.CAN_BUILD_IMAGE, PLAYER_2_COLOUR)
  private val PLAYER_3_CAN_BUILD_IMAGE = ImageUtils.tint(Images.CAN_BUILD_IMAGE, PLAYER_3_COLOUR)

  private val PLAYER_COLOURS = Map(Player(1) -> PLAYER_1_COLOUR, Player(2) -> PLAYER_2_COLOUR, Player(3) -> PLAYER_3_COLOUR)
  private val PLAYER_WALL_IMAGES = Map(Player(1) -> PLAYER_1_WALL_IMAGE, Player(2) -> PLAYER_2_WALL_IMAGE, Player(3) -> PLAYER_3_WALL_IMAGE)
  private val PLAYER_CAN_BUILD_IMAGES = Map(Player(1) -> PLAYER_1_CAN_BUILD_IMAGE, Player(2) -> PLAYER_2_CAN_BUILD_IMAGE, Player(3) -> PLAYER_3_CAN_BUILD_IMAGE)

  private val SMALL_CANNON_CANNOT_BUILD_IMAGE = ImageUtils.tint(Images.DEFAULT_SMALL_CANNON, new Color(200, 100, 100, 200))
  private val LARGE_CANNON_CANNOT_BUILD_IMAGE = ImageUtils.tint(Images.DEFAULT_LARGE_CANNON, new Color(200, 100, 100, 200))

  private val CROSS_HAIR_MOVE_SPEED = 6 // TODO: Constant speed regardless of frame rate
  private val FRAMES_PER_SECOND = 25
  private val TICK = Milliseconds(1000 / FRAMES_PER_SECOND)

}

