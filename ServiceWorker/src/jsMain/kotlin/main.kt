import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import org.w3c.fetch.Response
import org.w3c.workers.ExtendableEvent
import org.w3c.workers.FetchEvent
import org.w3c.workers.ServiceWorkerGlobalScope


external val self: ServiceWorkerGlobalScope

const val swVersion = "15"

var installed: Boolean = false

suspend fun openCache() = self.caches.open("cache-$swVersion").await()

suspend fun installCache() {
    if (installed) return
    installed = true

    val cache = openCache()

    val resources = self.fetch("resources.txt").await().text().await().lines()
    val rVersion = resources.first()

    val cVersion = try {
        (cache.match("cache-resource-date").await() as? Response)?.text()?.await()
    } catch (_: Throwable) {
        null
    }

    if (cVersion != rVersion) {
        console.log("Updating cache to $rVersion (was $cVersion)")

        resources.drop(1).forEach {
            try {
                cache.put(it, self.fetch(it).await())
            } catch (ex: Throwable) {
                console.log("Could not install $it in cache: ${ex.message}")
            }
        }

        cache.put("cache-resource-date", Response(rVersion)).await()
    } else {
        console.log("Cache is up to date to $rVersion")
    }
}

fun main() {

    self.oninstall = {
        console.log("Service Worker $swVersion installing...")
        (it as ExtendableEvent).waitUntil(MainScope().promise {
            installCache()
            console.log("Service Worker $swVersion installed!")
        })
    }

    self.onactivate = {
        console.log("Service Worker $swVersion activating...")
        (it as ExtendableEvent).waitUntil(MainScope().promise {
            installCache()
            console.log("Service Worker $swVersion active!")
        })
    }

    self.addEventListener("fetch", {
        it as FetchEvent
//        console.log("$version: ${it.request.url}")

        it.respondWith(MainScope().promise {
            val cache = openCache()
            try {
                val response = self.fetch(it.request).await()
                cache.put(it.request, response.clone()).await()
                response
            } catch (ex: Throwable) {
                val res = cache.match(it.request).await() ?: throw ex
                res as Response
            }
        })
    })
}
