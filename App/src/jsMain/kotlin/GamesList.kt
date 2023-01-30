import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import data.Game
import data.LocalLang
import data.name
import dev.petuska.kmdc.checkbox.MDCCheckbox
import dev.petuska.kmdc.chips.grid.ActionChip
import dev.petuska.kmdc.chips.grid.MDCChipsGrid
import dev.petuska.kmdc.chips.onInteraction
import dev.petuska.kmdc.dialog.*
import dev.petuska.kmdc.form.field.MDCFormField
import dev.petuska.kmdc.list.Divider
import dev.petuska.kmdc.list.MDCList
import dev.petuska.kmdc.list.item.ListItem
import dev.petuska.kmdc.list.item.Primary
import dev.petuska.kmdc.list.item.Secondary
import dev.petuska.kmdc.list.item.Text
import dev.petuska.kmdc.list.onAction
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Text
import utils.*
import kotlin.time.Duration.Companion.days

private fun Router.applyFilter(playerCount: Int, gameType: String?, favorites: Boolean) {
    val params = buildList {
        if (playerCount != 0) add("playerCount=$playerCount")
        if (gameType != null) add("gameType=${encodeURIComponent(gameType)}")
        if (favorites) add("favorites=1")
    }
    navigate(
        if (params.isNotEmpty()) "/games?${params.joinToString("&")}"
        else "/games"
    )
}

private fun List<Game>.playerCounts(gameType: String?): List<Int> =
    filter { if (gameType == null) true else gameType in it.types }
        .flatMap { it.playerCount }
        .distinct().sorted()

@Composable
private fun PlayerCountFilterDialog(
    isDialogOpen: Boolean,
    setDialogOpen: (Boolean) -> Unit,
    allCounts: List<Int>,
    availableCounts: List<Int>,
    applyFilter: (Int) -> Unit
) {
    MDCDialog(
        open = isDialogOpen,
        attrs = {
            onOpened { setDialogOpen(true) }
            onClosed { setDialogOpen(false) }
            onClosing {
                val action = it.detail.action ?: return@onClosing
                if (action.startsWith("s:")) {
                    applyFilter(if (action == "s:all") 0 else action.removePrefix("s:").toInt())
                }
            }
        }
    ) {
        Title(LocalLang.current.PlayerCount)
        Content {
            val playerCounts = availableCounts
            MDCList {
                ListItem(
                    text = "${allCounts.first()} - ${allCounts.last()}",
                    attrs = {
                        mdcDialogAction("s:all")
                    }
                )
                Divider()
                playerCounts.forEach {
                    ListItem(
                        text = "$it",
                        attrs = {
                            mdcDialogAction("s:$it")
                        }
                    )
                }
            }
        }
        Actions {
            Action(action = "close", text = LocalLang.current.Cancel)
        }
    }
}

@Composable
private fun GameTypeFilterDialog(
    isDialogOpen: Boolean,
    setDialogOpen: (Boolean) -> Unit,
    gameTypes: List<String>,
    applyFilter: (String?) -> Unit
) {

    MDCDialog(
        open = isDialogOpen,
        attrs = {
            onOpened { setDialogOpen(true) }
            onClosed { setDialogOpen(false) }
            onClosing {
                val action = it.detail.action ?: return@onClosing
                if (action.startsWith("s:")) {
                    applyFilter(if (action == "s:all") null else action.removePrefix("s:"))
                }
            }
        }
    ) {
        Title(LocalLang.current.GameType)
        Content {
            MDCList {
                ListItem(
                    text = LocalLang.current.AllTypes,
                    attrs = {
                        mdcDialogAction("s:all")
                    }
                )
                Divider()

                gameTypes.forEach {
                    ListItem(
                        text = LocalLang.current.gameTypes[it] ?: it,
                        attrs = {
                            mdcDialogAction("s:$it")
                        }
                    )
                }
            }
        }
        Actions {
            Action(action = "close", text = LocalLang.current.Cancel)
        }
    }
}

@Composable
private fun GamesListFilters(games: List<Game>, playerCount: Int, gameType: String?, favorites: Boolean) {

    @Suppress("NAME_SHADOWING") val playerCount by rememberUpdatedState(playerCount)
    @Suppress("NAME_SHADOWING") val gameType by rememberUpdatedState(gameType)
    @Suppress("NAME_SHADOWING") val favorites by rememberUpdatedState(favorites)

    val lang = LocalLang.current
    val router = Router.current

    val filterDialogs: MutableMap<String, Boolean> = remember { mutableStateMapOf() }

    val allCounts = remember { games.playerCounts(null) }

    MDCChipsGrid(attrs = {
        style { marginTop(0.5.em) }
        onInteraction {
            filterDialogs[it.detail.chipID] = true
        }
    }) {
        ActionChip(id = "playerCount", touch = true) {
            Text(when (playerCount) {
                0 -> "${allCounts.first()}-${allCounts.last()} ${lang.players}"
                1 -> "1 ${lang.player}"
                else -> "$playerCount ${lang.players}"
            })
        }
        ActionChip(id = "gameType", touch = true) {
            Text(gameType?.let { lang.gameTypes[it] ?: it } ?: lang.AllTypes)
        }
    }

    val availableCounts = remember(gameType) { games.playerCounts(gameType) }

    PlayerCountFilterDialog(
        isDialogOpen = filterDialogs.getOrElse("playerCount") { false },
        setDialogOpen = { filterDialogs["playerCount"] = it },
        allCounts = allCounts,
        availableCounts = availableCounts
    ) {
        router.applyFilter(playerCount = it, gameType = gameType, favorites = favorites)
    }

    val gameTypes = remember(playerCount) {
        games
            .filter { if (playerCount == 0) true else playerCount in it.playerCount }
            .flatMap { it.types }
            .distinct()
            .sortedBy { lang.gameTypes[it] ?: it }
    }

    GameTypeFilterDialog(
        isDialogOpen = filterDialogs.getOrElse("gameType") { false },
        setDialogOpen = { filterDialogs["gameType"] = it },
        gameTypes = gameTypes
    ) {
        router.applyFilter(playerCount = playerCount, gameType = it, favorites = favorites)
    }

    MDCFormField {
        MDCCheckbox(
            checked = favorites,
            touch = true,
            label = lang.Favorites_only,
            attrs = {
                onChange {
                    router.applyFilter(playerCount = playerCount, gameType = gameType, favorites = it.value)
                }
            }
        )
    }
}

@Composable
fun Welcome() {
    val router = Router.current
    var about by remember { mutableStateOf(Cookies["first-time"]) }
    MDCDialog(
        open = about == null,
        attrs = {
            onClosed {
                about = it.detail.action
            }
        },
        scrimClickAction = ""
    ) {
        Title(LocalLang.current.Welcome)
        Content {
            Text(LocalLang.current.FirstTime)
            Br()
            Text(LocalLang.current.GoToAbout)
        }
        Actions {
            Action(action = "goTo", text = LocalLang.current.GoToAboutYes)
            Action(action = "close", text = LocalLang.current.GoToAboutNo)
        }
    }
    LaunchedEffect(about) {
        if (about != null) {
            Cookies.set("first-time", "done", 365.days)
        }
        if (about == "goTo") {
            router.navigate("/about")
        }
    }

}

@Composable
fun GamesList(games: List<Game>, playerCount: Int, gameType: String?, favorites: Boolean) {
    GamesListFilters(games, playerCount, gameType, favorites)

    val lang = LocalLang.current
    val router = Router.current

    val favs = remember(Cookies["favs"]) { Cookies["favs"]?.split(",")?.map { decodeURIComponent(it) }?.toSet() ?: emptySet() }

    val filteredGames by rememberUpdatedState(remember(playerCount, gameType, favorites, lang.id) {
        games
            .filter { if (playerCount == 0) true else playerCount in it.playerCount }
            .filter { if (gameType == null) true else gameType in it.types }
            .filter { if (favorites) it.id in favs else true }
            .sortedBy { it.name(lang) }
    })

    MDCList(
        attrs = {
            style {
                width(100.percent)
                maxWidth(26.cssRem)
                marginBottom(2.cssRem)
            }
            onAction {
                router.navigate("/game/${filteredGames[it.detail.index].id}")
            }
        }
    ) {
        filteredGames.forEach {
            ListItem(
                attrs = {
                    style {
                        paddingTop(0.2.cssRem)
                        paddingBottom(0.6.cssRem)
                    }
                }
            ) {
                FlexRow(alignItems = AlignItems.Center) {
                    if (it.id in favs) {
                        I({
                            classes("material-icons")
                            style { width(2.cssRem) }
                        }) { Text("star") }
                    } else {
                        Div({
                            style { width(2.cssRem) }
                        })
                    }
                    Div {
                        Text {
                            Primary(it.name)
                            Secondary("${it.playerCount.toShortStrings().joinToString()} ${lang.players}")
                        }
                    }
                }
            }
        }
    }

    Welcome()
}
