package castle.gui

import castle.model._
import javax.swing.BorderFactory
import javax.swing.JFrame
import matt.utils.SwingUtils._
import castle.client._
import castle.server._
import matt.utils.ThreadUtils._
import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._
import castle.network._

object ClientMain extends Application {
  val initialArena = ArenaModel.initialModel(30, 40, Time.now())
  val client = new MinaClient("localhost")
  val clientCoordinator = new ClientCoordinator(client, initialArena)
  client.addServerMessageListener(clientCoordinator)
  doInNewThread {
    client.start()
  }
  
  doOnSwingThread {
    val canvas = new ArenaCanvas(clientCoordinator, clientCoordinator, CurrentTimeGetter, Some(Player(1)), Some(Player(2))) 
    canvas setBorder BorderFactory.createEmptyBorder(5, 5, 5, 5)
    val frame = new JFrame
    frame setTitle "Escalade"
    frame setDefaultCloseOperation JFrame.EXIT_ON_CLOSE
    frame add canvas
    frame.pack()
    frame setVisible true
  }
  
  private object CurrentTimeGetter extends ICurrentTimeGetter() { def currentTime() = Time.now() }
  
}
