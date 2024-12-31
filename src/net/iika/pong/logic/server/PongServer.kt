package net.iika.pong.logic.server

import net.iika.pong.util.GameState
import java.net.DatagramPacket
import java.net.DatagramSocket

class PongServer(port: Int, serverLogic: ServerLogic) {
    val socket = DatagramSocket(port)
    val buffer = ByteArray(1024) // Buffer for incoming data
    val clients = mutableMapOf<ClientInfo, ClientHandler>() // Map of clients to handlers

    init {
        println("UDP server running on port $port...")

        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            val clientInfo = ClientInfo(packet.address, packet.port)

            val handler = clients.getOrPut(clientInfo) {
                println("New client connected: ${clientInfo.address}:${clientInfo.port}")
                ClientHandler(clientInfo, socket, serverLogic)
            }

            handler.processPacket(packet.data)


        }
    }
}