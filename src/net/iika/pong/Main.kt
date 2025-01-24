package net.iika.pong

import net.iika.pong.logic.GameLoop
import net.iika.pong.logic.gameobject.*
import net.iika.pong.logic.server.ClientHandler
import net.iika.pong.logic.server.ClientInfo
import net.iika.pong.logic.server.PongServer
import net.iika.pong.logic.server.ServerLogic
import net.iika.pong.util.BiMap
import net.iika.pong.util.GameState
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
    val clients = BiMap<ClientInfo, ClientHandler>()
    val gameState = GameState(960.0, 100.0, 960.0, 100.0, 960.0, 540.0, 0, 0)

    val serverLogic = ServerLogic(gameObjectList, powerUpList, clients, gameState)
    val gameLoop = GameLoop(serverLogic, powerUpList)
    val server = PongServer(2438, serverLogic, clients, gameState)

    val serverThread = Thread {
        while (true) {
            if (clients.size() == 2) break
        }

        gameLoop.start()
    }

    serverThread.start()
}