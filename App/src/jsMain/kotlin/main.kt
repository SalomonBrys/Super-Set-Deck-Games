import androidx.compose.runtime.*
import app.softwork.routingcompose.HashRouter
import app.softwork.routingcompose.Router
import data.Game
import data.LocalLang
import data.langs
import dev.petuska.kmdc.core.jsObject
import dev.petuska.kmdc.menu.MDCMenu
import dev.petuska.kmdc.menu.MenuItem
import dev.petuska.kmdc.menu.onSelected
import dev.petuska.kmdc.menu.surface.MDCMenuSurfaceAnchor
import dev.petuska.kmdc.menu.surface.onClosed
import dev.petuska.kmdc.menu.surface.onOpened
import dev.petuska.kmdc.tooltip.MDCTooltip
import dev.petuska.kmdc.tooltip.tooltipId
import dev.petuska.kmdc.top.app.bar.ActionLink
import dev.petuska.kmdc.top.app.bar.MDCTopAppBarSectionScope
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody
import org.w3c.dom.HTMLScriptElement
import utils.Cookies
import kotlin.js.json
import kotlin.time.Duration.Companion.days


private fun getLanguage(): String {
    val cookie = Cookies["lang"]
    if (cookie != null && cookie in langs) {
        return cookie
    }

    val navs = when {
        window.navigator.languages.isNotEmpty() -> {
            window.navigator.languages.map { it.split("-")[0] } .distinct()
        }
        window.navigator.language.isNotEmpty() -> {
            listOf(window.navigator.language.split("-")[0])
        }
        else -> listOf("en")
    }

    val lang = navs.firstOrNull { it in langs } ?: "en"
    return lang
}

typealias LangMenu = @Composable MDCTopAppBarSectionScope.() -> Unit

@Composable
private fun WithLang(content: @Composable (LangMenu) -> Unit) {
    var langId by remember { mutableStateOf(getLanguage()) }

    LaunchedEffect(langId) {
        Cookies.set("lang", langId, 365.days)
    }

    val lang = langs.getValue(langId)

    CompositionLocalProvider(LocalLang provides lang) {
        content {
            var menuOpen by remember { mutableStateOf(false) }
            MDCTooltip("lang") { Text(LocalLang.current.Language) }
            ActionLink(attrs = {
                onClick { menuOpen = true }
                tooltipId("lang")
            }) {
                MDCMenuSurfaceAnchor {
                    val langIds = remember { langs.keys.sorted() }
                    MDCMenu(
                        open = menuOpen,
                        attrs = {
                            onOpened { menuOpen = true }
                            onClosed { menuOpen = false }
                            onSelected {
                                langId = langIds[it.detail.index]
                            }
                        }
                    ) {
                        langIds.forEach {
                            MenuItem { Text(it.uppercase()) }
                        }
                    }
                }
                Small {
                    Text(langId.uppercase())
                }
            }
        }
    }
}

fun homePath() = Cookies["lastFilterHash"] ?: "/"

@Composable
fun App() {
    var games by remember { mutableStateOf<List<Game>?>(null) }

    LaunchedEffect(null) {
        try {
            val response = window.fetch("games/games.json").await()
            if (!response.ok) error("${response.status} ${response.statusText}")
            games = Json.decodeFromString(response.text().await())
        } catch (e: Throwable) {
            window.alert("Error loading games: ${e.message ?: e.toString()}")
        }
    }

    WithLang { langMenu ->
        HashRouter(initPath = "/") {
            route("/game") {
                string { gameId ->
                    if (games == null) {
                        Game(null, langMenu)
                    } else {
                        val game = games!!.firstOrNull { it.id == gameId }
                        if (game != null) {
                            Game(game, langMenu)
                        } else {
                            val router = Router.current
                            LaunchedEffect(null) { router.navigate("/") }
                        }
                    }
                }
                noMatch {
                    redirect("/games")
                }
            }
            string { pageId ->
                val page = when (pageId) {
                    "games" -> {
                        LaunchedEffect(window.location.hash) {
                            Cookies.set("lastFilterHash", window.location.hash.removePrefix("#"), 14.days)
                        }
                        Page.Games
                    }
                    "packer" -> Page.Packer
                    "about" -> Page.About
                    else -> {
                        val router = Router.current
                        LaunchedEffect(Unit) { router.navigate("/games") }
                        null
                    }
                }
                Home(games, langMenu, page)
            }
            route("/games") {
                LaunchedEffect(window.location.hash) {
                    Cookies.set("lastFilterHash", window.location.hash.removePrefix("#"), 14.days)
                }
                Home(games, langMenu, Page.Games)
            }
            route("/packer") {
                Home(games, langMenu, Page.Packer)
            }
            route("/about") {
                Home(games, langMenu, Page.About)
            }
            noMatch {
                redirect("/games")
            }
        }
    }
}

@JsModule("./style.scss")
private external val style: dynamic


fun main() {
    style

    window.addEventListener("load", {
        MainScope().launch {
            try {
                window.navigator.serviceWorker.register("ServiceWorker.js").await()
                console.log("Service worker registered")
            } catch (ex: Throwable) {
                console.error("Could not register service worker: ${ex.message}")
            }
        }
    })

    renderComposableInBody { App() }
}
