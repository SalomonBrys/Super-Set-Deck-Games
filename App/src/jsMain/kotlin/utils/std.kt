package utils

import org.w3c.dom.ItemArrayLike


inline fun <T> ItemArrayLike<T>.forEach(block: (T) -> Unit) {
    (0 until length).forEach { block(item(it)!!) }
}
