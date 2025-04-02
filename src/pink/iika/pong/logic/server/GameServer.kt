package pink.iika.pong.logic.server

import pink.iika.pong.util.BiMap
import pink.iika.pong.util.gameenum.ClientPacketType
import pink.iika.pong.util.gameenum.ServerPacketType
import java.net.DatagramPacket
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask

class GameServer(private val logic: GameLogic, private val loop: LogicLoop, private val handler: ClientHandler) {
    private val knownClients = mutableListOf<ClientInfo>()
    private val acknowledgedClients = mutableSetOf<ClientInfo>()
    private val users = BiMap<ClientInfo, Int>()
    private var playerNumber = 1
    private val timer = Timer()  // Used for scheduling retries

    fun startServer() {
        handler.startReceiver { packet ->
            val clientInfo = ClientInfo(packet.address, packet.port)
            println("Received packet from ${packet.address}:${packet.port}")
            println("Known clients: $knownClients")

            if (clientInfo !in knownClients && playerNumber != 3) {
                knownClients.add(clientInfo)
                users.put(clientInfo, playerNumber)
                playerNumber++
            }
            handlePacket(packet, clientInfo)
        }
    }

    private fun handlePacket(packet: DatagramPacket, clientInfo: ClientInfo) {
        val ordinal = ByteBuffer.wrap(packet.data, 0, packet.length).get().toInt()
        val type = ClientPacketType.entries[ordinal]

        println("Handling ${packet.address}:${packet.port} | ClientInfo=$clientInfo | Is in users: ${users.containsKey(clientInfo)}")

        when (type) {
            ClientPacketType.JOIN_LOBBY -> {
                if (clientInfo in knownClients) {
                    // Send JOIN_ACCEPTED and start retry mechanism with 3 attempts.
                    sendJoinAcceptedWithRetry(clientInfo, 3)
                } else {
                    handler.broadcast(
                        byteArrayOf(ServerPacketType.JOIN_DENIED.ordinal.toByte()),
                        mutableListOf(clientInfo)
                    )
                }
            }
            ClientPacketType.JOIN_ACCEPTED_ACK -> {
                acknowledgedClients.add(clientInfo)
                println("Received JOIN_ACK from $clientInfo")
                if (acknowledgedClients.size == 2) {
                    handler.broadcast(byteArrayOf(ServerPacketType.ENABLE_START.ordinal.toByte()), mutableListOf(knownClients[0]))
                    logic.setClients(knownClients)
                }
            }
            ClientPacketType.EXIT_LOBBY -> {}
            ClientPacketType.PADDLE_LEFT_START -> logic.startMovement("LEFT", users.getValue(clientInfo)!!)
            ClientPacketType.PADDLE_RIGHT_START -> logic.startMovement("RIGHT", users.getValue(clientInfo)!!)
            ClientPacketType.PADDLE_LEFT_END -> logic.endMovement("LEFT", users.getValue(clientInfo)!!)
            ClientPacketType.PADDLE_RIGHT_END -> logic.endMovement("RIGHT", users.getValue(clientInfo)!!)
            ClientPacketType.START_GAME -> loop.start()
            ClientPacketType.STOP_GAME_ACK -> loop.stop()
        }
    }

    // Sends JOIN_ACCEPTED and retries if no JOIN_ACK is received.
    private fun sendJoinAcceptedWithRetry(client: ClientInfo, attempts: Int) {
        if (attempts <= 0) {
            println("No more attempts left for $client")
            return
        }
        // Send the JOIN_ACCEPTED message to the client.
        handler.broadcast(byteArrayOf(ServerPacketType.JOIN_ACCEPTED.ordinal.toByte()), mutableListOf(client))
        println("Sent JOIN_ACCEPTED to $client. Attempts left: $attempts")

        // Schedule a retry after 2 seconds if no ACK is received.
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!acknowledgedClients.contains(client)) {
                    println("No JOIN_ACK received from $client, resending JOIN_ACCEPTED. Attempts left: ${attempts - 1}")
                    sendJoinAcceptedWithRetry(client, attempts - 1)
                } else {
                    println("JOIN_ACK received from $client, no need to resend.")
                }
            }
        }, 2000)
    }
}
