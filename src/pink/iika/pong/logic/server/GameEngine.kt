package pink.iika.pong.logic.server

import kotlin.math.abs

import pink.iika.pong.util.GameState
import pink.iika.pong.util.PongConstants

class GameEngine {
    // Game state variables
    var paddle1X: Double = PongConstants.FIELD_WIDTH / 2 - PongConstants.PADDLE_WIDTH / 2
    var paddle2X: Double = PongConstants.FIELD_WIDTH / 2 - PongConstants.PADDLE_WIDTH / 2
    var ballX: Double = PongConstants.FIELD_WIDTH / 2 - PongConstants.BALL_DIAMETER / 2
    var ballY: Double = PongConstants.FIELD_HEIGHT / 2 - PongConstants.BALL_DIAMETER / 2
    var ballVelX: Double = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    var ballVelY: Double = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
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
        if (paddle1Left) paddle1X -= PongConstants.PADDLE_SPEED * dt
        if (paddle1Right) paddle1X += PongConstants.PADDLE_SPEED * dt
        if (paddle2Left) paddle2X -= PongConstants.PADDLE_SPEED * dt
        if (paddle2Right) paddle2X += PongConstants.PADDLE_SPEED * dt
        paddle1X = paddle1X.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.PADDLE_WIDTH)
        paddle2X = paddle2X.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.PADDLE_WIDTH)

        // Update ball position
        ballX += ballVelX * dt
        ballY += ballVelY * dt

        // Bounce off left/right walls
        if (ballX <= 0 || ballX >= PongConstants.FIELD_WIDTH - PongConstants.BALL_DIAMETER) {
            ballVelX = -ballVelX
            ballX = ballX.coerceIn(0.0, PongConstants.FIELD_WIDTH - PongConstants.BALL_DIAMETER)
        }

        // Check collision with paddles
        // For player2 (top paddle)
        if (ballY <= PongConstants.PADDLE2_Y + 10) {  // assuming paddle height ~10
            if (ballX + PongConstants.BALL_DIAMETER >= paddle2X && ballX <= paddle2X + PongConstants.PADDLE_WIDTH) {
                ballVelY = abs(ballVelY)
                ballY = PongConstants.PADDLE2_Y + 10.0
            }
        }
        // For player1 (bottom paddle)
        if (ballY + PongConstants.BALL_DIAMETER >= PongConstants.PADDLE1_Y) {
            if (ballX + PongConstants.BALL_DIAMETER >= paddle1X && ballX <= paddle1X + PongConstants.PADDLE_WIDTH) {
                ballVelY = -abs(ballVelY)
                ballY = PongConstants.PADDLE1_Y - PongConstants.BALL_DIAMETER
            }
        }

        // Scoring: if ball goes off the top or bottom.
        if (ballY < 0) {
            score1++
            resetBall()
        }
        if (ballY > PongConstants.FIELD_HEIGHT) {
            score2++
            resetBall()
        }
    }

    fun resetBall() {
        ballX = PongConstants.FIELD_WIDTH / 2 - PongConstants.BALL_DIAMETER / 2
        ballY = PongConstants.FIELD_HEIGHT / 2 - PongConstants.BALL_DIAMETER / 2
        ballVelX = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
        ballVelY = PongConstants.BALL_SPEED * if (Math.random() < 0.5) 1 else -1
    }

    fun getGameState(): GameState {
        return GameState(
            paddle1X = paddle1X,
            paddle1Width = PongConstants.PADDLE_WIDTH,
            paddle2X = paddle2X,
            paddle2Width = PongConstants.PADDLE_WIDTH,
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