package pink.iika.pong.logic.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pink.iika.pong.logic.gameobject.Ball
import pink.iika.pong.logic.gameobject.GameObject
import pink.iika.pong.logic.gameobject.Paddle
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class GameRoom(val name: String, var id: Int, val clients: MutableList<ClientInfo>) {
    @Transient
    private lateinit var logic: GameLogic
    @Transient
    private lateinit var loop: LogicLoop

    fun initialize(handler: ClientHandler) {
        logic = GameLogic(CopyOnWriteArrayList<GameObject>().apply {
            add(Ball())
            add(Paddle(side = 1))
            add(Paddle(side = 2))
        }, handler)
        loop = LogicLoop(logic)
    }

    fun getLogic() = logic
    fun getLoop() = loop
}