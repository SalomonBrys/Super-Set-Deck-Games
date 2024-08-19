package module.google

import kotlinx.coroutines.CompletableDeferred
import web.dom.document
import web.events.EventHandler
import web.html.HTML


suspend fun loadScript(url: String): Boolean {
    val deferred = CompletableDeferred<Boolean>()
    document.body.appendChild(
        document.createElement(HTML.script).apply {
            src = url
            async = true
            defer = true
            onload = EventHandler {
                deferred.complete(true)
            }
            onerror = {
                deferred.complete(false)
                Unit
            }
        }
    )
    return deferred.await()
}
