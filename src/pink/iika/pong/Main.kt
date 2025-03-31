package pink.iika.pong

import pink.iika.pong.logic.gameobject.Ball
import pink.iika.pong.logic.gameobject.GameObject
import pink.iika.pong.logic.gameobject.Paddle
import pink.iika.pong.logic.server.ClientHandler
import pink.iika.pong.logic.server.GameLogic
import pink.iika.pong.logic.server.GameServer
import pink.iika.pong.logic.server.LogicLoop
import java.util.concurrent.CopyOnWriteArrayList

fun main() {
    val handler = ClientHandler(2438)
    val logic = GameLogic(CopyOnWriteArrayList<GameObject>().apply {
        add(Ball())
        add(Paddle(side = 1))
        add(Paddle(side = 2))
    }, handler)
    val loop = LogicLoop(logic)
    val server = GameServer(logic, loop, handler)
    server.startServer()
}