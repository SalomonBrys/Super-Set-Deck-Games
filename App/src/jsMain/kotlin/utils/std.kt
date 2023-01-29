package utils

import org.w3c.dom.*


inline fun <T> ItemArrayLike<T>.forEach(block: (T) -> Unit) {
    (0 until length).forEach { block(item(it)!!) }
}
