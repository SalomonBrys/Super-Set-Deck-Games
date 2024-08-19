package ui.doc

import data.Lang
import data.name
import js.objects.jso
import kotlinx.coroutines.await
import mui.material.Box
import mui.material.Paper
import mui.material.styles.Theme
import mui.material.styles.useTheme
import mui.system.sx
import react.*
import ui.*
import web.cssom.*
import web.cssom.atrule.minWidth


external interface DocScreenProps : Props {
    var doc: String
    var translated: Boolean
    var title: (Lang) -> String
}

val DocScreen = FC<DocScreenProps> { props ->
    val (langId, lang) = useLang()
    val theme = useTheme<Theme>()
    var html by useState("")

    val htmlPath =
        if (props.translated) "docs/${props.doc}-${langId}.html"
        else "docs/${props.doc}.html"

    useEffect(props.doc, langId) {
        html = try {
            kotlinx.browser.window.fetch(htmlPath).await().text().await()
        } catch (e: Throwable) {
            console.error(e)
            "<p style=\"font-weight: bold; color: ${theme.palette.error.main};\">${lang.Error_loading_document}</p>"
        }
    }

    ContentMobile {
        key = htmlPath
        mainId = ContentArea.List

        SSDAppBar {
            navigationIcon = OpenAppDrawerIcon.create()

            SSDAppBarTitle {
                label = props.title(lang)
            }
        }

        Box {
            sx {
                gridArea = ContentArea.List
                display = Display.flex
                width = 100.pct
                backgroundColor = Color("grey.light")
                flexDirection = FlexDirection.column
                alignItems = AlignItems.center
                padding = Padding(16.px, 0.px)
                media(minWidth(600.px)) {
                    padding = Padding(16.px, 8.px)
                }
            }

            Paper {
                sx {
                    width = 100.pct - 16.px
                }

                Box {
                    sx {
                        set(CustomPropertyName("--theme-primary"), theme.palette.primary.main)
                        set(CustomPropertyName("--theme-secondary"), theme.palette.secondary.main)
                    }
                    className = ClassName("adoc adoc-fragment")
                    dangerouslySetInnerHTML = jso {
                        __html = html
                    }
                }

            }
        }
    }
}
