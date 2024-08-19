package ui.game

import ui.ContentArea
import ui.ContentSize
import ui.OpenAppDrawerIcon
import ui.SSDAppBar
import ui.SSDAppBarTitle
import data.name
import data.useGame
import js.objects.jso
import mui.icons.material.Close
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.FC
import react.Props
import react.create
import react.dom.aria.ariaLabel
import react.router.useNavigate
import react.router.useOutlet
import react.router.useParams
import react.useState
import ui.useLang
import ui.utils.CombinedParamsProvider
import ui.utils.useGamesCombineSearchParams
import web.cssom.*
import web.cssom.Auto.Companion.auto


val GamesScreenLarge = FC("GamesListLarge") {
    var count by useState<Int?>(null)
    val (_, lang) = useLang()
    val params = useParams()

    Box {
        sx {
            display = Display.grid
            height = 100.pct
            gridTemplateRows = array(
                Length.maxContent,
                auto,
            )
            gridTemplateColumns = array(
                ContentSize.List.Width,
                auto,
            )
            gridTemplateAreas = GridTemplateAreas(
                arrayOf(ContentArea.Header, ContentArea.Header),
                arrayOf(ContentArea.List, ContentArea.Content),
            )
        }
        GamesListLargeHeader {
            listTitle = if (count != null) lang.X_games(count!!) else ""
            gameId = params.get("id")
        }
        CombinedParamsProvider {
            GamesList {
                scrollableList = true
                onCount = { count = it }
                forwardFilters = true
                selected = params["id"]
            }
            GameScreenLargeGameContent()
        }
    }
}

private external interface GamesListLargeHeaderProps : Props {
    var listTitle: String
    var gameId: String?
}

private val GamesListLargeHeader = FC<GamesListLargeHeaderProps>("GamesListLargeHeader") { props ->
    val (langId, lang) = useLang()
    val game = useGame(props.gameId)
    val navigate = useNavigate()
    val (filters) = useFilters()

    SSDAppBar {
        navigationIcon = OpenAppDrawerIcon.create()

        SSDAppBarTitle {
            sx {
                flexGrow = number(0.0)
                width = ContentSize.List.Width - 122.px
            }
            label = props.listTitle
        }

        Box {
            sx {
                width = 60.px
                height = 64.px
            }
        }

        if (game != null) {
            IconButton {
                size = Size.large
                color = IconButtonColor.inherit
                ariaLabel = "close"
                onClick = {
                    navigate("/games?${filters.urlSearchParams()}")
                }

                Close()
            }
        }

        SSDAppBarTitle {
            label = game?.name(langId) ?: ""
        }

        if (game != null) {
            GameShareMenu {
                gameId = game.id
                gameName = game.name(langId)
            }
        }
    }
}

private val GameScreenLargeNoGameContent = FC("NoGameContent") {
    Box {
        sx {
            gridArea = ContentArea.Content
            width = 100.pct
            height = 100.pct
            backgroundColor = Color("grey.light")
            display = Display.flex
            flexDirection = FlexDirection.column
            justifyContent = JustifyContent.center
            alignItems = AlignItems.center
            paddingTop = 4.em
        }
        Typography {
            sx {
                color = Color("custom.fadeOnLightGrey")
                alignSelf = AlignSelf.center
            }
            variant = TypographyVariant.h2
            +"Super-Set Deck"
        }
    }
}

private val GameScreenLargeGameContent = FC {
    val outlet = useOutlet()
    val (_, setParams) = useGamesCombineSearchParams()

    OnGameFilterProvider {
        onGameFilter = { filters ->
            setParams({
                it.applyFilters(filters)
            }, jso {
                replace = true
                preventScrollReset = true
            })
        }

        if (outlet != null) {
            +outlet
        } else {
            GameScreenLargeNoGameContent()
        }
    }
}