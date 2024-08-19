package ui.utils

import org.w3c.dom.DOMRect
import web.events.Event
import web.events.EventTarget
import web.events.EventType
import web.navigator.Navigator


external class VirtualKeyboard : EventTarget {
    val boundingRect: DOMRect
    var overlaysContent: Boolean
}

val Navigator.virtualKeyboard: VirtualKeyboard? get() = asDynamic().virtualKeyboard

@Suppress("NOTHING_TO_INLINE")
inline fun Event.Companion.ongeometrychange(): EventType<Event, VirtualKeyboard> =
    EventType("geometrychange")
