package castle.gui
import castle.model._
import javax.swing.BorderFactory
import java.awt.Color
import javax.swing.JFrame
import matt.utils.SwingUtils._
import castle.client._
import castle.server._
import matt.utils.ThreadUtils._
import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._
import castle.network._
import java.net._
import java.awt.event.{WindowAdapter, WindowEvent}

object LocalServerClientMain extends Application {
  val server = new MinaServer()
  val serverCoordinator = new ServerCoordinator(server)
  val initialArena: IArenaModel = serverCoordinator.getArenaModel
  server addClientMessageListener serverCoordinator 
  server.start()
  
  val client = new MinaClient(InetAddress.getLocalHost().getHostAddress)
  val clientCoordinator = new ClientCoordinator(client, initialArena)
  client addServerMessageListener clientCoordinator
  doInNewThread {
    client.start()
  }
  
  doOnSwingThread {
    val canvas = new ArenaCanvas(clientCoordinator, clientCoordinator, CurrentTimeGetter, Some(Player(1)), Some(Player(2)))
    val frame = new JFrame
    frame setTitle "Escalade"
    frame setDefaultCloseOperation JFrame.EXIT_ON_CLOSE
    frame add canvas
    def goFullScreen() {
      frame setUndecorated true
      import java.awt.GraphicsEnvironment
      val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
      val graphicsDevice = graphicsEnvironment.getDefaultScreenDevice
      //if (graphicsDevice.isFullScreenSupported) 
      graphicsDevice setFullScreenWindow frame
    }
    frame.addWindowFocusListener(windowGainedFocus { canvas.requestFocusInWindow() } )

    // goFullScreen()
    frame.pack()
    frame.setLocationRelativeTo( null );
    frame setVisible true
  }	
  
  private object CurrentTimeGetter extends ICurrentTimeGetter() { def currentTime() = Time.now() }

}
