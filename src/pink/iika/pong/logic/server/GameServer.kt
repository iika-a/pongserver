package pink.iika.pong.logic.server

import pink.iika.pong.util.gameenum.ClientPacketType
import pink.iika.pong.util.gameenum.ServerPacketType
import java.net.DatagramPacket
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import kotlinx.serialization.json.Json

class GameServer(private val handler: ClientHandler) {
    private val rooms = mutableListOf<GameRoom>()
    private val timer = Timer()  // Used for scheduling retries

    fun startServer() {
        handler.startReceiver { packet ->
            val clientInfo = ClientInfo(packet.address, packet.port)
            handlePacket(packet, clientInfo)
        }
    }

    private fun handlePacket(packet: DatagramPacket, clientInfo: ClientInfo) {
        val ordinal = ByteBuffer.wrap(packet.data, 0, packet.length).get().toInt()
        val type = ClientPacketType.entries[ordinal]

        when (type) {
            ClientPacketType.JOIN_LOBBY -> {
                val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val room = Json.decodeFromString<GameRoom>(dataStr)
                if (clientInfo !in room.clients && room in rooms) {
                    room.clients.add(clientInfo)
                    sendWithRetry(clientInfo, 3, room, ServerPacketType.JOIN_ACCEPTED, ClientPacketType.JOIN_ACCEPTED_ACK)
                }
                else if (clientInfo in room.clients || room.clients.size == 2 || room !in rooms) handler.broadcast(
                    byteArrayOf(ServerPacketType.JOIN_DENIED.ordinal.toByte()),
                    mutableListOf(clientInfo)
                )
            }
            ClientPacketType.JOIN_ACCEPTED_ACK -> {
                for (room in rooms) {
                    if (clientInfo in room.clients) {
                        room.acknowledge(clientInfo, ClientPacketType.JOIN_ACCEPTED_ACK)
                    }
                    if (room.hasAck(room.clients.getOrNull(0) ?: continue, ClientPacketType.JOIN_ACCEPTED_ACK) &&
                        room.hasAck(room.clients.getOrNull(1) ?: continue, ClientPacketType.JOIN_ACCEPTED_ACK)) {
                        handler.broadcast(
                            byteArrayOf(ServerPacketType.ENABLE_START.ordinal.toByte()),
                            mutableListOf(room.clients[0])
                        )
                        room.getLogic().setClients(room.clients)
                    }
                }
            }

            ClientPacketType.EXIT_LOBBY -> {
                val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val room = Json.decodeFromString<GameRoom>(dataStr)
                if (room in rooms && clientInfo in room.clients) room.clients.remove(clientInfo)
                if (room.clients.size == 0) rooms.remove(room)
            }
            ClientPacketType.PADDLE_LEFT_START -> for (room in rooms) if (clientInfo in room.clients) room.getLogic().startMovement("LEFT", room.clients.indexOf(clientInfo))
            ClientPacketType.PADDLE_RIGHT_START -> for (room in rooms) if (clientInfo in room.clients) room.getLogic().startMovement("RIGHT", room.clients.indexOf(clientInfo))
            ClientPacketType.PADDLE_LEFT_END -> for (room in rooms) if (clientInfo in room.clients) room.getLogic().endMovement("LEFT", room.clients.indexOf(clientInfo))
            ClientPacketType.PADDLE_RIGHT_END -> for (room in rooms) if (clientInfo in room.clients) room.getLogic().endMovement("RIGHT", room.clients.indexOf(clientInfo))
            ClientPacketType.START_GAME -> for (room in rooms) if (clientInfo in room.clients) room.getLoop().start()
            ClientPacketType.STOP_GAME_ACK -> for (room in rooms) if (clientInfo in room.clients) room.getLoop().stop()
            ClientPacketType.GET_ROOMS -> {
                val jsonString = Json.encodeToString(rooms)
                val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
                val header = byteArrayOf(ServerPacketType.ROOMS.ordinal.toByte())
                val buffer = header + jsonBytes
                handler.broadcast(buffer, mutableListOf(clientInfo))
            }
            ClientPacketType.CREATE_ROOM -> {
                val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val room = Json.decodeFromString<GameRoom>(dataStr)
                rooms.add(room)
            }
        }
    }

    private fun sendWithRetry(
        client: ClientInfo,
        attempts: Int,
        room: GameRoom,
        packetToSend: ServerPacketType,
        expectedAck: ClientPacketType
    ) {
        if (attempts <= 0) {
            println("No more attempts left for $client on packet ${packetToSend.name}")
            return
        }

        handler.broadcast(byteArrayOf(packetToSend.ordinal.toByte()), mutableListOf(client))
        println("Sent ${packetToSend.name} to $client. Attempts left: $attempts")

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!room.hasAck(client, expectedAck)) {
                    println("No ${expectedAck.name} received from $client, resending ${packetToSend.name}. Attempts left: ${attempts - 1}")
                    sendWithRetry(client, attempts - 1, room, packetToSend, expectedAck)
                } else {
                    println("${expectedAck.name} received from $client, no need to resend ${packetToSend.name}.")
                }
            }
        }, 2000)
    }
}
