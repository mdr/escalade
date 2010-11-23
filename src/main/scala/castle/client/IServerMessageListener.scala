package castle.client

import castle.network.protocol.ServerMessages.ServerMessage

trait IServerMessageListener {

  def messageFromServer(message: ServerMessage)
  
}
