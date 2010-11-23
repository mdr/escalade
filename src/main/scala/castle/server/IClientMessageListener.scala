package castle.server
import castle.network.protocol.ClientMessages._

case class ClientId(id: Int)

trait IClientMessageListener {
  
  //def messageFromClient(message: ClientMessage, clientId: ClientId)
  def messageFromClient(message: ClientMessage)

}
