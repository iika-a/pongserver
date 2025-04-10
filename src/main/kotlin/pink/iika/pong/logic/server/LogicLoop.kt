package pink.iika.pong.logic.server

@Suppress("unused")
class LogicLoop(private val logic: GameLogic) : Runnable {

    @Volatile
    private var isRunning = false
    private val targetFPS = 120
    private val nsPerFrame = 1_000_000_000.0 / targetFPS
    private var count = 0
    private var loopThread = Thread(this)

    fun start() {
        if (!isRunning) {
            logic.initializeBall()
            logic.initializePaddles()
            reset()
            isRunning = true
            loopThread.start()
            println("loop started")
        }
    }

    fun stop() {
        isRunning = false
        count = 0
    }

    fun pause() {
        isRunning = false
    }

    fun resume() {
        if (!isRunning) {
            reset()
            isRunning = true
            loopThread.start()
        }
    }

    private fun reset() {
        count = 0
        loopThread = Thread(this)
    }

    override fun run() {
        while (isRunning) {
            val startTime = System.nanoTime()
            val elapsedTime = System.nanoTime() - startTime
            logic.advanceGame(1.0 / targetFPS)


            val sleepTime = nsPerFrame - elapsedTime
            if (sleepTime > 0) {
                val endTime = System.nanoTime() + sleepTime
                while (System.nanoTime() < endTime) {
                    // waiting for accurate frame timing
                }
            }
        }
    }
}
