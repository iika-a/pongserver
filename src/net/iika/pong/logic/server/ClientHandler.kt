package net.iika.pong.logic.server

import net.iika.pong.util.GameState
import net.iika.pong.util.gameenum.ClientPacketType
import net.iika.pong.util.gameenum.ServerPacketType
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

class ClientHandler(
    private val clientInfo: ClientInfo,
    private val socket: DatagramSocket, // Shared socket for all clients
    private val serverLogic: ServerLogic
) {
    var lastSeen: Long = System.currentTimeMillis() // For timeout tracking

    fun processPacket(data: ByteArray) {
        val packetType = data[0].toInt() // First byte is the packet type
        when (packetType) {
            //ClientPacketType.MOVE_PADDLE_LEFT.ordinal -> movePaddle(-5.0)
            //ClientPacketType.MOVE_PADDLE_RIGHT.ordinal -> movePaddle(5.0)
            else -> println("Unknown packet type from ${clientInfo.address}:${clientInfo.port}")
        }
        lastSeen = System.currentTimeMillis() // Update last seen timestamp
    }

    fun sendPacket(type: ServerPacketType, data: ByteArray) {
        val packetData = byteArrayOf(type.ordinal.toByte()) + data // Serialize the game state
        val packet = DatagramPacket(
            packetData, packetData.size, clientInfo.address, clientInfo.port
        )
        socket.send(packet) // Send to the client
    }

    companion object {
        fun serializeGameState(state: GameState): ByteArray {
            val buffer = ByteBuffer.allocate(64) // Adjust size to fit GameState fields
            buffer.putDouble(state.paddle1X)
            buffer.putDouble(state.paddle1Width)
            buffer.putDouble(state.paddle2X)
            buffer.putDouble(state.paddle2Width)
            buffer.putDouble(state.ballX)
            buffer.putDouble(state.ballY)
            buffer.putInt(state.score1)
            buffer.putInt(state.score2)
            return buffer.array()
        }
    }
}