package ui.game

import emotion.react.css
import js.objects.jso
import module.qrcode.qrcode
import mui.icons.material.Share
import mui.material.*
import mui.system.sx
import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.canvas
import ui.useLang
import web.cssom.integer
import web.cssom.number
import web.cssom.pct
import web.dom.Element
import web.html.HTMLCanvasElement
import web.navigator.navigator


external interface GameShareMenuProps : Props {
    var gameId: String
    var gameName: String
}

val GameShareMenu = FC<GameShareMenuProps>("ShareMenu") { props ->
    val (_, lang) = useLang()
    var menuAnchor by useState<Element?>(null)
    val hasShare = useMemo { navigator.asDynamic().share != null }
    var qrCodeDialogShown by useState(false)

    IconButton {
        size = Size.large
        color = IconButtonColor.inherit
        ariaLabel = "share"
        onClick = { menuAnchor = it.currentTarget }

        Share()
    }
    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        sx {
            zIndex = integer(10_000)
        }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        if (hasShare) {
            MenuItem {
                onClick = {
                    navigator.shareAsync(jso {
                        title = props.gameName
                        text = props.gameName
                        url = "https://super-set-deck.games/#/games/${props.gameId}"
                    })
                        .then { menuAnchor = null }
                }
                +lang.share__Share_by_Message
            }
        }
        MenuItem {
            onClick = {
                qrCodeDialogShown = true
                menuAnchor = null
            }
            +lang.share__Share_by_QR_Code
        }
        MenuItem {
            onClick = {
                navigator.clipboard.writeTextAsync("https://super-set-deck.games/#/games/${props.gameId}")
                    .then { menuAnchor = null }
            }
            +lang.share__Copy_game_page_link
        }
    }

    Dialog {
        open = qrCodeDialogShown
        onClose = { _, _ -> qrCodeDialogShown = false }

        ShareMenuQRCodeDialog {
            +props
        }
    }
}

private val ShareMenuQRCodeDialog = FC<GameShareMenuProps> { props ->
    val canvas = useRef<HTMLCanvasElement>()

    useEffect(props.gameName, props.gameId) {
        val element = canvas.current!!
        qrcode.toCanvas(element, "https://super-set-deck.games/#/games/${props.gameId}", jso {
            width = element.width
            height = element.height
        })
    }

    DialogTitle {
        +props.gameName
    }

    canvas {
        ref = canvas
        css {
            width = 100.pct
            aspectRatio = number(1.0)
        }
    }

}