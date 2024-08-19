package module.google

import js.coroutines.internal.IsolatedCoroutineScope
import js.objects.jso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import web.window.window


private external interface JsGoogleAccountsAPI {

    val oauth2: Oauth2

    interface Oauth2 {

        fun initTokenClient(config: TokenClientConfig): TokenClient

        interface OverridableTokenClientConfig {
            var scope: String?
        }

        interface TokenClientConfig : OverridableTokenClientConfig {
            var client_id: String
            var callback: (TokenResponse) -> Unit
        }

        interface TokenClient {
            fun requestAccessToken(overrideConfig: OverridableTokenClientConfig?)
        }

        interface TokenResponse {
            val access_token: String
        }
    }
}

object GoogleAuth {

    private val scope = IsolatedCoroutineScope()

    private val accountsDeferred = scope
        .async(Dispatchers.Unconfined) {
            val loaded = loadScript("https://accounts.google.com/gsi/client")
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            if (loaded) window.asDynamic().google.accounts as JsGoogleAccountsAPI
            else null
        }

//    val isLoaded = scope.async { accountsDeferred.await() != null }

    fun requestAccessToken(
        clientId: String,
        scopes: List<String>,
        onAccessToken: (String) -> Unit
    ) {
        scope.launch {
            val accounts = accountsDeferred.await() ?: return@launch

            val tokenClient = accounts.oauth2.initTokenClient(jso {
                this.client_id = clientId
                this.scope = scopes.joinToString(" ")
                this.callback = callback@ {
                    onAccessToken(it.access_token)
                }
            })

            tokenClient.requestAccessToken(null)
        }
    }
}
