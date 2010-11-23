package castle.gui

import castle.model._
import castle.server._
import castle.network._

object ServerMain extends Application {
  val server = new MinaServer()
  val serverCoordinator = new ServerCoordinator(server)
  val initialArena: IArenaModel = serverCoordinator.getArenaModel
  server addClientMessageListener serverCoordinator
  server.start()
}
