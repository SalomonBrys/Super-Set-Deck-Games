package utils


fun Iterable<Int>.toShortStrings(): List<String> {
    val ranges = ArrayList<String>()

    fun toRange(start: Int, end: Int): List<String> =
        when {
            start == end -> listOf("$start")
            start == end - 1 -> listOf("$start", "$end")
            else -> listOf("$start-$end")
        }

    var start = this.first()
    var end = start
    sorted().drop(1).forEach {
        if (it == end + 1) {
            end = it
        } else {
            ranges += toRange(start, end)
            start = it
            end = start
        }
    }
    ranges += toRange(start, end)

    return ranges
}
