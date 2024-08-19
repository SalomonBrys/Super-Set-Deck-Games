package utils

import org.w3c.dom.*

private operator fun NodeList.iterator(): Iterator<Node> = iterator {
    repeat(length) { yield(item(it)) }
}
private fun NodeList.iterable() : Iterable<Node> = Iterable { iterator() }
fun Element.elements(tagName: String): List<Element> = getElementsByTagName(tagName).iterable().map { it as Element }
fun Document.elements(tagName: String): List<Element> = getElementsByTagName(tagName).iterable().map { it as Element }

fun Node.attr(name: String): String = attributes.getNamedItem(name).textContent
