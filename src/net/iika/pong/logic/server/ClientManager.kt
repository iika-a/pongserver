package net.iika.pong.logic.server

import java.net.InetAddress

class ClientManager {
    private val clients = mutableListOf<ClientInfo>()

    fun addClient(address: InetAddress, port: Int): ClientInfo {
        // Assign player 1 if first join, otherwise player 2.
        val playerNumber = if (clients.isEmpty()) 1 else 2
        val client = ClientInfo(address, port, playerNumber)
        clients.add(client)
        println("Client joined: $address:$port as Player $playerNumber")
        return client
    }

    fun removeClient(address: InetAddress, port: Int) {
        clients.removeIf { it.address == address && it.port == port }
        println("Client exited: $address:$port")
    }

    fun getClient(address: InetAddress, port: Int): ClientInfo? {
        return clients.find { it.address == address && it.port == port }
    }

    fun getAllClients(): List<ClientInfo> = clients.toList()
}
