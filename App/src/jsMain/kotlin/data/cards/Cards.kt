package data.cards

val CardSuits = listOf("stars", "spades", "hearts", "clubs", "diamonds", "florettes", "wheels", "specials")

val CardValues = mapOf(
    "stars" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1, "16" to 1, "17" to 1, "18" to 1, "19" to 1,
        "20" to 1, "21" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "S" to 1, "C" to 1, "A" to 1,
        "-‽" to 1, "×2" to 1,
    ),
    "spades" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1, "16" to 1, "17" to 1, "18" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "S" to 1, "A" to 1,
    ),
    "hearts" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1, "16" to 1, "17" to 1, "18" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "S" to 1, "A" to 1,
    ),
    "clubs" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1, "16" to 1, "17" to 1, "18" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "S" to 1, "A" to 1,
    ),
    "diamonds" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1, "16" to 1, "17" to 1, "18" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "S" to 1, "A" to 1,
    ),
    "florettes" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1, "9" to 1,
        "10" to 1, "11" to 1, "12" to 1, "13" to 1, "14" to 1, "15" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "K" to 3, "A" to 1,
        "►" to 1,
    ),
    "wheels" to setOf(
        "0" to 1, "1" to 1, "2" to 1, "3" to 1, "4" to 1, "5" to 1, "6" to 1, "7" to 1, "8" to 1,
        "J" to 1, "Q" to 1, "K" to 1, "K" to 2, "A" to 1,
    ),
    "specials" to setOf(
        "Butterfly" to 1, "Butterfly" to 2, "Butterfly" to 3, "Butterfly" to 4, "Butterfly" to 5, "Butterfly" to 6,
        "Wolf" to 1, "Wolf" to 2, "Wolf" to 3, "Wolf" to 4, "Wolf" to 5, "Wolf" to 6,
        "Phoenix" to 1, "Phoenix" to 2, "Phoenix" to 3, "Phoenix" to 4,
        "Snake" to 1, "Snake" to 2, "Snake" to 3, "Snake" to 4,
        "Dragon" to 1, "Dragon" to 2,
        "Monkey" to 1, "Monkey" to 2,
        "Ø" to 1,
    )
)

val CardRows = listOf(
    listOf("0" to 1, "Butterfly" to 1),
    listOf("1" to 1, "Butterfly" to 2),
    listOf("2" to 1, "Butterfly" to 3),
    listOf("3" to 1, "Butterfly" to 4),
    listOf("4" to 1, "Butterfly" to 5),
    listOf("5" to 1, "Butterfly" to 6),
    listOf("6" to 1, "Wolf" to 1),
    listOf("7" to 1, "Wolf" to 2),
    listOf("8" to 1, "Wolf" to 3),
    listOf("9" to 1, "Wolf" to 4),
    listOf("10" to 1, "Wolf" to 5),
    listOf("11" to 1, "Wolf" to 6),
    listOf("12" to 1, "Phoenix" to 1),
    listOf("13" to 1, "Phoenix" to 2),
    listOf("14" to 1, "Phoenix" to 3),
    listOf("15" to 1, "Phoenix" to 4),
    listOf("16" to 1, "Snake" to 1),
    listOf("17" to 1, "Snake" to 2),
    listOf("18" to 1, "Snake" to 3),
    listOf("19" to 1, "Snake" to 4),
    listOf("20" to 1, "Dragon" to 1),
    listOf("21" to 1, "Dragon" to 2),
    listOf("J" to 1, "Monkey" to 1),
    listOf("Q" to 1, "Monkey" to 2),
    listOf("S" to 1, "Ø" to 1),
    listOf("K" to 1),
    listOf("K" to 2),
    listOf("K" to 3),
    listOf("C" to 1),
    listOf("A" to 1),
    listOf("-‽" to 1, "►" to 1),
    listOf("×2" to 1)
)

object CardSuitComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val aIndex = CardSuits.indexOf(a)
        require(aIndex >= 0) { "Unknown suit: $a" }
        val bIndex = CardSuits.indexOf(b)
        require(bIndex >= 0) { "Unknown suit: $b" }
        return aIndex - bIndex
    }
}

object CardValueComparator : Comparator<String> {
    private val values = CardRows.flatten().map { it.first }.distinct()
    override fun compare(a: String, b: String): Int {
        val aIndex = values.indexOf(a)
        require(aIndex >= 0) { "Unknown value: $a" }
        val bIndex = values.indexOf(b)
        require(bIndex >= 0) { "Unknown value: $b" }
        return aIndex - bIndex
    }
}

fun suitSymbol(suit: String) = when (suit) {
    "stars" -> "★"
    "spades" -> "♠"
    "hearts" -> "♥"
    "clubs" -> "♣"
    "diamonds" -> "♦"
    "florettes" -> "✿"
    "wheels" -> "⎈"
    "specials" -> ""
    else -> error("Unknown suit $suit")
}

fun suitDarkColor(suit: String) = when (suit) {
    "stars" -> "#6b6b6b"
    "spades" -> "#0d8c8c"
    "hearts" -> "#e00000"
    "clubs" -> "#1e9f20"
    "diamonds" -> "#767613"
    "florettes" -> "#cc00cc"
    "wheels" -> "#8600e6"
    "specials" -> "#000000"
    else -> error("Unknown suit $suit")
}