package data

import kotlinx.browser.window
import web.storage.localStorage


object LocalConfig {

    private object Key {
        val lang = "lang"
    }

    fun getLangId(): String {
        localStorage.getItem(Key.lang)?.let { return it }

        val navs = when {
            window.navigator.languages.isNotEmpty() -> {
                window.navigator.languages.map { it.split("-")[0] } .distinct()
            }
            window.navigator.language.isNotEmpty() -> {
                listOf(window.navigator.language.split("-")[0])
            }
            else -> listOf(Lang.default)
        }

        val langId = navs.firstOrNull { it in Lang } ?: Lang.default

        localStorage.setItem(Key.lang, langId)
        return langId
    }

    suspend fun setLangId(lang: String) {
        localStorage.setItem(Key.lang, lang)
    }
}