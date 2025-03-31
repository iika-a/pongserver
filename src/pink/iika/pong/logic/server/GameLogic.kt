package pink.iika.pong.logic.server

import pink.iika.pong.logic.gameobject.Ball
import pink.iika.pong.logic.gameobject.GameObject
import pink.iika.pong.logic.gameobject.Paddle
import pink.iika.pong.util.gameenum.CollisionEvent
import pink.iika.pong.util.gameenum.ServerPacketType
import pink.iika.pong.util.listener.GameCollisionListener
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*
import kotlin.random.Random

class GameLogic(private val gameObjectList: CopyOnWriteArrayList<GameObject>, private var handler: ClientHandler) {
    private var clients = mutableListOf<ClientInfo>()
    private val collisionListener = GameCollisionListener()

    fun startMovement(move: String, player: Int) {
        for (paddle in gameObjectList.filterIsInstance<Paddle>()) {
            when (move) {
                "LEFT" -> if (paddle.side != player) paddle.leftPress = true
                "RIGHT" -> if (paddle.side != player) paddle.leftPress = true
            }
        }
    }

    fun endMovement(move: String, player: Int) {
        for (paddle in gameObjectList.filterIsInstance<Paddle>()) {
            when (move) {
                "LEFT" -> if (paddle.side != player) paddle.leftPress = false
                "RIGHT" -> if (paddle.side != player) paddle.leftPress = false
            }
        }
    }

    fun advanceGame(dt: Double) {
        for (gameObject in gameObjectList) {
            when (gameObject) {
                is Ball -> {
                    gameObject.move(gameObject.xVelocity * dt, gameObject.yVelocity * dt)
                    speedUpBall(5 * dt)
                }

                is Paddle -> {
                    if (gameObject.leftPress) gameObject.move(-gameObject.paddleSpeed * dt, 0.0)
                    if (gameObject.rightPress) gameObject.move(gameObject.paddleSpeed * dt, 0.0)
                }
            }
        }

        doCollisionLogic()

        val buffer = ByteBuffer.allocate(33).apply {
            put(ServerPacketType.GAME_TICK.ordinal.toByte())
            putDouble(gameObjectList[0].xPosition)
            putDouble(gameObjectList[0].yPosition)
            putDouble(gameObjectList[1].xPosition)
            putDouble(gameObjectList[2].xPosition)
        }
        handler.broadcast(buffer.array(), clients)
    }

    private fun doCollisionLogic() {
        for (gameObject in gameObjectList) {
            when (gameObject) {
                is Ball -> {
                    if (gameObject.xPosition <= 0 || gameObject.xPosition + gameObject.width >= 1920) {
                        val wallIntersect = if (gameObject.xPosition <= 0) 0 - gameObject.xPosition
                        else 1920 - gameObject.xPosition - gameObject.width

                        gameObject.velocityAngle = atan2(gameObject.yVelocity, -1 * gameObject.xVelocity)
                        collisionListener.onCollision(event = CollisionEvent.BALL_WALL, obj1 = gameObject, intersect = wallIntersect)
                    }

                    for (otherObject in gameObjectList) {
                        if (otherObject is Paddle && GameCollisionListener.checkIntersect(gameObject, otherObject)) {
                            gameObject.velocityAngle = atan2(-1 * gameObject.yVelocity, gameObject.xVelocity)

                            when (otherObject.side) {
                                1 -> {
                                    if (gameObject.yPosition + gameObject.height >= otherObject.yPosition) {
                                        val intersect = otherObject.yPosition - gameObject.yPosition - gameObject.height
                                        collisionListener.onCollision(CollisionEvent.BALL_PADDLE, gameObject, otherObject, intersect)
                                    }
                                }
                                2 -> {
                                    if (gameObject.yPosition <= otherObject.yPosition + otherObject.height) {
                                        val intersect = otherObject.yPosition + otherObject.height - gameObject.yPosition
                                        collisionListener.onCollision(CollisionEvent.BALL_PADDLE, gameObject, otherObject, intersect)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun initializeBall() {
        for (gameObject in gameObjectList) {
            if (gameObject is Ball) {
                gameObject.xPosition = ((1920/4)..(2 * 1920/3)).random().toDouble()
                gameObject.yPosition = 1080 / 2.0

                if (gameObject.isTemporary) gameObjectList.remove(gameObject)

                gameObject.processed = false
                gameObject.ballSpeed = (650..725).random().toDouble()
                gameObject.velocityAngle = getRandomAngle(gameObject.initialDirection)
                gameObject.xVelocity = gameObject.ballSpeed * cos(gameObject.velocityAngle)
                gameObject.yVelocity = gameObject.ballSpeed * sin(gameObject.velocityAngle)
            }
        }
    }

    fun initializePaddles() {
        for (paddle in gameObjectList.filterIsInstance<Paddle>()) {
            paddle.width = 150.0
            paddle.paddleSpeed = 800.0
            paddle.xPosition = 1920 / 2 - paddle.width / 2
            if (paddle.side == 1) paddle.xPosition = 1920 / 2 - paddle.width / 2

            if (paddle.side == 1) paddle.yPosition = 1080 - paddle.paddleHeight
            else paddle.yPosition = 0.0
        }
    }

    private fun speedUpBall(increment: Double = 0.0) {
        for (ball in gameObjectList.filterIsInstance<Ball>()) {
            ball.ballSpeed += increment
            ball.xVelocity = ball.ballSpeed * cos(ball.velocityAngle)
            ball.yVelocity = ball.ballSpeed * sin(ball.velocityAngle)
        }
    }

    fun setClients(c: MutableList<ClientInfo>) { clients = c }
    private fun getRandomAngle(direction: Int) =
        if (direction == 0) Random.nextDouble(PI /9, 7 * PI /18)
        else Random.nextDouble(PI /9 + PI, 7 * PI /18 + PI)
}