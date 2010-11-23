package castle.network
import java.io._
import java.net._
import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._

import matt.utils.NetworkUtils._

//object RunServer extends Application {
//  new Server().start()
//}
 
object Server {
  val port = 12111
}

class Server {
  private var threads: List[ServerThread] = List()
  
  def start() {
    val serverSocket = new ServerSocket(Server.port)
    println ("Launched server")
    while (true) {
      val clientSocket = serverSocket.accept
      val thread = new ServerThread(clientSocket)
      thread.start()
      threads = thread :: threads
    }
  }
  
}
 
class ServerThread(clientSocket: Socket) extends Thread {
  type PlayerId = String
  
  private var playerIdOption: Option[PlayerId] = None
  
  def getPlayerId = playerIdOption
  
  override def run {
    println ("Acquired client")
    workWithSocketAsServer(clientSocket) { (objectInputStream, objectOutputStream) =>
      def send(x: Any) = objectOutputStream.writeObject(x)
      
      var finish = false
      while (!finish) {
        var obj: Any = null
        try {
          obj = objectInputStream.readObject()
        } catch {
          case ex: EOFException => finish = true 
          case ex => throw ex
        }
        if (!finish) {
          obj match {
            case null => finish = true
            case x => dispatch(x)
          }
        } 
      }
        
    }
  }
  private def dispatch(obj: Any) {
    println(obj)
  }
}