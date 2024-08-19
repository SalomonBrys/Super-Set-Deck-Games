package ui

import mui.material.Box
import mui.system.sx
import mui.system.useMediaQuery
import react.FC
import react.PropsWithChildren
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.cssom.atrule.maxWidth


object ContentArea {
    val Header = ident("header")
    val List = ident("list")
    val Content = ident("content")
}

object ContentSize {
    object App {
        val maxWidth = 1600.px
    }

    object List {
        val Width = 380.px
    }

    object Filters {
        val height = 120.px
    }
}

fun minAspectRatio(width: Int, height: Int): SizeQuery =
    SizeQuery("(min-aspect-ratio:$width/$height)")

fun useMediaQuery(q: MediaQuery): Boolean = useMediaQuery(q.unsafeCast<String>())

fun useIsMobile(): Boolean = useMediaQuery(maxWidth(1172.px))

fun useIsLandscape(): Boolean = useMediaQuery(minAspectRatio(1, 1))


external interface ContentMobileProps : PropsWithChildren {
    var mainId: Ident
}

val ContentMobile = FC<ContentMobileProps> { props ->
    Box {
        sx {
            display = Display.grid
            height = 100.pct
            gridTemplateRows = array(
                Length.maxContent,
                auto,
            )
            gridTemplateColumns = array(
                auto,
            )
            gridTemplateAreas = GridTemplateAreas(
                arrayOf(ContentArea.Header),
                arrayOf(props.mainId),
            )
        }

        +props.children
    }
}
