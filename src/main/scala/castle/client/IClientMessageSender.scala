package castle.client

import castle.network.protocol.ClientMessages.ClientMessage

trait IClientMessageSender {
  
  def sendClientMessage(message: ClientMessage)
  
}
