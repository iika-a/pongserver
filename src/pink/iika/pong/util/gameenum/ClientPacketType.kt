package pink.iika.pong.util.gameenum

enum class ClientPacketType {
    JOIN_LOBBY,
    JOIN_ACCEPTED_ACK,
    EXIT_LOBBY,
    PADDLE_LEFT_DOWN,
    PADDLE_LEFT_UP,
    PADDLE_RIGHT_DOWN,
    PADDLE_RIGHT_UP,
    START_GAME
}