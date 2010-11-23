package castle.network

import org.apache.mina.transport.socket.nio.NioSocketConnector
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory
import org.apache.mina.filter.codec.ProtocolCodecFilter
import java.net.InetSocketAddress
import castle.server._
import castle.client._
import castle.network.protocol.ServerMessages._
import castle.network.protocol.ClientMessages._
import scala.collection.JavaConversions._

class MinaClient(serverLocation: String) extends IClientMessageSender {
  
  private var serverMessageListeners: Set[IServerMessageListener] = Set.empty
  def addServerMessageListener(listener: IServerMessageListener) { serverMessageListeners += listener }
  private class ServerMessageListener extends IServerMessageListener {
    def messageFromServer(message: ServerMessage) {
      serverMessageListeners.foreach(_.messageFromServer(message))
    }
  }  
  
  private val connector = new NioSocketConnector();
  private val codecFilter = new ProtocolCodecFilter(new ObjectSerializationCodecFactory())
  connector.getFilterChain.addLast("codec", codecFilter)
  connector.setHandler(new ClientHandler(new ServerMessageListener()));
  
  def start() {
    println("Client: started")
    val future = connector.connect(new InetSocketAddress(serverLocation, MinaServer.PORT));
    println("Client: awaiting session")
    val session = future.await().getSession
    println("Client: session acquired")
    /**
     * session.write("Message from the client").await();
     * session.close()
     **/
  }
  
  def sendClientMessage(message: ClientMessage) {
    println("Client: sending message " + message)
    for ((_, session) <- connector.getManagedSessions) 
      session.write(message)
  }
  
}


import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession

class ClientHandler(serverMessageListener: IServerMessageListener) extends IoHandlerAdapter {
  
  override def messageReceived( session: IoSession, message: Any ) {
    // 	Thread.sleep(new scala.util.Random().nextInt % 100 + 200)
    println("Client: message received: " + message.toString.take(25))
    message match {
      case serverMessage: ServerMessage => serverMessageListener.messageFromServer(serverMessage)  
      case somethingElse => error("Client: Unexpected message received from server: " + somethingElse) // TODO: Log properly
    }
  }
  
  override def sessionOpened(session: IoSession) {
    println("Client: Session opened")
  }
  
  override def sessionCreated(session: IoSession) {
    println("Client: Session created")
  }
}  


