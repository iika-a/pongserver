package pink.iika.pong.logic.server

import java.net.InetAddress

data class ClientInfo(val address: InetAddress, val port: Int)