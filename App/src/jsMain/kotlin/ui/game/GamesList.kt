package ui.game

import ui.ContentArea
import ui.ContentSize
import ui.NavFromApp
import data.*
import emotion.react.css
import js.objects.jso
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.PropsWithSx
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.img
import react.router.useNavigate
import ui.useLang
import ui.utils.toShortStrings
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.cssom.Display.Companion.block
import web.html.HTMLUListElement


enum class SortOrder(
    val text: (Lang) -> String,
) {
    Name(Lang::filter__by_Name),
    Rating(Lang::filter__by_Rating),
    Year(Lang::filter__by_Year),
}

external interface GamesListProps : Props {
    var stickyFilters: Boolean?
    var scrollableList: Boolean?
    var onCount: ((Int) -> Unit)?
    var forwardFilters: Boolean?
    var selected: String?
}

val GamesList = FC<GamesListProps>("GamesList") { props ->
    val allGames = useGames()
    val (langId, _) = useLang()

    val (filters) = useFilters()
    val listRef = useRef<HTMLUListElement>()

    useEffect(filters.hashCode()) {
        listRef.current?.scrollTo(0.0, 0.0)
    }

    var listGames by useState(allGames)

    useEffect(allGames, filters.userList) {
        if (allGames == null || filters.userList == null) {
            listGames = allGames
        }
        else {
            AppDatabase.get()?.watchAllGamesInUserList(filters.userList.id)?.collect { gameIds ->
                listGames = gameIds.mapNotNull { allGames[it] }
            } ?: run {
                listGames = allGames
            }
        }
    }

    val filteredGames = useMemo(listGames, filters) {
        if (listGames == null) return@useMemo null

        val filteredGames = listGames!!.filter(
            types = filters.types,
            playerCounts = filters.playerCounts,
            search = filters.search,
        ).sortedBy { it.name(langId) }

        when (filters.sort) {
            SortOrder.Name -> filteredGames
            SortOrder.Rating -> filteredGames.sortedByDescending { it.bgg.rating }
            SortOrder.Year -> filteredGames.sortedByDescending { it.bgg.yearPublished }
        }
    }

    useEffect(filteredGames?.size) {
        filteredGames?.size?.let {
            props.onCount?.invoke(it)
        }
    }

    Box {
        sx {
            gridArea = ContentArea.List
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
            backgroundColor = Color("background.default")

            if (props.scrollableList == true) {
                position = Position.fixed
                height = 100.vh - 64.px
                top = 64.px
                width = ContentSize.List.Width
                backgroundColor = Color("background.default")
            }
        }

        if (filteredGames == null) {
            CircularProgress {
                size = 4.em
                color = CircularProgressColor.primary
                sx {
                    margin = Margin(4.em, auto)
                    display = block
                }
            }
            return@Box
        }

        GamesFilters {
            this.sticky = props.stickyFilters
            this.filteredGames = filteredGames!!
        }

        if (props.stickyFilters == true) {
            Box {
                sx {
                    width = 100.pct
                    height = ContentSize.Filters.height
                    backgroundColor = Color("grey.light")
                }
            }
        }

        GamesListList {
            ref = listRef
            games = filteredGames!!
            forwardFilters = props.forwardFilters
            selected = props.selected
            sx {
                if (props.scrollableList == true) {
                    height = 0.px
                    flexGrow = number(1.0)
                    overflowY = Overflow.scroll
                }
            }
        }
    }
}

private external interface GamesListListProps : PropsWithSx, PropsWithRef<HTMLUListElement> {
    var games: List<Game>
    var forwardFilters: Boolean?
    var selected: String?
}

private val GamesListList = ForwardRef<GamesListListProps> { props ->
    val (langId, lang) = useLang()
    val navigate = useNavigate()
    val (filters) = useFilters()

    List {
        ref = props.ref
        disablePadding = true
        sx {
            width = 100.pct
            +props.sx
        }
        props.games.forEach { game ->
            ListItem {
                disablePadding = true
                selected = game.id == props.selected
                sx {
                    maxWidth = ContentSize.List.Width
                    margin = auto
                }

                ListItemButton {
                    onClick = {
                        navigate(
                            to = game.id + if (props.forwardFilters == true) "?" + filters.urlSearchParams() else "",
                            options = jso {
                                state = NavFromApp
                            }
                        )
                    }
                    ListItemIcon {
                        sx {
                            justifyContent = JustifyContent.center
                        }
                        img {
                            src = "games/${game.id}/thumbnail.${game.thumbnailExt}"
                            css {
                                maxWidth = 3.em
                                maxHeight = 3.em
                            }
                        }
                    }
                    ListItemText {
                        Typography {
                            variant = TypographyVariant.h6
                            +game.name(langId)
                        }
                        Typography {
                            sx {
                                opacity = number(0.6)
                            }
                            variant = TypographyVariant.body1
                            +lang.X_players.invoke(game.playerCount.toShortStrings().joinToString())
                        }
                    }
                }
            }
        }
    }
}
