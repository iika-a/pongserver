package net.iika.pong

import net.iika.pong.logic.GameLoop
import net.iika.pong.logic.gameobject.*
import net.iika.pong.logic.server.PongServer
import net.iika.pong.logic.server.ServerLogic
import java.util.concurrent.CopyOnWriteArrayList

fun main() {
    val powerUpList = CopyOnWriteArrayList<PowerUp>()
    val gameObjectList = CopyOnWriteArrayList<GameObject>().apply {
        add(Paddle())
        add(Paddle())
        add(Ball())
        add(Obstacle(
            (20..1920 - 200).random().toDouble(),
            (20..1080 - 200).random().toDouble()
        ))
        add(Obstacle(
            (20..1920 - 200).random().toDouble(),
            (20..1080 - 200).random().toDouble()
        ))
        add(Obstacle(
            (20..1920 - 200).random().toDouble(),
            (20..1080 - 200).random().toDouble()
        ))
        add(Obstacle(
            (20..1920 - 200).random().toDouble(),
            (20..1080 - 200).random().toDouble()
        ))
    }
    val serverLogic = ServerLogic(gameObjectList, powerUpList)
    val gameLoop = GameLoop(serverLogic, powerUpList)
    val server = PongServer(2438, serverLogic)

    val serverThread = Thread {
        while (true) {
            if (server.clients.size == 2) break
        }

        gameLoop.start()
    }

    serverThread.start()
}