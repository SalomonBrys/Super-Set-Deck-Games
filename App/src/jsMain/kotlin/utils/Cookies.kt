package utils

import kotlinx.browser.document
import kotlin.time.Duration
import kotlin.time.DurationUnit

object Cookies {
    fun set(key: String, value: String, duration: Duration) {
        document.cookie = "${encodeURIComponent(key)}=${encodeURIComponent(value)};max-age=${duration.toInt(DurationUnit.SECONDS)}"
    }

    fun all() =
        document.cookie.split(";")
            .filter { it.isNotEmpty() }
            .map { str ->
                val (k, v) = str.trim().split("=", limit = 2)
                    .map { decodeURIComponent(it) }
                k to v
            }
            .toMap()

    operator fun get(key: String) = all()[key]
}