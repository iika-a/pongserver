package net.iika.pong.logic.server

import kotlin.math.abs
import net.iika.pong.util.GameState

const val FIELD_WIDTH = 1280.0
const val FIELD_HEIGHT = 720.0
const val PADDLE_WIDTH = 150.0
const val PADDLE_SPEED = 500.0     // pixels per second
const val BALL_DIAMETER = 20.0
const val BALL_SPEED = 300.0       // pixels per second
const val PADDLE1_Y = FIELD_HEIGHT - 50.0
const val PADDLE2_Y = 50.0

class GameEngine {
    // Game state variables
    var paddle1X: Double = FIELD_WIDTH / 2 - PADDLE_WIDTH / 2
    var paddle2X: Double = FIELD_WIDTH / 2 - PADDLE_WIDTH / 2
    var ballX: Double = FIELD_WIDTH / 2 - BALL_DIAMETER / 2
    var ballY: Double = FIELD_HEIGHT / 2 - BALL_DIAMETER / 2
    var ballVelX: Double = BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    var ballVelY: Double = BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    var score1: Int = 0
    var score2: Int = 0

    // Paddle movement flags (set by incoming client packets)
    @Volatile var paddle1Left = false
    @Volatile var paddle1Right = false
    @Volatile var paddle2Left = false
    @Volatile var paddle2Right = false

    // Update game state based on dt (in seconds)
    fun update(dt: Double) {
        // Update paddle positions
        if (paddle1Left) paddle1X -= PADDLE_SPEED * dt
        if (paddle1Right) paddle1X += PADDLE_SPEED * dt
        if (paddle2Left) paddle2X -= PADDLE_SPEED * dt
        if (paddle2Right) paddle2X += PADDLE_SPEED * dt
        paddle1X = paddle1X.coerceIn(0.0, FIELD_WIDTH - PADDLE_WIDTH)
        paddle2X = paddle2X.coerceIn(0.0, FIELD_WIDTH - PADDLE_WIDTH)

        // Update ball position
        ballX += ballVelX * dt
        ballY += ballVelY * dt

        // Bounce off left/right walls
        if (ballX <= 0 || ballX >= FIELD_WIDTH - BALL_DIAMETER) {
            ballVelX = -ballVelX
            ballX = ballX.coerceIn(0.0, FIELD_WIDTH - BALL_DIAMETER)
        }

        // Check collision with paddles
        // For player2 (top paddle)
        if (ballY <= PADDLE2_Y + 10) {  // assuming paddle height ~10
            if (ballX + BALL_DIAMETER >= paddle2X && ballX <= paddle2X + PADDLE_WIDTH) {
                ballVelY = abs(ballVelY)
                ballY = PADDLE2_Y + 10.0
            }
        }
        // For player1 (bottom paddle)
        if (ballY + BALL_DIAMETER >= PADDLE1_Y) {
            if (ballX + BALL_DIAMETER >= paddle1X && ballX <= paddle1X + PADDLE_WIDTH) {
                ballVelY = -abs(ballVelY)
                ballY = PADDLE1_Y - BALL_DIAMETER
            }
        }

        // Scoring: if ball goes off the top or bottom.
        if (ballY < 0) {
            score1++
            resetBall()
        }
        if (ballY > FIELD_HEIGHT) {
            score2++
            resetBall()
        }
    }

    fun resetBall() {
        ballX = FIELD_WIDTH / 2 - BALL_DIAMETER / 2
        ballY = FIELD_HEIGHT / 2 - BALL_DIAMETER / 2
        ballVelX = BALL_SPEED * if (Math.random() < 0.5) 1 else -1
        ballVelY = BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    }

    fun getGameState(): GameState {
        return GameState(
            paddle1X = paddle1X,
            paddle1Width = PADDLE_WIDTH,
            paddle2X = paddle2X,
            paddle2Width = PADDLE_WIDTH,
            ballX = ballX,
            ballY = ballY,
            score1 = score1,
            score2 = score2
        )
    }

    fun isGameOver(): Boolean {
        return (score1 >= 5 || score2 >= 5)
    }
}