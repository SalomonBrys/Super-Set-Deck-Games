
typealias ValueToCount = Map<String, Int>
typealias SuitToValues = Map<String, ValueToCount>
typealias PlayerToCards = Map<String, Game.P2C>
typealias GameToPlayers = Map<String, PlayerToCards>

data class Game(
    val id: String,
    val names: Map<String, String>,
    val types: List<String>,
    val playerCount: List<Int>,
    val cards: GameToPlayers,
    val references: List<String>
) {
    data class P2C(val players: List<Int>, val cards: SuitToValues)
}
