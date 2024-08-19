package ui

import data.AppDatabase
import data.GamesProvider
import data.Lang
import emotion.react.Global
import emotion.react.styles
import js.objects.jso
import mui.icons.material.Close
import mui.material.*
import mui.material.Size
import mui.material.styles.ThemeProvider
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.dom.aria.ariaLabel
import react.router.*
import react.router.dom.ScrollRestoration
import react.router.dom.createHashRouter
import ui.doc.DocScreen
import ui.game.GameContent
import ui.game.GamesScreen
import ui.packer.PackerScreen
import ui.lists.ListsScreen
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.timers.setTimeout
import kotlin.time.Duration.Companion.milliseconds


val App = FC("App") {
    LangProvider {
        ThemeProvider {
            theme = appTheme

            Global {
                styles {
                    "html" {
                        backgroundColor = Color("#eee")
                    }
                    "body" {
                        overflowY = Overflow.scroll
                    }
                    "#root" {
                        height = 100.vh
                        backgroundColor = Color("#eee")
                    }
                }
            }
            CssBaseline()

            AppContent()
        }
    }
}

object NavFromApp

private val AppContent = FC("AppContent") {
    val (_, lang) = useLang()

    val errors: MutableList<String> = useMemo { ArrayList() }
    var errorMessage by useState("")
    var errorSnackOpen by useState(false)

    fun addError(error: String) {
        errors += error
        errorMessage = errors.first()
        errorSnackOpen = true
    }

    fun dismissError() {
        errorSnackOpen = false
        setTimeout(250.milliseconds) {
            errorMessage = ""
            errors.removeFirst()
            if (errors.isNotEmpty()) {
                errorMessage = errors.first()
                errorSnackOpen = true
            }
        }
    }

    useEffectOnce {
        val dbResult = AppDatabase.getResult()
        dbResult.exceptionOrNull()?.let {
            addError(lang.Error_opening_database(it.message ?: "Unknown error"))
        }
    }

    Box {
        sx {
            maxWidth = ContentSize.App.maxWidth
            margin = auto
            position = Position.relative
        }

        GamesProvider {
            onError = { addError(lang.Error_loading_games(it)) }
            RouterProvider {
                router = appRouter
            }
        }
    }

    Snackbar {
        open = errorSnackOpen
        message = ReactNode(errors.firstOrNull() ?: "")
        anchorOrigin = jso {
            vertical = SnackbarOriginVertical.bottom
            horizontal = SnackbarOriginHorizontal.center
        }
        action = Fragment.create {
            IconButton {
                size = Size.small
                ariaLabel = "close"
                color = IconButtonColor.inherit
                onClick = {
                    dismissError()
                }

                Close {
                    fontSize = SvgIconSize.small
                }
            }
        }
    }
}

private val AppRootRoute = FC("AppRootRoute") {
    val outlet = useOutlet()

    ScrollRestoration()

    if (outlet == null) {
        Navigate {
            to = "/games"
            replace = true
        }
    }

    ProvideAppDrawer {
        +outlet
    }
}

private val RouteError = FC("RouteError") {
    val navigate = useNavigate()

    ProvideAppDrawer {
        SSDAppBar {
            navigationIcon = OpenAppDrawerIcon.create()
        }

        Box {
            sx {
                margin = Margin(32.px, auto)
                width = 400.px
                maxWidth = 90.pct
                display = Display.flex
                flexDirection = FlexDirection.column
                alignItems = AlignItems.center
                justifyContent = JustifyContent.center
                padding = 16.px
            }
            Typography {
                variant = TypographyVariant.h3
                sx {
                    color = Color("error.main")
                    textAlign = TextAlign.center
                }
                +"Unknown page"
            }
            Typography {
                variant = TypographyVariant.body1
                sx {
                    textAlign = TextAlign.center
                }
                +"How did you end up here?!?"
            }
            Button {
                variant = ButtonVariant.contained
                onClick = {
                    navigate("/games", jso { state = NavFromApp })
                }

                sx {
                    marginTop = 32.px
                }

                Typography {
                    variant = TypographyVariant.h4
                    +"See Games!"
                }
            }
        }
    }
}

private val appRouter = createHashRouter(
    routes = arrayOf(
        RouteObject(
            path = "/",
            element = AppRootRoute.create(),
            errorElement = RouteError.create(),
            children = arrayOf(
                RouteObject(
                    path = "games",
                    element = GamesScreen.create(),
                    children = arrayOf(
                        RouteObject(
                            path = ":id",
                            element = GameContent.create()
                        )
                    )
                ),
                RouteObject(
                    path = "lists",
                    element = ListsScreen.create(),
                ),
                RouteObject(
                    path = "packer",
                    element = PackerScreen.create()
                ),
                RouteObject(
                    path = "about",
                    element = DocScreen.create {
                        doc = "about"
                        translated = true
                        title = Lang::title_About
                    }
                ),
                RouteObject(
                    path = "changelog",
                    element = DocScreen.create {
                        doc = "changelog"
                        translated = false
                        title = Lang::title_Changelog
                    }
                ),
            )
        ),
    ),
)
