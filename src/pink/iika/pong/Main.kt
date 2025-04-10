package pink.iika.pong

import pink.iika.pong.logic.server.ClientHandler
import pink.iika.pong.logic.server.GameServer

fun main() {
    val handler = ClientHandler(2438)
    val server = GameServer(handler)
    server.startServer()
}