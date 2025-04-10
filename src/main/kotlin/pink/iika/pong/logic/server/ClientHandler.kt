package pink.iika.pong.logic.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pink.iika.pong.util.gameenum.ServerPacketType
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class ClientHandler(port: Int) {
    @Transient
    private val socket = DatagramSocket(port)

    fun startReceiver(onPacket: (DatagramPacket) -> Unit) {
        thread(start = true) {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    onPacket(packet)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun broadcast(header: ServerPacketType, data: ByteArray, clients: List<ClientInfo>) {
        for (client in clients) {
            try {
                val fullPayload = byteArrayOf(header.ordinal.toByte()) + data
                val packet = DatagramPacket(fullPayload, fullPayload.size, client.address, client.port)
                socket.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}