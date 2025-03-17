package pink.iika.pong.logic.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

import pink.iika.pong.util.PongConstants

class UdpHandler(val port: Int) {
    private val socket = DatagramSocket(port)

    fun startReceiver(onPacket: (DatagramPacket) -> Unit) {
        thread(start = true) {
            val buffer = ByteArray(PongConstants.BUFFER_SIZE)
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

    fun broadcast(data: ByteArray, clients: List<ClientInfo>) {
        for (client in clients) {
            try {
                val packet = DatagramPacket(data, data.size, client.address, client.port)
                socket.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}