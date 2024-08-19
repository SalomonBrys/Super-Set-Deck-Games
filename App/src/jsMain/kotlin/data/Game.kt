package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import react.*
import web.http.fetch
import kotlin.js.RegExp

typealias ValueToCount = Map<String, Int>
typealias SuitToValues = Map<String, ValueToCount>
typealias PlayerToCards = Map<String, Game.P2C>
typealias GameToPlayers = Map<String, PlayerToCards>

@Serializable
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
    @Serializable
    data class Designers(
        val authors: List<String>? = null,
        val adaptedBy: List<String>? = null,
        val tradition: String? = null
    )

    @Serializable
    data class P2C(val players: List<Int>, val cards: SuitToValues)

    @Serializable
    data class BggItem(
        val bggId: Long,
        val names: List<String>,
        val yearPublished: Int,
        val playingTime: Int,
        val minAge: Int,
        val rating: Double,
    )
}

fun Game.name(langId: String) = names[langId] ?: names["en"] ?: names.values.first()

private val GamesContext = createContext<List<Game>>()

external interface GamesProviderProps : PropsWithChildren {
    var onError: (String) -> Unit
}

val GamesProvider = FC<GamesProviderProps> { props ->
    var games by useState<List<Game>>()
    useEffectOnce {
        try {
            val json = fetch("games/games.json").text()
            games = Json.decodeFromString(json)
        } catch (error: Throwable) {
            props.onError(error.message ?: "Unexpected error")
            games = emptyList()
        }
    }

    GamesContext(games) {
        +props.children
    }
}

fun useGames(): List<Game>? = useContext(GamesContext)

fun useGame(id: String?): Game? {
    val games = useGames()
    return if (id != null && games != null) games[id] else null
}

private fun String.norm() = (asDynamic().normalize("NFKD").replace(RegExp("[\u0300-\u036f]", "g"), "") as String).lowercase()

private fun List<Game>.filterText(text: String): List<Game> {
    val tokens = text.split(Regex("\\s+")).map { it.trim().norm() }
    return filter { game ->
        val values = (
            game.names.values
        +   game.designers.authors.orEmpty()
        +   game.designers.adaptedBy.orEmpty()
        +   (game.designers.tradition?.let { listOf(it) } ?: emptyList())
        +   game.bgg.names
        )
            .map { it.norm() }

        tokens.all { token -> values.any { value -> token in value } }
    }
}

fun List<Game>.filter(
    types: List<String>,
    playerCounts: List<Int>,
    search: String,
) = this
    .let { list -> if (types.isNotEmpty()) list.filter { it.types.containsAll(types) } else list }
    .let { list -> if (playerCounts.isNotEmpty()) list.filter { it.playerCount.containsAll(playerCounts) } else list }
    .let { list -> if (search.isNotBlank()) list.filterText(search) else list }

operator fun List<Game>.get(id: String) = firstOrNull { it.id == id }
