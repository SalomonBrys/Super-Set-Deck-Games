package ui

import data.Lang
import data.LocalConfig
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.dom.aria.ariaLabel
import web.cssom.integer
import web.dom.Element


val LangMenu = FC("LangMenu") {
    var langId by useRequiredContext(langIdStateContext)
    var menuAnchor by useState<Element?>(null)

    IconButton {
        size = Size.large
        edge = IconButtonEdge.end
        color = IconButtonColor.inherit
        ariaLabel = "language"
        onClick = { menuAnchor = it.currentTarget }

        Typography {
            variant = TypographyVariant.body2
            +langId.uppercase()
        }
    }
    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        sx {
            zIndex = integer(10_000)
        }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        Lang.forEach { (id, lang) ->
            MenuItem {
                onClick = {
                    menuAnchor = null
                    langId = id
                }
                +lang.langName
            }
        }
    }
}

private val langIdStateContext = createRequiredContext<StateInstance<String>>()

fun useLang(): Pair<String, Lang> {
    val langId by useRequiredContext(langIdStateContext)
    return langId to (Lang[langId] ?: error("Invalid lang $langId"))
}

val LangProvider = FC<PropsWithChildren>("LangProvider") { props ->
    val langIdState = useState(LocalConfig.getLangId())
    val langId by langIdState

    useEffect(langId) {
        LocalConfig.setLangId(langId)
    }

    langIdStateContext(langIdState) {
        +props.children
    }
}
