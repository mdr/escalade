package castle.server

import castle.network.protocol.ServerMessages.ServerMessage
trait IServerMessageSender {

  def broadcastServerMessage(message: ServerMessage)

}
