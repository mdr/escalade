package castle.gui
import castle.client._
import castle.server._

import castle.network.protocol.ClientMessages._
import castle.network.protocol.ServerMessages._

class DirectServerClientMessageSender extends IServerMessageSender with IClientMessageSender {
    
  private var serverMessageListeners: Set[IServerMessageListener] = Set.empty
  private var clientMessageListeners: Set[IClientMessageListener] = Set.empty

  private val clientId = ClientId(1)
  def sendClientMessage(message: ClientMessage) = { // TODO: ALlow only one client, and then fix the ID
    clientMessageListeners.foreach(_.messageFromClient(message))
  } 
  def addClientMessageListener(listener: IClientMessageListener) { clientMessageListeners += listener }
       
  def broadcastServerMessage(message: ServerMessage) = {
    serverMessageListeners.foreach(_.messageFromServer(message))
  }    
  def addServerMessageListener(listener: IServerMessageListener) { serverMessageListeners += listener }
   
  // To debug serialisation without needing to do the network stuff
  private def writeToDiskAndBack[T](obj: T): T = {
    import java.io._
    val tempFile = File.createTempFile("escalade", "tmp")
    tempFile.deleteOnExit()
    val fos = new FileOutputStream(tempFile);
    val oos = new ObjectOutputStream(fos);
    oos.writeObject(obj);
    oos.flush();
    oos.close();
    val fis = new FileInputStream(tempFile);
    val ois = new ObjectInputStream(fis);
    val obj2 = ois.readObject();
    obj2.asInstanceOf[T]
  }
  
}
	
