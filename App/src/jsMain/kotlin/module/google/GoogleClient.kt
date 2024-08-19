package module.google

import js.coroutines.internal.IsolatedCoroutineScope
import js.objects.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import web.blob.Blob
import web.window.window
import kotlin.random.Random


private external interface JsGoogleClientAPI {

    interface TokenObject {
        var access_token: String
    }

    fun setToken(tokenObject: TokenObject)

    fun load(discoveryDocumentUrl: String): GoogleClientJs.Thenable<Nothing?>

    interface RequestArgs {
        var path: String?
        var method: String?
        var params: ReadonlyRecord<String, Any>?
        var headers: ReadonlyRecord<String, Any>?
        var body: String?
    }
    fun <T> request(args: RequestArgs): GoogleClientJs.Thenable<GoogleClientJs.Result<T>>
}

private external interface JsGoogleAPI {
    interface LoadArgs {
        var callback: () -> Unit
        var onerror: () -> Unit
        var timeout: Int
        var ontimeout: () -> Unit
    }
    fun load(libraries: String, args: LoadArgs)
    val client: JsGoogleClientAPI?
}

external interface GoogleClientJs {
    interface Thenable<T> {
        fun then(
            onFulfilled: (data: T) -> Unit,
            onRejected: (error: dynamic) -> Unit,
        )
    }
    interface Result<T> {
        val status: Int
        val headers: ReadonlyRecord<String, String>
        val result: dynamic
        val body: String?
    }
    interface Error {
        val code: Int
        val message: String
    }
}

suspend fun <T> GoogleClientJs.Thenable<T>.awaitFulfilment(): T {
    val deferred = CompletableDeferred<T>()
    then(
        onFulfilled = { deferred.complete(it) },
        onRejected = {
            deferred.completeExceptionally(RuntimeException((it as? String) ?: "Error"))
        }
    )
    return deferred.await()
}
suspend fun <T> GoogleClientJs.Thenable<GoogleClientJs.Result<T>>.awaitResult(): GoogleClientJs.Result<T> {
    val deferred = CompletableDeferred<GoogleClientJs.Result<T>>()
    then(
        onFulfilled = { deferred.complete(it) },
        onRejected = {
            val error = it.unsafeCast<GoogleClientJs.Result<*>>().error()
            deferred.completeExceptionally(RuntimeException(error.message))
        }
    )
    return deferred.await()
}

fun GoogleClientJs.Result<*>.isSuccess(): Boolean = this.status in 200..299
fun <T> GoogleClientJs.Result<T>.result(): T = result.unsafeCast<T>()
fun <T> GoogleClientJs.Result<T>.error(): GoogleClientJs.Error = result.error.unsafeCast<GoogleClientJs.Error>()

class GoogleClient private constructor(
    private val client: JsGoogleClientAPI
) {

    companion object {
        private val scope = IsolatedCoroutineScope()

        private val clientDeferred = scope
            .async(Dispatchers.Unconfined) {
                val loaded = loadScript("https://apis.google.com/js/api.js")
                if (!loaded) return@async null
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                val gapi = (window.asDynamic().gapi as JsGoogleAPI?) ?: return@async null

                val clientDeferred = CompletableDeferred<JsGoogleClientAPI?>()
                gapi.load("client", jso {
                    callback = { clientDeferred.complete(gapi.client) }
                    onerror = {
                        console.error("Error loading Google Client API")
                        clientDeferred.complete(null)
                    }
                    timeout = 10_000
                    ontimeout = {
                        console.error("Timeout loading Google Client API")
                        clientDeferred.complete(null)
                    }
                })
                clientDeferred.await()
            }

        suspend fun get() = clientDeferred.await()?.let { GoogleClient(it) }
    }

    fun setAccessToken(token: String) {
        client.setToken(jso {
            access_token = token
        })
    }

    suspend fun <T : Any> load(
        discoveryDocumentUrl: String,
        property: String,
    ): T? {
        try {
            client.load(discoveryDocumentUrl).awaitFulfilment()
        } catch (_: Throwable) {}
        return client.asDynamic()[property] as? T
    }

    suspend fun <T> request(
        path: String,
        method: String? = null,
        params: Record<String, dynamic>? = null,
        headers: Record<String, String>? = null,
        body: String? = null,
    ): T {
        return client.request<T>(jso {
            this.path = path
            this.method = method
            this.params = params
            this.headers = headers
            this.body = body
        }).awaitResult().result()
    }

    data class MultipartRequestPart(
        val contentType: String,
        val body: String
    )

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun <T> multipartRequest(
        path: String,
        parts: List<MultipartRequestPart>,
        method: String? = null,
        params: Record<String, dynamic>? = null,
        headers: Record<String, String>? = null,
    ): T {
        val boundary = Random.nextBytes(16).toHexString()
        val delim = "\r\n--$boundary\r\n"
        val closeDelim = "\r\n--$boundary--"

        val body = buildString {
            parts.forEach { part ->
                append(delim)
                append("Content-Type: ${part.contentType}\r\n\r\n")
                append(part.body)
            }
            append(closeDelim)
        }

        val fullHeaders = recordOf(
            "Content-Type" to "multipart/related; boundary=\"$boundary\""
        )

        if (headers != null) {
            Object.assign(fullHeaders, headers)
        }

        return request(
            path = path,
            method = method,
            params = params,
            headers = fullHeaders,
            body = body
        )
    }
}
