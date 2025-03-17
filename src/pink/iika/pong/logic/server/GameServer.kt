package pink.iika.pong.logic.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import kotlin.concurrent.thread

import pink.iika.pong.util.gameenum.ClientPacketType
import pink.iika.pong.util.gameenum.ServerPacketType
import pink.iika.pong.util.PongConstants

object GameServer {
    private lateinit var socket: DatagramSocket
    private val clients = mutableListOf<ClientInfo>()

    // Game variables
    private var paddle1X = PongConstants.FIELD_WIDTH / 2 - PongConstants.PADDLE_WIDTH / 2
    private var paddle2X = PongConstants.FIELD_WIDTH / 2 - PongConstants.PADDLE_WIDTH / 2
    private var ballX = PongConstants.FIELD_WIDTH / 2 - PongConstants.BALL_DIAMETER / 2
    private var ballY = PongConstants.FIELD_HEIGHT / 2 - PongConstants.BALL_DIAMETER / 2
    private var ballVelX = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    private var ballVelY = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    private var score1 = 0
    private var score2 = 0

    // Paddle movement flags (updated by client packets)
    @Volatile private var paddle1Left = false
    @Volatile private var paddle1Right = false
    @Volatile private var paddle2Left = false
    @Volatile private var paddle2Right = false

    // Flag that indicates if the game is running.
    @Volatile private var gameRunning = false

    // Starts the UDP server and the necessary threads.
    fun start() {
        socket = DatagramSocket(PongConstants.PORT)
        println("Server started on port ${PongConstants.PORT}")

        // Start the receiving thread.
        thread(start = true) { receiveLoop() }

        // Wait until at least 2 clients have joined and the first player has signaled to start.
        while (true) {
            Thread.sleep(100)
            if (clients.size >= 2 && gameRunning) break
        }

        // Broadcast a start-game packet.
        broadcastStartGame()

        // Run the game loop.
        gameLoop()
    }

    // Loop that continuously receives UDP packets.
    private fun receiveLoop() {
        val buffer = ByteArray(PongConstants.BUFFER_SIZE)
        while (true) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                processPacket(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Process an incoming packet from a client.
    private fun processPacket(packet: DatagramPacket) {
        val data = packet.data
        val bb = ByteBuffer.wrap(data)
        val packetType = bb.int
        when (packetType) {
            ClientPacketType.JOIN_LOBBY.ordinal -> {
                // Add client if not already present.
                if (clients.none { it.address == packet.address && it.port == packet.port }) {
                    val playerNumber = if (clients.isEmpty()) 1 else 2
                    clients.add(ClientInfo(packet.address, packet.port, playerNumber))
                    println("Client joined: ${packet.address}:${packet.port} as Player $playerNumber")
                    // Optionally, acknowledge the join with a new packet type (we use 8 here for JOIN_ACK).
                    broadcastJoinAck(playerNumber)
                }
            }
            ClientPacketType.EXIT_LOBBY.ordinal -> {
                clients.removeIf { it.address == packet.address && it.port == packet.port }
                println("Client exited: ${packet.address}:${packet.port}")
            }
            ClientPacketType.PADDLE_LEFT_DOWN.ordinal -> {
                clients.find { it.address == packet.address && it.port == packet.port }?.let { client ->
                    if (client.playerNumber == 1) paddle1Left = true else paddle2Left = true
                }
            }
            ClientPacketType.PADDLE_LEFT_UP.ordinal -> {
                clients.find { it.address == packet.address && it.port == packet.port }?.let { client ->
                    if (client.playerNumber == 1) paddle1Left = false else paddle2Left = false
                }
            }
            ClientPacketType.PADDLE_RIGHT_DOWN.ordinal -> {
                clients.find { it.address == packet.address && it.port == packet.port }?.let { client ->
                    if (client.playerNumber == 1) paddle1Right = true else paddle2Right = true
                }
            }
            ClientPacketType.PADDLE_RIGHT_UP.ordinal -> {
                clients.find { it.address == packet.address && it.port == packet.port }?.let { client ->
                    if (client.playerNumber == 1) paddle1Right = false else paddle2Right = false
                }
            }
            ClientPacketType.CLIENT_START_GAME.ordinal -> {
                // Only allow the first player to start the game.
                clients.find { it.address == packet.address && it.port == packet.port }?.let { client ->
                    if (client.playerNumber == 1) {
                        gameRunning = true
                        println("Start game command received from Player 1")
                    }
                }
            }
            else -> println("Unknown client packet type: $packetType")
        }
    }

    // Main game loop: updates game logic and broadcasts state.
    private fun gameLoop() {
        val tickInterval = (1000 / PongConstants.TICK_RATE).toLong()
        while (gameRunning) {
            val startTime = System.currentTimeMillis()
            updateGameState(1.0 / PongConstants.TICK_RATE)
            broadcastGameTick()
            // Check win condition.
            if (score1 >= 5 || score2 >= 5) {
                broadcastStopGame()
                gameRunning = false
                println("Game over. Final Score - Player 1: $score1, Player 2: $score2")
                break
            }
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = tickInterval - elapsed
            if (sleepTime > 0) Thread.sleep(sleepTime)
        }
    }

    // Update the game state based on paddle flags and ball physics.
    private fun updateGameState(dt: Double) {
        // Update paddle positions.
        if (paddle1Left) paddle1X -= PongConstants.PADDLE_SPEED * dt
        if (paddle1Right) paddle1X += PongConstants.PADDLE_SPEED * dt
        if (paddle2Left) paddle2X -= PongConstants.PADDLE_SPEED * dt
        if (paddle2Right) paddle2X += PongConstants.PADDLE_SPEED * dt
        paddle1X = paddle1X.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.PADDLE_WIDTH)
        paddle2X = paddle2X.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.PADDLE_WIDTH)

        // Update ball position.
        ballX += ballVelX * dt
        ballY += ballVelY * dt

        // Bounce off left/right walls.
        if (ballX <= 0 || ballX >= PongConstants.FIELD_WIDTH - PongConstants.BALL_DIAMETER) {
            ballVelX = -ballVelX
            ballX = ballX.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.BALL_DIAMETER)
        }

        // Check collision with paddles.
        // For player2 (top paddle)
        if (ballY <= PongConstants.PADDLE2_Y + 10) { // assuming paddle height ~10
            if (ballX + PongConstants.BALL_DIAMETER >= paddle2X && ballX <= paddle2X + PongConstants.PADDLE_WIDTH) {
                ballVelY = -ballVelY
                ballY = PongConstants.PADDLE2_Y + 10.0
            }
        }
        // For player1 (bottom paddle)
        if (ballY + PongConstants.BALL_DIAMETER >= PongConstants.PADDLE1_Y) {
            if (ballX + PongConstants.BALL_DIAMETER >= paddle1X && ballX <= paddle1X + PongConstants.PADDLE_WIDTH) {
                ballVelY = -ballVelY
                ballY = PongConstants.PADDLE1_Y - PongConstants.BALL_DIAMETER
            }
        }

        // Scoring: if the ball goes off the top or bottom.
        if (ballY < 0) {
            score1++
            resetBall()
        }
        if (ballY > PongConstants.FIELD_HEIGHT) {
            score2++
            resetBall()
        }
    }

    // Reset ball to center with a new random direction.
    private fun resetBall() {
        ballX = PongConstants.FIELD_WIDTH / 2 - PongConstants.BALL_DIAMETER / 2
        ballY = PongConstants.FIELD_HEIGHT / 2 - PongConstants.BALL_DIAMETER / 2
        ballVelX = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
        ballVelY = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    }

    // Broadcast a packet to all connected clients.
    private fun broadcast(data: ByteArray) {
        for (client in clients) {
            try {
                val packet = DatagramPacket(data, data.size, client.address, client.port)
                socket.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Broadcast a start-game packet using ServerPacketType.START_GAME.
    private fun broadcastStartGame() {
        val bb = ByteBuffer.allocate(4)
        bb.putInt(ServerPacketType.START_GAME.ordinal)
        broadcast(bb.array())
    }

    // Broadcast a game tick with the current game state.
    private fun broadcastGameTick() {
        // Prepare a packet with:
        // [packet type (GAME_TICK)] [paddle1X, paddle1Width, paddle2X, paddle2Width, ballX, ballY] [score1, score2]
        val bb = ByteBuffer.allocate(4 + 6 * 8 + 2 * 4)
        bb.putInt(ServerPacketType.GAME_TICK.ordinal)
        bb.putDouble(paddle1X)
        bb.putDouble(PongConstants.PADDLE_WIDTH)
        bb.putDouble(paddle2X)
        bb.putDouble(PongConstants.PADDLE_WIDTH)
        bb.putDouble(ballX)
        bb.putDouble(ballY)
        bb.putInt(score1)
        bb.putInt(score2)
        broadcast(bb.array())
    }

    // Broadcast a stop-game packet.
    private fun broadcastStopGame() {
        val bb = ByteBuffer.allocate(4)
        bb.putInt(ServerPacketType.STOP_GAME.ordinal)
        broadcast(bb.array())
    }

    // Broadcast a join acknowledgement packet.
    // We define a new server packet type with an arbitrary integer (8) for JOIN_ACK.
    private fun broadcastJoinAck(playerNumber: Int) {
        val JOIN_ACK = 8
        val bb = ByteBuffer.allocate(4 + 4)
        bb.putInt(JOIN_ACK)
        bb.putInt(playerNumber)
        broadcast(bb.array())
    }
}