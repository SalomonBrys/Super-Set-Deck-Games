package utils

inline fun <R> retry(count: Int, block: () -> R): R {
    var fails = 0
    while (true) {
        try {
            return block()
        } catch (e: Throwable) {
            if (fails >= count) throw e
            ++fails
            Thread.sleep(1000)
        }
    }
}
