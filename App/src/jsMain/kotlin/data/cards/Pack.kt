package data.cards

import data.Game
import ui.utils.toShortStrings

class Pack {

    // Map(Suit -> Map(Value -> Map(Number -> Set(Game))))
    private val packedSuits = HashMap<String, HashMap<String, HashMap<Int, HashSet<Game>>>>()

    fun add(game: Game, playerCounts: Set<Int>, variants: Set<String>) {
        (variants + "Base").forEach { variant ->
            game.cards[variant]!!.values
                .filter { p2c -> playerCounts.any { it in p2c.players } }
                .forEach { p2c ->
                    p2c.cards.forEach { (suit, values) ->
                        val packedValues = packedSuits.getOrPut(suit) { HashMap() }
                        values.forEach { (value, count) ->
                            val packedCards = packedValues.getOrPut(value) { HashMap() }
                            repeat(count) { index ->
                                val packedGames = packedCards.getOrPut(index + 1) { HashSet() }
                                packedGames.add(game)
                            }
                        }
                    }
                }
        }
    }

    val suits: Set<String> get() = packedSuits.keys

    data class ValueGroup(
        val count: Int,
        val values: String
    )

    data class SuitGroup(
        val suit: String,
        val values: List<ValueGroup>
    )

    fun groups(): List<SuitGroup> =
        packedSuits.keys.sortedWith(CardSuitComparator).map { suit ->
            val values = packedSuits[suit]!!
            val valueGroups = ArrayList<ValueGroup>()
            val singles = ArrayList<Int>()
            values.keys.sortedWith(CardValueComparator).forEach { value ->
                val count = values[value]!!.count()
                if (count == 1 && value.toIntOrNull() != null) {
                    singles.add(value.toInt())
                } else {
                    if (singles.isNotEmpty()) {
                        valueGroups += singles.toShortStrings().map { ValueGroup(1, it) }
                        singles.clear()
                    }
                    valueGroups += ValueGroup(count, value)
                }
            }
            if (singles.isNotEmpty()) {
                valueGroups += singles.toShortStrings().map { ValueGroup(1, it) }
            }
            SuitGroup(suit, valueGroups)
        }

    data class PackedCard(
        val suit: String,
        val value: String,
        val number: Int
    )

    fun cards(): Map<PackedCard, Set<Game>> {
        val map = HashMap<PackedCard, Set<Game>>()
        packedSuits.forEach { (suit, values) ->
            values.forEach { (value, cards) ->
                cards.forEach { (number, games) ->
                    map[PackedCard(suit, value, number)] = games
                }
            }
        }
        return map
    }

//    fun count(suit: String, value: String): Int =
//        packedSuits.get(suit)?.get(value) ?: 0
}