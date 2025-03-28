package pink.iika.pong.logic.server

import java.nio.ByteBuffer

class GameLogic(private var handler: ClientHandler) {
    private var clients = mutableListOf<ClientInfo>()
    private val shapes = listOf(Shape(100.0, 100.0), Shape(500.0, 500.0))
    fun startMovement(move: String, player: Int) {
        when (move) {
            "UP" -> shapes[player-1].setYVelocity(-200.0)
            "DOWN" -> shapes[player-1].setYVelocity(200.0)
            "LEFT" -> shapes[player-1].setXVelocity(-200.0)
            "RIGHT" -> shapes[player-1].setXVelocity(200.0)
        }
    }

    fun endMovement(move: String, player: Int) {
        when (move) {
            "UP" -> shapes[player-1].setYVelocity(0.0)
            "DOWN" -> shapes[player-1].setYVelocity(0.0)
            "LEFT" -> shapes[player-1].setXVelocity(0.0)
            "RIGHT" -> shapes[player-1].setXVelocity(0.0)
        }
    }

    fun tick(dt: Double) {
        for (shape in shapes) shape.move(dt)

        val buffer = ByteBuffer.allocate(33).apply {
            put(ServerPacket.STATE.ordinal.toByte())
            putDouble(shapes[0].x)
            putDouble(shapes[0].y)
            putDouble(shapes[1].x)
            putDouble(shapes[1].y)
        }
        handler.broadcast(buffer.array(), clients)
        println(shapes)
    }

    fun setClients(c: MutableList<ClientInfo>) {
        clients = c
    }
}