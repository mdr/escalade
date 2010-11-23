package castle.network

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.Charset
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.logging.LoggingFilter
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.apache.mina.core.service._ 
import castle.server._
import castle.network.protocol.ServerMessages._
import castle.network.protocol.ClientMessages._
import scala.collection.JavaConversions._

object MinaServer {
  val PORT = 9127
}

class MinaServer extends IServerMessageSender {

  private var acceptor = new NioSocketAcceptor() 
  private val codecFilter = new ProtocolCodecFilter(new ObjectSerializationCodecFactory())
  
  acceptor.getFilterChain.addLast("codec", codecFilter)

  private var clientMessageListeners: Set[IClientMessageListener] = Set.empty
  def addClientMessageListener(listener: IClientMessageListener) { clientMessageListeners += listener }
  private class ClientMessageListener extends IClientMessageListener {
    def messageFromClient(message: ClientMessage) {
      clientMessageListeners.foreach(_.messageFromClient(message))
    }
  }  
  acceptor.setHandler( new ServerHandler(new ClientMessageListener) );

  def start() {
    println("Server: Binding to port " + MinaServer.PORT)
    acceptor.bind( new InetSocketAddress(MinaServer.PORT) );
  }
  
  def broadcastServerMessage(message: ServerMessage) {
    println("Server: sending message " + message.toString.take(25))
    for ((_, session) <- acceptor.getManagedSessions) 
      session.write(message)
  }
  
}


import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession

class ServerHandler(clientMessageListener: IClientMessageListener) extends IoHandlerAdapter {
  
  override def messageReceived( session: IoSession, message: Any ) {
    // Thread.sleep(new scala.util.Random().nextInt % 100 + 200)
    println("Server: Message received: " + message.toString.take(25))
    message match {
      case clientMessage: ClientMessage => clientMessageListener.messageFromClient(clientMessage)  
      case somethingElse => error("Unexpected message received from client: " + somethingElse) // TODO: Log properly
    } 
  }

  override def sessionOpened(session: IoSession) {
    println("Server: Session opened")
  }
  
  override def sessionCreated(session: IoSession) {
    println("Server: Session created")
  }  
}

object RunServer extends Application {
  new MinaServer().start()
}

