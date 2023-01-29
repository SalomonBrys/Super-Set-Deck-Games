package utils

import org.w3c.dom.HTMLCanvasElement

external interface QrCodeOptions {
    var width: Int
    var height: Int
}

external interface QrCodeJs {
    fun toCanvas(canvas: HTMLCanvasElement, text: String, options: QrCodeOptions? = definedExternally)
}

@JsModule("qrcode")
external val qrcode: QrCodeJs
