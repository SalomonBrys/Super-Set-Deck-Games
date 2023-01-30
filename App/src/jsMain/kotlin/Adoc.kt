import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import dev.petuska.kmdc.card.Content
import dev.petuska.kmdc.card.MDCCard
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.get
import utils.forEach

@Composable
fun Adoc(url: String, section: String?, page: String, filePrefix: String) {
    val router = Router.current
    var html: String? by remember { mutableStateOf(null) }

    LaunchedEffect(url) {
        html = null
        try {
            html = window.fetch(url).await().text().await()
        } catch (e: Throwable) {
            console.error(e)
            html = "<h1>Error :(</h1>"
        }
    }

    MDCCard(attrs = {
        style {
            width(98.percent)
            maxWidth(60.cssRem)
        }
    }) {
        Content(attrs = {
            classes("adoc", "adoc-fragment")
        }) {
            DisposableEffect(html) {
                if (html != null) {
                    scopeElement.innerHTML = html!!
                    scopeElement.getElementsByTagName("a").forEach { link ->
                        link as HTMLAnchorElement
                        link.attributes["href"]?.value?.let {
                            when {
                                it.startsWith("#") -> link.href = "#$page?section=${it.removePrefix("#")}"
                                !it.startsWith("/") && "://" !in it -> link.href = "$filePrefix/${it}"
                            }
                        }
                    }
                    scopeElement.getElementsByTagName("img").forEach { img ->
                        img as HTMLImageElement
                        img.attributes["src"]?.value?.let {
                            img.src = "$filePrefix/$it"
                        }
                    }
                    scopeElement.querySelectorAll("h2, h3, h4").forEach { heading ->
                        heading as HTMLHeadingElement
                        if (heading.id.isNotEmpty()) {
                            heading.style.cursor = "pointer"
                            heading.onclick = {
                                router.navigate("$page?section=${heading.id}")
                            }
                        }
                    }
                    val divs = arrayOf(scopeElement)
                    window.setTimeout({ window.asDynamic().MathJax.typeset(divs) }, 1)
                }
                onDispose {}
            }

            var hasLoaded by remember { mutableStateOf(false) }
            DisposableEffect(html, section) {
                if (html != null) {
                    if (section != null) {
                        scopeElement.querySelector("#$section")?.scrollIntoView(if (hasLoaded) js("{ behavior: \"smooth\" }") else null)
                    }
                    hasLoaded = true
                }
                onDispose {}
            }

            if (html == null) {
                Loader()
            }
        }
    }

}
