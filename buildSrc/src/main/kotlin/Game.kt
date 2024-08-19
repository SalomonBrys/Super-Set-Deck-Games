
typealias ValueToCount = Map<String, Int>
typealias SuitToValues = Map<String, ValueToCount>
typealias PlayerToCards = Map<String, Game.P2C>
typealias GameToPlayers = Map<String, PlayerToCards>

data class Game(
    val id: String,
    val names: Map<String, String>,
    val designers: Designers,
    val types: List<String>,
    val playerCount: List<Int>,
    val cards: GameToPlayers,
    val references: List<String>,
    val thumbnailExt: String,
    val imageExt: String,
    val bgg: BggItem,
) {
    data class Designers(
        val authors: List<String>?,
        val adaptedBy: List<String>?,
        val tradition: String?
    )
    data class P2C(val players: List<Int>, val cards: SuitToValues)

    data class BggItem(
        val bggId: Long,
        val names: List<String>,
        val yearPublished: Int,
        val playingTime: Int,
        val minAge: Int,
        val rating: Double,
    )

}
