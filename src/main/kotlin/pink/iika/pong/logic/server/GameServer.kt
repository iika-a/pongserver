package pink.iika.pong.logic.server

import pink.iika.pong.util.gameenum.ClientPacketType
import pink.iika.pong.util.gameenum.ServerPacketType
import java.net.DatagramPacket
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import kotlinx.serialization.json.Json
import java.net.InetAddress

class GameServer(private val handler: ClientHandler) {
    private val rooms = mutableListOf<GameRoom>()
    private val timer = Timer()  // Used for scheduling retries
    private val lastRoomCreatedAt = mutableMapOf<InetAddress, Long>()
    private val clientToRoom = mutableMapOf<ClientInfo, GameRoom>()
    private val acknowledgedClients = mutableListOf<ClientInfo>()

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
            ClientPacketType.JOIN_ROOM -> {
                val dataStr = String(packet.data, 1, packet.length - 1, Charsets.UTF_8)
                val decodedRoom = Json.decodeFromString<GameRoom>(dataStr)
                val room = rooms.find { it.id == decodedRoom.id } ?: return
                if (clientInfo !in room.clients && room in rooms) {
                    rooms[rooms.indexOf(room)].clients.add(clientInfo)
                    sendWithRetry(clientInfo, 3, room, ServerPacketType.JOIN_ACCEPTED, ClientPacketType.JOIN_ACCEPTED_ACK)
                    println(room)
                }
                else if (clientInfo in room.clients || room.clients.size == 2 || room !in rooms) handler.broadcast(
                    ServerPacketType.JOIN_DENIED,
                    byteArrayOf(),
                    mutableListOf(clientInfo)
                )
            }
            ClientPacketType.JOIN_ACCEPTED_ACK -> {
                if (clientInfo !in acknowledgedClients) acknowledgedClients.add(clientInfo)
                println(acknowledgedClients)
                for (room in rooms) {
                    if (clientInfo in room.clients) {
                        handler.broadcast(
                            ServerPacketType.ROOM_STATE,
                            Json.encodeToString(room).toByteArray(),
                            room.clients
                        )

                        room.getLogic().setClients(room.clients)
                        if (room.clients.size == 2 && room.clients[0] in acknowledgedClients && room.clients[1] in acknowledgedClients)
                            handler.broadcast(
                                ServerPacketType.ENABLE_START,
                                byteArrayOf(),
                                mutableListOf(room.clients[0])
                            )
                    }
                }
            }

            ClientPacketType.EXIT_ROOM -> {
                val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val decodedRoom = Json.decodeFromString<GameRoom>(dataStr)
                val room = rooms.find { it.id == decodedRoom.id } ?: return
                if (room in rooms && clientInfo in room.clients) room.clients.remove(clientInfo)
                if (room.clients.size == 0) rooms.remove(room)
                if (clientInfo in acknowledgedClients) acknowledgedClients.remove(clientInfo)
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
                handler.broadcast(ServerPacketType.ROOMS, jsonBytes, mutableListOf(clientInfo))
            }
            ClientPacketType.CREATE_ROOM -> {
                val ip = clientInfo.address
                val now = System.currentTimeMillis()
                val last = lastRoomCreatedAt[ip] ?: 0L

                if (now - last < 5000) {
                    println("Client $clientInfo at $ip is creating rooms too fast.")
                    handler.broadcast(ServerPacketType.JOIN_DENIED, byteArrayOf(), listOf(clientInfo))
                    return
                }

                val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val room = try {
                    Json.decodeFromString<GameRoom>(dataStr)
                } catch (e: Exception) {
                    println("Invalid room JSON from $clientInfo: ${e.message}")
                    handler.broadcast(ServerPacketType.JOIN_DENIED, byteArrayOf(), listOf(clientInfo))
                    return
                }

                if (room.clients.size > 1) {
                    println("Client $clientInfo tried to create room with too many clients.")
                    handler.broadcast(ServerPacketType.JOIN_DENIED, byteArrayOf(), listOf(clientInfo))
                    return
                }

                room.initialize(handler)
                assignRoomID(room)
                rooms.add(room)
                clientToRoom[clientInfo] = room
                lastRoomCreatedAt[ip] = now

                println("Room created by $clientInfo at $ip")
            }
        }
    }

    private fun assignRoomID(room: GameRoom) {
        if (room.id >= 0) return

        val usedIds = rooms.map { it.id }.toSet()
        var newId = 0
        while (newId in usedIds) newId++
        room.id = newId
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

        handler.broadcast(packetToSend, byteArrayOf(), mutableListOf(client))
        println("Sent ${packetToSend.name} to $client. Attempts left: $attempts")

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (client !in acknowledgedClients) {
                    println("No ${expectedAck.name} received from $client, resending ${packetToSend.name}. Attempts left: ${attempts - 1}")
                    sendWithRetry(client, attempts - 1, room, packetToSend, expectedAck)
                } else {
                    println("${expectedAck.name} received from $client, no need to resend ${packetToSend.name}.")
                }
            }
        }, 2000)
    }
}
