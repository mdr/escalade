package castle.network
import java.io._
import java.net._
import matt.utils.NetworkUtils._

//object RunClient extends Application {
//  
//}

class Client  {
  def start() {
    val socket = new Socket(InetAddress.getLocalHost(), 12111);
    println("Connected to server")
    workWithSocketAsClient(socket) { (objectInputStream, objectOutputStream) =>
      println("Beginning writing")
      objectOutputStream.writeObject("Hello World")
      objectOutputStream.writeObject(Map(1 -> "Wibble"))
      objectOutputStream.flush()
    }
    println("Done")
  }
}
