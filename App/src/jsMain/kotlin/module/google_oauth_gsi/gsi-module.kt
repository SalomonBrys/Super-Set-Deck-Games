//@file:JsModule("module.google-oauth-gsi")
//
//package module.google_oauth_gsi
//
//import web.html.HTMLElement
//
//
//external class GoogleOAuthProvider(options: ProviderOptions) {
//
//    interface ProviderOptions {
//        var clientId: String?
//        var onScriptLoadError: (() -> Unit)?
//        var onScriptLoadSuccess: (() -> Unit)?
//    }
//
//    fun useRenderButton(options: RenderButtonOptions): () -> Unit
//
//    interface AuthSuccess {
//        val clientId: String
//        val credential: String
//        val select_by: String
//    }
//
//    interface RenderButtonOptions {
//        var useOneTap: Boolean?
//        var element: HTMLElement?
//        var onError: (() -> Unit)?
//        var onSuccess: ((AuthSuccess) -> Unit)?
//    }
//
//}
