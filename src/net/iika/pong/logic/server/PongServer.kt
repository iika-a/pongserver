package net.iika.pong.logic.server

import net.iika.pong.util.BiMap
import net.iika.pong.util.GameState
import java.net.DatagramPacket
import java.net.DatagramSocket

class PongServer(port: Int, serverLogic: ServerLogic, clients: BiMap<ClientInfo, ClientHandler>, gameState: GameState) {
    private val socket = DatagramSocket(port)
    private val buffer = ByteArray(1024)

    init {
        println("UDP server running on port $port...")

        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            val clientInfo = ClientInfo(packet.address, packet.port)

            val handler = clients.getValue(clientInfo) ?: run {
                println("New client connected: ${clientInfo.address}:${clientInfo.port}")
                val newHandler = ClientHandler(clientInfo, socket, serverLogic, gameState)
                clients.put(clientInfo, newHandler)
                newHandler
            }

            handler.processPacket(packet)
        }
    }
}