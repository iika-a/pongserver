package net.iika.pong.logic.server

import net.iika.pong.logic.gameobject.*
import net.iika.pong.util.gameenum.CollisionEvent
import net.iika.pong.util.listener.GameCollisionListener
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ServerLogic(private val gameObjectList: CopyOnWriteArrayList<GameObject>, private val powerUpList: CopyOnWriteArrayList<PowerUp>, clients: MutableMap<ClientInfo, ClientHandler>) {
    private val collisionListener: GameCollisionListener = GameCollisionListener()
    private var player1Gain: Int = 0
    private var player2Gain: Int = 0
    private var player1Score: Int = 0
    private var player2Score: Int = 0

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

        when (checkForLoss()) {
            1 -> {
                refreshGains()
                TODO("send losses")
            }

            2 -> {
                refreshGains()
            }
        }

        when (checkForWin()) {
            1 -> {
                TODO("send wins")
            }

            2 -> {
            }

            3 -> {
            }
        }
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

                        if (otherObject == Obstacle(200.0, 200.0, 1920 - 400.0, 1080 - 400.0) && !GameCollisionListener.checkIntersect(gameObject, otherObject)) gameObject.isImmune = false

                        if (gameObject != otherObject && GameCollisionListener.checkIntersect(gameObject, otherObject) && otherObject !is Paddle && !gameObject.isImmune) {
                            val xIntersect = smallerAbsoluteValueWithSign(otherObject.xPosition - gameObject.xPosition - gameObject.width, otherObject.xPosition + otherObject.width - gameObject.xPosition)
                            val yIntersect = smallerAbsoluteValueWithSign(otherObject.yPosition - gameObject.yPosition - gameObject.height, otherObject.yPosition + otherObject.height - gameObject.yPosition)
                            if (abs(xIntersect) < abs(yIntersect)) gameObject.velocityAngle = atan2(gameObject.yVelocity, -1 * gameObject.xVelocity)
                            if (abs(xIntersect) > abs(yIntersect)) gameObject.velocityAngle = atan2(-1 * gameObject.yVelocity, gameObject.xVelocity)

                            when (otherObject) {
                                is Ball -> {
                                    if (abs(xIntersect) < abs(yIntersect)) collisionListener.onCollision(CollisionEvent.BALL_BALL_SIDE, gameObject, otherObject, xIntersect)
                                    else collisionListener.onCollision(CollisionEvent.BALL_BALL_TOP_BOTTOM, gameObject, otherObject, yIntersect)
                                }
                                is Obstacle -> {
                                    if (abs(xIntersect) < abs(yIntersect)) collisionListener.onCollision(CollisionEvent.BALL_OBSTACLE_SIDE, gameObject, otherObject, xIntersect)
                                    else collisionListener.onCollision(CollisionEvent.BALL_OBSTACLE_TOP_BOTTOM, gameObject, otherObject, yIntersect)
                                }
                            }
                        }
                    }
                }

                is Paddle -> {
                    if (gameObject.xPosition <= 0) gameObject.xPosition = 0.0
                    if (gameObject.xPosition + gameObject.width >= 1920) gameObject.xPosition = 1920 - gameObject.width

                    for (powerUp in powerUpList) {
                        if (((powerUp.xPosition in gameObject.xPosition..(gameObject.xPosition + gameObject.width)) || (powerUp.xPosition + powerUp.width in gameObject.xPosition..(gameObject.xPosition + gameObject.width))) && gameObject.side == powerUp.side) {
                            collisionListener.onCollision(CollisionEvent.PADDLE_POWERUP, gameObject, powerUp, 0.0, gameObjectList)
                            powerUpList.remove(powerUp)
                        }
                    }
                }
            }
        }
    }

    private fun checkForLoss(): Int {
        var allBallsOffScreen = true

        for (gameObject in gameObjectList) {
            if (gameObject is Ball) {
                when {
                    gameObject.yPosition + gameObject.width < 0 -> {
                        if (!gameObject.processed) {
                            refreshScore()
                            player1Gain++
                            player1Score++
                        }
                        gameObjectList.remove(gameObject)
                    }

                    gameObject.yPosition > 1080 -> {
                        if (!gameObject.processed) {
                            refreshScore()
                            player2Gain++
                            player2Score++
                        }
                        gameObjectList.remove(gameObject)
                    }
                    else -> allBallsOffScreen = false
                }
            }
        }

        return if (allBallsOffScreen) 1 else 0
    }

    private fun checkForWin(): Int {
            var player1Win = false
            var player2Win = false

            for (gameObject in gameObjectList) {
                if (gameObject is Paddle) {
                    when (gameObject.side) {
                        1 -> if (gameObject.width > 1920 / 2) player2Win = true
                        2 -> if (gameObject.width > 1920 / 2) player1Win = true
                    }
                }
            }

            if (player1Win) return 2
            if (player2Win) return 1

        return 0
    }

    private fun speedUpBall(increment: Double = 0.0) {
        for (ball in gameObjectList.filterIsInstance<Ball>()) {
            ball.ballSpeed += increment
            ball.xVelocity = ball.ballSpeed * cos(ball.velocityAngle)
            ball.yVelocity = ball.ballSpeed * sin(ball.velocityAngle)
        }
    }

    private fun refreshScore() {
        TODO("send refreshed score")
    }

    private fun refreshGains() {
        TODO("send refreshed gains")
    }

    private fun smallerAbsoluteValueWithSign(a: Double, b: Double) = if (abs(a) < abs(b)) a else b
    fun getGameObjectList() = gameObjectList
}