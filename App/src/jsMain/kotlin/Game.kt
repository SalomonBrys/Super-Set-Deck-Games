import androidx.compose.runtime.*
import app.softwork.routingcompose.RouteBuilder
import app.softwork.routingcompose.Router
import data.Game
import data.LocalLang
import data.name
import dev.petuska.kmdc.card.Content
import dev.petuska.kmdc.card.MDCCard
import dev.petuska.kmdc.card.PrimaryAction
import dev.petuska.kmdc.chips.grid.ActionChip
import dev.petuska.kmdc.chips.grid.MDCChipsGrid
import dev.petuska.kmdc.chips.onInteraction
import dev.petuska.kmdc.dialog.*
import dev.petuska.kmdc.menu.MDCMenu
import dev.petuska.kmdc.menu.MenuItem
import dev.petuska.kmdc.menu.onSelected
import dev.petuska.kmdc.menu.surface.MDCMenuSurfaceAnchor
import dev.petuska.kmdc.menu.surface.onClosed
import dev.petuska.kmdc.menu.surface.onOpened
import dev.petuska.kmdc.tab.Content
import dev.petuska.kmdc.tab.Label
import dev.petuska.kmdc.tab.Tab
import dev.petuska.kmdc.tab.bar.MDCTabBar
import dev.petuska.kmdc.tab.bar.onActivated
import dev.petuska.kmdc.tab.indicator.Content
import dev.petuska.kmdc.tab.indicator.Indicator
import dev.petuska.kmdc.tab.indicator.MDCTabIndicatorType
import dev.petuska.kmdc.tab.scroller.Scroller
import dev.petuska.kmdc.tooltip.MDCTooltip
import dev.petuska.kmdc.tooltip.tooltipId
import dev.petuska.kmdc.top.app.bar.*
import dev.petuska.kmdcx.icons.MDCIcon
import dev.petuska.kmdcx.icons.mdcIcon
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.INSTANT
import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import org.w3c.dom.events.EventListener
import utils.*
import kotlin.js.json
import kotlin.time.Duration.Companion.days

@Composable
private fun GameRules(game: Game, section: String?) {
    val router = Router.current

    H1({
        style {
            fontFamily("Picon-Extended", "sans-serif")
            color(Color("var(--mdc-theme-primary)"))
            padding(0.5.cssRem)
            textAlign("center")
            fontSize(2.5.em)
            marginBottom(0.5.cssRem)
        }
    }) {
        var isFav by remember { mutableStateOf(Cookies["favs"]?.split(",")?.contains(encodeURIComponent(game.id)) ?: false) }

        MDCIconButton(
            icon = if (isFav) MDCIcon.Star else MDCIcon.StarBorder,
            touch = true,
            attrs = {
                onClick {
                    val newFav = !isFav
                    val favs = Cookies["favs"]?.split(",")?.toSet() ?: emptySet()
                    Cookies.set(
                        "favs",
                        (if (newFav) favs + encodeURIComponent(game.id) else favs - encodeURIComponent(game.id)).joinToString(","),
                        (5 * 365).days
                    )
                    isFav = newFav
                }
                mdcIcon()
            }
        )
        Text(game.names[LocalLang.current.id] ?: game.names["en"] ?: game.names.values.firstOrNull() ?: "")
    }

    MDCChipsGrid(attrs = {
        onInteraction {
            router.navigate("/games?gameType=${it.detail.chipID}")
        }
    }) {
        game.types.forEach {
            val encoded = encodeURIComponent(it)
            ActionChip(
                id = encoded,
            ) {
                Text(LocalLang.current.gameTypes[it] ?: it)
            }
        }
    }

    P {
        Text("${game.playerCount.toShortStrings().joinToString()} ${LocalLang.current.players}")
    }

    val langId = LocalLang.current.id.takeIf { it in game.names } ?: "en".takeIf { it in game.names } ?: game.names.values.first()
    Adoc(
        url = "games/${game.id}/${langId}.html",
        section = section,
        page = "/game/${game.id}",
        filePrefix = "games/${game.id}"
    )
}

@Composable
fun GameReferences(game: Game) {
    var focusRefId: String? by remember { mutableStateOf(null) }

    val noSleep = remember { NoSleep() }

    FlexRow(JustifyContent.Center, AlignItems.Center, {
        style {
            flexWrap(FlexWrap.Wrap)
        }
    }) {
        game.references.sorted().forEach { ref ->
            MDCCard(
                attrs = {
                    style {
                        margin(1.em)
                        overflow("hidden")
                    }
                    onClick {
                        noSleep.enable()
                        focusRefId = ref
                    }
                }
            ) {
                Content {
                    H4({
                        style {
                            textAlign("center")
                            margin(0.2.em)
                        }
                    }) {
                        Text(ref.replace('_', ' '))
                    }
                }
                PrimaryAction {
                    Img(src = "games/${game.id}/R-$ref.png") {
                        style {
                            maxWidth("min(60vw, 300px)")
                            maxHeight("80vh")
                        }
                    }
                }
            }
        }
    }

    FlexColumn(JustifyContent.Center, AlignItems.Center, {
        style {
            position(Position.Fixed)
            width(100.percent)
            height(100.percent)
            top(0.percent)
            left(0.percent)
            backgroundColor(Color.black)
            property("z-index", "10")
            if (focusRefId != null) {
                opacity(1)
            } else {
                opacity(0)
                property("pointer-events", "none")
            }
            property("transition", "opacity 150ms")
            overflow("hidden")
        }
    }) {
        if (focusRefId != null) {
            RefViewer(game, focusRefId!!) {
                focusRefId = it
                if (it == null) {
                    noSleep.disable()
                }
            }
        }
    }
}

@Composable
fun RouteBuilder.Game(game: Game?, langMenu: LangMenu) {
    var selectedTab by remember { mutableStateOf(-1) }
    var shareDialogOpen by remember { mutableStateOf(false) }

    val router = Router.current

    MDCTopAppBar {
        TopAppBar({
            onNav { router.navigate("/") }
        }) {
            Row {
                val gameName = game?.name ?: ""
                Section(MDCTopAppBarSectionAlign.Start) {
                    MDCTooltip("back") { Text(LocalLang.current.Back) }
                    NavButton(
                        touch = true,
                        attrs = {
                            mdcIcon()
                            tooltipId("back")
                        }
                    ) { Text(MDCIcon.ArrowBack.type) }
                    Title(gameName)
                }
                Section(MDCTopAppBarSectionAlign.End) {
                    var shareMenuOpen by remember { mutableStateOf(false) }
                    MDCTooltip("share") { Text(LocalLang.current.Share) }
                    ActionButton(attrs = {
                        mdcIcon()
                        onClick {
                            shareMenuOpen = true
                        }
                        tooltipId("share")
                    }) {
                        MDCMenuSurfaceAnchor {
                            MDCMenu(
                                open = shareMenuOpen,
                                attrs = {
                                    onOpened { shareMenuOpen = true }
                                    onClosed { shareMenuOpen = false }
                                    onSelected {
                                        when (it.detail.item.id) {
                                            "link" -> {
                                                val share = json(
                                                    "title" to gameName,
                                                    "url" to "https://super-set-deck.games/#/game/${game?.id}"
                                                )
                                                window.navigator.asDynamic().share(share)
                                            }
                                            "qrcode" -> {
                                                shareDialogOpen = true
                                            }
                                        }
                                    }
                                }
                            ) {
                                if (window.navigator.asDynamic().share) {
                                    MenuItem(attrs = { id("link") }) { Text(LocalLang.current.ShareByMessage) }
                                }
                                MenuItem(attrs = { id("qrcode") }) { Text(LocalLang.current.ShareByQrCode) }
                            }
                        }

                        Text(MDCIcon.Share.type)
                    }

                    langMenu()
                }
            }
        }
        Main {
            if (game?.references?.isNotEmpty() == true) {
                MDCTabBar({
                    onActivated {
                        when (it.detail.index) {
                            0 -> router.navigate("/game/${game.id}")
                            1 -> router.navigate("/game/${game.id}/references")
                        }
                    }
                }) {
                    Scroller {
                        Tab(active = selectedTab == 0) {
                            Content {
                                Icon(MDCIcon.Gavel)
                                Label(LocalLang.current.Rules)
                            }
                            Indicator(active = selectedTab == 0) { Content(MDCTabIndicatorType.Underline) }
                        }
                        Tab(active = selectedTab == 1) {
                            Content {
                                Icon(MDCIcon.Lightbulb)
                                Label(LocalLang.current.References)
                            }
                            Indicator(active = selectedTab == 1) { Content(MDCTabIndicatorType.Underline) }
                        }
                    }
                }
            }

            FlexColumn(JustifyContent.Center, AlignItems.Center, {
                style {
                    marginBottom(2.cssRem)
                }
            }) {
                if (game == null) {
                    Loader()
                } else {
                    route("/references") {
                        SideEffect { selectedTab = 1 }
                        GameReferences(game)
                    }
                    string {
                        LaunchedEffect(null) { router.navigate("/game/${game.id}") }
                    }
                    noMatch {
                        SideEffect { selectedTab = 0 }
                        GameRules(game, parameters?.map?.get("section")?.firstOrNull()?.let { decodeURIComponent(it) })
                    }
                }
            }
        }
    }

    MDCDialog(
        open = shareDialogOpen,
        attrs = {
            onOpened { shareDialogOpen = true }
            onClosed { shareDialogOpen = false }
        }
    ) {
        Title(game?.name ?: "")
        Content {
            Canvas({
                style {
                    width(100.percent)
                    property("aspect-ratio", "1")
                }
            }) {
                DisposableEffect(shareDialogOpen, game) {
                    if (!shareDialogOpen) return@DisposableEffect onDispose {}
                    game?.id?.let {
                        val options = js("({})").unsafeCast<QrCodeOptions>().apply {
                            width = scopeElement.width
                            height = scopeElement.height
                        }
                        qrcode.toCanvas(scopeElement, "https://super-set-deck.games/#/game/$it", options)
                    }
                    onDispose {}
                }
            }
        }
        Actions {
            Action("close") { Text(LocalLang.current.Close) }
        }
    }

}

@Composable
fun RefViewer(
    game: Game,
    focusRefId: String,
    onRefChange: (String?) -> Unit
) {
    P({
        style {
            position(Position.Absolute)
            top(0.2.em)
            right(0.2.em)
            fontWeight(900)
            color(Color.white)
            fontSize(3.em)
            cursor("pointer")
            property("z-index", "12")
            padding(0.em)
            margin(0.em)
        }
        onClick { onRefChange(null) }
    }) {
        Text("×")
    }
    val references = remember(game) { game.references.sorted() }
    Div({
        style {
            width(100.percent)
            height(100.percent)
            overflowX("scroll")
            overflowY("hidden")
        }
    }) {
        var isPortrait by remember { mutableStateOf(window.innerHeight >= window.innerWidth) }
        DisposableEffect(null) {
            val onResize = EventListener {
                isPortrait = window.innerHeight >= window.innerWidth
            }
            window.addEventListener("resize", onResize)
            onDispose { window.removeEventListener("resize", onResize) }
        }
        val imgWidth = if (isPortrait || references.size == 1) 100.vw else 50.vw
        val imgSep = if (isPortrait) 50 else 0

        var counter by remember { mutableStateOf(0) }
        DisposableEffect(focusRefId, counter) {
            val index = references.indexOf(focusRefId)
            scopeElement.scrollTo(ScrollToOptions(
                left = (index * (window.innerWidth + imgSep)).toDouble(),
                behavior = if (counter == 0) ScrollBehavior.INSTANT else ScrollBehavior.SMOOTH
            ))
            onDispose {}
        }
        Div({
            style {
                width(game.references.size * imgWidth + (game.references.size - 1) * imgSep.px)
                height(100.percent)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Row)
                flexWrap(FlexWrap.Nowrap)
                justifyContent(JustifyContent.SpaceBetween)
                overflow("hidden")
            }
        }) {
            for (ref in references) {
                Img(src = "games/${game.id}/R-$ref.png") {
                    style {
                        width(imgWidth)
                        height(100.vh)
                        property("object-fit", "contain")
                        transform { scale(1.06) }
                        property("z-index", "11")
                    }
                    onClick {
                        onRefChange(ref)
                        ++counter
                    }
                }
            }
        }
    }
}
