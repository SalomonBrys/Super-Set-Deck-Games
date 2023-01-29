package data

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

typealias ValueToCount = Map<String, Int>
typealias SuitToValues = Map<String, ValueToCount>
typealias PlayerToCards = Map<String, Game.P2C>
typealias GameToPlayers = Map<String, PlayerToCards>

@Serializable
data class Game(
    val id: String,
    val names: Map<String, String>,
    val types: List<String>,
    val playerCount: List<Int>,
    val cards: GameToPlayers,
    val references: List<String>
) {
    @Serializable
    data class P2C(val players: List<Int>, val cards: SuitToValues)
}

fun Game.name(lang: Lang) = names[lang.id] ?: names["en"] ?: names.values.first()
val Game.name @Composable get() = name(LocalLang.current)
