package ui.game

import data.*
import ui.ContentSize
import js.objects.jso
import mui.icons.material.Clear
import mui.icons.material.Search
import mui.icons.material.Sort
import mui.material.*
import mui.system.sx
import react.*
import ui.useLang
import ui.utils.toShortStrings
import ui.utils.useFlow
import ui.utils.useGamesCombineSearchParams
import web.cssom.*
import web.dom.Element
import web.html.HTMLInputElement
import web.url.URLSearchParams


data class Filters(
    val types: List<String> = emptyList(),
    val userList: UserList? = null,
    val playerCounts: List<Int> = emptyList(),
    val search: String = "",
    val sort: SortOrder = SortOrder.Name
)

typealias SetFilters = (Filters.() -> Filters) -> Unit

fun URLSearchParams.applyFilters(filters: Filters): URLSearchParams {
    if (filters.types.isNotEmpty()) set("types", filters.types.joinToString(",")) else delete("types")
    if (filters.userList != null) set("list", "${filters.userList.id.id}.${filters.userList.name}") else delete("list")
    if (filters.playerCounts.isNotEmpty()) set("playerCounts", filters.playerCounts.joinToString(",")) else delete("playerCounts")
    if (filters.search.isNotBlank()) set("search", filters.search) else delete("search")
    if (filters.sort != SortOrder.Name) set("sort", filters.sort.name.lowercase()) else delete("sort")
    return this
}

fun Filters.urlSearchParams(): String = URLSearchParams().applyFilters(this).toString()

fun useFilters(): Pair<Filters, SetFilters> {
    fun URLSearchParams.toFilters() = Filters(
        types = this["types"]?.split(",") ?: emptyList(),
        userList = this["list"]?.let {
            val split = it.split(".")
            if (split.size < 2) null
            else UserList(UserList.ID(split[0]), split[1])
        },
        playerCounts = this["playerCounts"]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
        search = this["search"] ?: "",
        sort = this["sort"]?.let { name -> SortOrder.entries.firstOrNull { it.name.lowercase() == name.lowercase() } } ?: SortOrder.Name
    )

    val (params, setParams) = useGamesCombineSearchParams()

    return params.toFilters() to { transformFilters ->
        setParams(
            { params ->
                val filters = params.toFilters().transformFilters()
                params.applyFilters(filters)
            },
            jso {
                replace = true
                preventScrollReset = true
            }
        )
    }
}

external interface GamesListFiltersProps : Props {
    var sticky: Boolean?
    var filteredGames: List<Game>
}

val GamesFilters = FC<GamesListFiltersProps> { props ->
    val (_, lang) = useLang()
    val allGames = useGames()!!

    val allPlayerCounts = useMemo(allGames) {
        allGames.flatMapTo(HashSet()) { it.playerCount }.sorted()
    }

    val allTypes = useMemo(allGames) {
        allGames.flatMapTo(HashSet()) { it.types }.sorted()
    }

    val (filters, setFilters) = useFilters()

    Box {
        sx {
            width = 100.pct
            height = ContentSize.Filters.height
            if (props.sticky == true) {
                position = Position.fixed
            }
            backgroundColor = Color("background.default")
            zIndex = integer(1)
            borderBottom = Border(2.px, LineStyle.solid)
            borderBottomColor = Color("primary.main")
            display = Display.flex
            flexDirection = FlexDirection.row
            justifyContent = JustifyContent.center
        }
        Box {
            sx {
                width = 100.pct
                maxWidth = ContentSize.List.Width
                display = Display.flex
                flexDirection = FlexDirection.column
                alignItems = AlignItems.center
            }
            Toolbar {
                sx {
                    marginTop = 8.px
                    width = 100.pct
                }
                SortMenu()
                Input {
                    onChange = {
                        val newSearch = (it.target as HTMLInputElement).value
                        setFilters { copy(search = newSearch) }
                    }
                    value = filters.search
                    sx {
                        margin = Margin(0.px, 8.px)
                        flexGrow = number(1.0)
                    }
                    startAdornment = InputAdornment.create {
                        position = InputAdornmentPosition.start
                        Search()
                    }
                    if (filters.search.isNotEmpty()) {
                        endAdornment = InputAdornment.create {
                            position = InputAdornmentPosition.start
                            IconButton {
                                onClick = {
                                    setFilters { copy(search = "") }
                                }
                                Clear()
                            }
                        }
                    }
                    placeholder = lang.filter__searchPlaceholder
                }
            }
            Box {
                sx {
                    display = Display.flex
                    flexDirection = FlexDirection.row
                    alignItems = AlignItems.center
                    justifyContent = JustifyContent.center
                    flexWrap = FlexWrap.wrap
                }

                PlayerCountsMenu {
                    this.availablePlayerCounts = props.filteredGames.filter(
                        types = filters.types,
                        playerCounts = filters.playerCounts,
                        search = filters.search,
                    ).flatMapTo(HashSet()) { it.playerCount }.sorted()
                    this.allPlayerCounts = allPlayerCounts
                }

                Box {
                    sx { width = 16.px }
                }

                TypesMenu {
                    this.availableTypes = props.filteredGames.filter(
                        types = filters.types,
                        playerCounts = filters.playerCounts,
                        search = filters.search,
                    ).flatMapTo(HashSet()) { it.types }.sorted()
                    this.allTypes = allTypes
                }
            }
        }
    }
}

private val SortMenu = FC("SortMenu") {
    val (_, lang) = useLang()
    var menuAnchor by useState<Element?>(null)

    val (filters, setFilters) = useFilters()

    IconButton {
        onClick = { menuAnchor = it.currentTarget }
        Sort()
    }
    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        SortOrder.entries.forEach { order ->
            MenuItem {
                onClick = {
                    menuAnchor = null
                    setFilters { copy(sort = order) }
                }
                ListItemText {
                    primaryTypographyProps = jso {
                        if (filters.sort == order) {
                            sx { fontWeight = FontWeight.bold }
                        }
                    }
                    +order.text(lang)
                }
            }
        }
    }
}

private external interface PlayerCountsMenuProps : Props {
    var allPlayerCounts: List<Int>
    var availablePlayerCounts: List<Int>
}

private val PlayerCountsMenu = FC<PlayerCountsMenuProps>("PlayersMenu") { props ->
    val (_, lang) = useLang()
    var menuAnchor by useState<Element?>(null)
    val (filters, setFilters) = useFilters()

    Chip {
        label = ReactNode(
            if (filters.playerCounts.isNotEmpty()) {
                lang.X_players(filters.playerCounts.toShortStrings().joinToString())
            } else {
                lang.X_players(
                    if (props.availablePlayerCounts.isNotEmpty()) props.availablePlayerCounts.toShortStrings().joinToString() else "0"
                )
            }
        )
        onClick = { menuAnchor = it.currentTarget }
        if (filters.playerCounts.isNotEmpty()) {
            onDelete = {
                setFilters { copy(playerCounts = emptyList()) }
            }
        }
    }

    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        props.allPlayerCounts.forEach { pc ->
            MenuItem {
                if (pc !in props.availablePlayerCounts) {
                    disabled = true
                }

                onClick = {
                    setFilters {
                        if (pc in playerCounts) copy(playerCounts = playerCounts - pc)
                        else copy(playerCounts = (playerCounts + pc).sorted())
                    }
                }
                ListItemText {
                    primaryTypographyProps = jso {
                        if (pc in filters.playerCounts) {
                            sx { fontWeight = FontWeight.bold }
                        }
                    }
                    +lang.X_players(pc.toString())
                }
            }
        }
    }
}

private external interface TypesMenuProps : Props {
    var allTypes: List<String>
    var availableTypes: List<String>
}

private val TypesMenu = FC<TypesMenuProps>("TypesMenu") { props ->
    val (_, lang) = useLang()
    var menuAnchor by useState<Element?>(null)
    val (filters, setFilters) = useFilters()
    val userLists = useFlow { AppDatabase.get()?.watchAllUserLists() }

    val filteredTypes = listOfNotNull(
        filters.userList?.let {
            if (it.id == AppDatabase.favourites.id) lang.lists__favourites
            else it.name
        }
    ) + filters.types.map { lang.gameTypes[it] ?: it }

    Chip {
        label = ReactNode(
            if (filteredTypes.isNotEmpty()) {
                if (filteredTypes.size == 1) filteredTypes.single()
                else lang.X_types(filteredTypes.size)
            } else {
                if (props.availableTypes.size == 1) lang.gameTypes[props.availableTypes.single()] ?: props.availableTypes.single()
                lang.X_types(props.availableTypes.size)
            }
        )
        onClick = { menuAnchor = it.currentTarget }
        if (filters.userList != null || filters.types.isNotEmpty()) {
            onDelete = {
                setFilters { copy(types = emptyList(), userList = null) }
            }
        }
    }

    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        userLists?.forEach { ul ->
            MenuItem {
                if (filters.userList != null && filters.userList.id != ul.id) {
                    disabled = true
                }
                onClick = {
                    setFilters {
                        if (filters.userList?.id == ul.id) copy(userList = null)
                        else copy(userList = ul)
                    }
                }
                ListItemText {
                    primaryTypographyProps = jso {
                        if (filters.userList?.id == ul.id) {
                            sx { fontWeight = FontWeight.bold }
                        }
                    }
                    if (ul.id == AppDatabase.favourites.id) +lang.lists__favourites
                    else +ul.name
                }
            }
        }
        if (userLists != null) {
            Divider()
        }
        props.allTypes.forEach { t ->
            MenuItem {
                if (t !in props.availableTypes) {
                    disabled = true
                }
                onClick = {
                    setFilters {
                        if (t in types) copy(types = types - t)
                        else copy(types = (types + t).sorted())
                    }
                }
                ListItemText {
                    primaryTypographyProps = jso {
                        if (t in filters.types) {
                            sx { fontWeight = FontWeight.bold }
                        }
                    }
                    +(lang.gameTypes[t] ?: t)
                }
            }
        }
    }
}
