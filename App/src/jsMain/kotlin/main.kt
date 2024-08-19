import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import module.react.image_gallery.ImageGalleryStyles
import react.create
import react.dom.client.createRoot
import ui.App
import web.dom.document


fun main() {
    ImageGalleryStyles

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

    createRoot(document.getElementById("root")!!).render(App.create())
}
