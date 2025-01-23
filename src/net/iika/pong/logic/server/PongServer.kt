package net.iika.pong.logic.server

import net.iika.pong.util.BijectiveMap
import net.iika.pong.util.GameState
import java.net.DatagramPacket
import java.net.DatagramSocket

class PongServer(port: Int, serverLogic: ServerLogic) {
    val socket = DatagramSocket(port)
    val buffer = ByteArray(1024) // Buffer for incoming data
    val clients = arrayListOf<BijectiveMap<ClientInfo, ClientHandler>>() // Map of clients to handlers

    init {
        println("UDP server running on port $port...")

        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            val clientInfo = ClientInfo(packet.address, packet.port)

            val handler: ClientHandler = clients.firstOrNull { bijectiveMap ->
                bijectiveMap.containsKey(clientInfo)
            }?.let { bijectiveMap ->
                bijectiveMap.getValue(clientInfo)!!
            } ?: run {
                println("New client connected: ${clientInfo.address}:${clientInfo.port}")
                val newHandler = ClientHandler(clientInfo, socket, serverLogic)
                val newMap = BijectiveMap<ClientInfo, ClientHandler>()
                newMap.put(clientInfo, newHandler)
                clients.add(newMap)
                newHandler
            }

            handler.processPacket(packet)
        }
    }
}