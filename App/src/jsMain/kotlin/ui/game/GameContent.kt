package ui.game

import data.*
import emotion.react.css
import js.array.asList
import js.intl.NumberFormat
import js.objects.jso
import kotlinx.coroutines.await
import module.nosleep.NoSleep
import module.react.image_gallery.ImageGallery
import module.react.image_gallery.ImageGalleryProps
import mui.icons.material.*
import mui.material.*
import mui.material.Menu
import mui.material.Tab
import mui.material.styles.Theme
import mui.material.styles.TypographyVariant
import mui.material.styles.useTheme
import mui.system.sx
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.img
import react.router.useParams
import ui.*
import ui.lists.PutListDialog
import ui.utils.useCoroutineScope
import ui.utils.useFlow
import ui.utils.useGamesCombineSearchParams
import ui.utils.useSuspend
import utils.launchUndispatched
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.cssom.Display.Companion.block
import web.cssom.atrule.minWidth
import web.dom.Element
import web.dom.document
import web.dom.fullscreenChange
import web.events.Event
import web.events.EventHandler
import web.events.addEventHandler
import web.html.*
import web.scroll.ScrollBehavior
import web.window.WindowTarget
import web.window.resize
import web.window.window


val GameContent = FC("GameContent") {
    val params = useParams()
    val allGames = useGames()
    val gameId = params["id"] ?: error("No game ID")
    val game = allGames?.get(gameId)

    Box {
        key = gameId
        sx {
            gridArea = ContentArea.Content
            width = 100.pct
            backgroundColor = Color("grey.light")
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
            media(minWidth(600.px)) {
                padding = Padding(16.px, 8.px)
            }
        }

        if (allGames == null) {
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

        if (game == null) {
            Paper {
                sx {
                    padding = Padding(8.px, 16.px)
                    marginTop = 32.px
                }
                Typography {
                    variant = TypographyVariant.h6
                    sx {
                        color = Color("error.main")
                    }
                    +"GAME NOT FOUND"
                }
            }
            return@Box
        }

        FoundGameContent {
            this.game = game
        }
    }
}

private external interface FoundGameContentProps : Props {
    var game: Game
}

private var lastDownload: Pair<String, ReactNode?> = "" to null

private val FoundGameContent = FC<FoundGameContentProps>("Game") { props ->
    val (params, setParams) = useGamesCombineSearchParams()
    val page = params["page"] ?: "rules"

    Paper {
        sx {
            width = 100.pct
            media(minWidth(600.px)) {
                width = 100.pct - 16.px
            }
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
        }

        Box {
            sx {
                borderBottom = Border(1.px, web.cssom.LineStyle.solid)
                borderColor = Color("divider")
                width = 100.pct
                marginBottom = 8.px
            }

            if (props.game.references.isNotEmpty()) {
                Tabs {
                    centered = true
                    value = params["page"] ?: "rules"
                    onChange = { _, value ->
                        setParams(
                            { params ->
                                params.apply {
                                    when (value) {
                                        "rules" -> delete("page")
                                        "references" -> set("page", "references")
                                    }
                                }
                            },
                            jso {
                                replace = true
                                state = NavFromApp
                            }
                        )
                    }

                    Tab {
                        label = ReactNode("RÈGLES")
                        value = "rules"
                        sx { flexGrow = number(1.0) }
                    }
                    Tab {
                        label = ReactNode("RÉFÉRENCES")
                        value = "references"
                        sx { flexGrow = number(1.0) }
                    }
                }
            }
        }

        when (page) {
            "rules" -> {
                GameContentRules {
                    +props
                }
            }
            "references" -> {
                GameContentReferences {
                    +props
                }
            }
        }
    }
}

private val GameContentRules = FC<FoundGameContentProps> { props ->
    val (langId, lang) = useLang()
    val isMobile = useIsMobile()
    val onGameFilter = useOnGameFilter()
    var adoc: ReactNode? by useState(run {
        val (lastGameId, lastAdoc) = lastDownload
        if (lastGameId == props.game.id) lastAdoc
        else null
    })
    val theme = useTheme<Theme>()
    val db = useSuspend { AppDatabase.get() }
    var packDialogShown by useState(false)

    useEffect(props.game.id, langId) {
        val path = "games/${props.game.id}/${langId}.html"
        if (lastDownload.first == path) return@useEffect
        adoc = null
        val html = try {
            kotlinx.browser.window.fetch(path).await().text().await()
        } catch (e: Throwable) {
            console.error(e)
            "<p style=\"font-weight: bold; color: ${theme.palette.error.main};\">${lang.Error_loading_game}</p>"
        }
        val newAdoc = Adoc.create {
            this.gameId = props.game.id
            this.gameName = props.game.name(langId)
            this.html = html
            this.openPackDialog = { packDialogShown = true }
        }
        adoc = newAdoc
        lastDownload = props.game.id to newAdoc
    }

    Box {
        sx {
            display = Display.flex
            flexDirection = FlexDirection.column
            flexWrap = FlexWrap.wrap
            alignItems = AlignItems.center
        }

        Box {
            sx {
                display = Display.flex
                flexDirection = FlexDirection.row
                flexWrap = FlexWrap.wrap
                justifyContent = JustifyContent.center
                alignItems = AlignItems.center
                padding = Padding(8.px, 16.px)
            }

            img {
                src = "games/${props.game.id}/image.${props.game.imageExt}"
                css {
                    width = 360.px
                    maxWidth = 90.pct
                    aspectRatio = number(1.0)
                    objectFit = ObjectFit.contain
                }
            }

            Box {
                sx {
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    alignItems = AlignItems.center
                    padding = Padding(8.px, 16.px)
                    paddingTop = 32.px
                    flexGrow = number(1.0)
                }

                Typography {
                    variant = TypographyVariant.h3
                    sx {
                        color = Color("primary.main")
                        textAlign = TextAlign.center
                    }
                    +props.game.name(langId)
                }

                Typography {
                    operator fun List<String>.unaryPlus() = forEachIndexed { i, person ->
                        if (i != 0) {
                            if (i == lastIndex) +" & "
                            else +", "
                        }
                        b { +person }
                    }

                    variant = TypographyVariant.body1
                    sx {
                        color = Color("primary.dark")
                        textAlign = TextAlign.center
                        fontSize = 1.0625.rem
                    }
                    props.game.designers.authors?.let { authors ->
                        +lang.game__A_game_by
                        +" "
                        +authors
                    }
                    props.game.designers.adaptedBy?.let { adapters ->
                        +", "
                        +lang.game__adapted_by
                        +" "
                        +adapters
                    }
                    props.game.designers.tradition?.let { tradition ->
                        +lang.game__A_traditional_game(lang.traditions[tradition] ?: tradition)
                    }
                    +"."
                }

                ButtonBase {
                    component = ReactHTML.div
                    onClick = { window.open("https://boardgamegeek.com/boardgame/${props.game.bgg.bggId}", WindowTarget._blank) }
                    sx {
                        display = Display.flex
                        flexDirection = FlexDirection.row
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
//                        border = Border(2.px, web.cssom.LineStyle.solid, Color("#FF5100"))
                        backgroundColor = Color("#3f3a60")
                        color = NamedColor.white
                        borderRadius = 16.px
                        padding = Padding(4.px, 12.px)
                        margin = 4.px
                        marginTop = 8.px
                    }

                    img {
                        src = "bgg.svg"
                        css {
                            height = 1.6.em
                            marginRight = 0.5.em
                        }
                    }

                    +props.game.bgg.yearPublished.toString()

                    ReactHTML.span { css { marginLeft = 1.em } }

                    EscalatorWarning()
                    +(props.game.bgg.minAge.toString() + "+")

                    ReactHTML.span { css { marginLeft = 1.em } }

                    Timer()
                    +props.game.bgg.playingTime.toString()

                    ReactHTML.span { css { marginLeft = 1.em } }

                    Grade()
                    +(NumberFormat(langId, jso {
                        asDynamic().maximumFractionDigits = 1
                        asDynamic().minimumFractionDigits = 1
                    }).format(props.game.bgg.rating))
                }


                Box {
                    sx {
                        display = Display.flex
                        flexDirection = FlexDirection.row
                        flexWrap = FlexWrap.wrap
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
                        paddingTop = 16.px
                    }

                    props.game.types.forEach { type ->
                        Chip {
                            sx {
                                margin = 4.px
                            }
                            label = ReactNode(lang.gameTypes[type] ?: type)
                            onClick = {
                                onGameFilter(Filters(types = listOf(type)))
                            }
                        }
                    }
                }
                if (db != null) {
                    UserLists {
                        game = props.game
                        this.db = db
                    }
                }
            }
        }

        if (adoc == null) {
            CircularProgress {
                size = 4.em
                color = CircularProgressColor.primary
                sx {
                    margin = Margin(4.em, auto)
                    display = block
                }
            }
        } else {
            +adoc
        }
    }

    Dialog {
        open = packDialogShown
        onClose = { _, _ -> packDialogShown = false }
        fullWidth = true
        fullScreen = isMobile
        sx {
            zIndex = integer(2000)
        }

        IconButton {
            onClick = { packDialogShown = false }
            sx {
                position = Position.absolute
                top = 8.px
                right = 8.px
            }
            Close()
        }

        GamePackDialogContent {
            game = props.game
            key = props.game.id
        }
    }

}

private external interface UserListsProps : Props {
    var game: Game
    var db: AppDatabase
}

private val UserLists = FC<UserListsProps> { props ->
    val (_, lang) = useLang()
    val scope = useCoroutineScope()
    val gameLists = useFlow { props.db.watchAllUserListsOfGame(props.game.id) }
    val onGameFilter = useOnGameFilter()

    Box {
        sx {
            display = Display.flex
            flexDirection = FlexDirection.row
            flexWrap = FlexWrap.wrap
            justifyContent = JustifyContent.center
            alignItems = AlignItems.center
            paddingTop = 8.px
        }

        gameLists?.forEach { list ->
            Chip {
                color = ChipColor.secondary
                sx {
                    margin = 4.px
                }
                label = ReactNode(if (list.id == AppDatabase.favourites.id) lang.lists__favourites else list.name)
                onClick = {
                    onGameFilter(Filters(userList = list))
                }
                onDelete = {
                    scope.launchUndispatched {
                        props.db.removeGameFromUserList(list.id, props.game.id)
                    }
                }
            }
        }
        AddToListMenu {
            +props
        }
    }
}

private val AddToListMenu = FC<UserListsProps> { props ->
    val (_, lang) = useLang()
    var menuAnchor: Element? by useState()
    val scope = useCoroutineScope()
    val lists = useFlow { props.db.watchAllUserLists() }
    var newListDialogShown by useState(false)

    Chip {
        color = ChipColor.secondary
        variant = ChipVariant.outlined
        sx {
            margin = 4.px
        }
        icon = Add.create()
        label = ReactNode(lang.lists__Add_to_list)
        onClick = { menuAnchor = it.currentTarget }
    }

    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        lists?.forEach { list ->
            MenuItem {
                onClick = {
                    scope.launchUndispatched {
                        props.db.addGameToUserList(list.id, props.game.id)
                    }
                    menuAnchor = null
                }
                ListItemText {
                    if (list.id == AppDatabase.favourites.id) +lang.lists__favourites
                    else +list.name
                }
            }
        }
        Divider()
        MenuItem {
            onClick = {
                newListDialogShown = true
            }
            ListItemText {
                +lang.lists__New_list
            }
        }
    }

    PutListDialog {
        db = props.db
        open = newListDialogShown
        onClose = { newListDialogShown = false }
        onPut = {
            scope.launchUndispatched {
                props.db.addGameToUserList(it.id, props.game.id)
            }
            menuAnchor = null
        }
    }
}

private external interface AdocProps : Props {
    var gameId: String
    var gameName: String
    var html: String
    var openPackDialog: () -> Unit
}

private var lastScroll = ""

private val Adoc = FC<AdocProps>("Adoc") { props ->
    val boxRef = useRef<HTMLDivElement>()
    val theme = useTheme<Theme>()
    val (params, setParams) = useGamesCombineSearchParams()

    val section = params.get("section")

    var luggageOffset: Pair<Int, Int>? by useState()

    useLayoutEffect {
        val div = boxRef.current!!
//        div.innerHTML = props.html

        div.getElementsByTagName("a").asList().forEach { link ->
            link as HTMLAnchorElement
            link.attributes.getNamedItem("href")?.value?.let { href ->
                when {
                    href.startsWith("#") -> {
                        link.removeAttribute("href")
                        link.onclick = EventHandler {
                            setParams(
                                { params ->
                                    params.apply { set("section", href.removePrefix("#")) }
                                },
                                jso {
                                    replace = true
                                    preventScrollReset = true
                                }
                            )
                        }
                    }
                    !href.startsWith("/") && "://" !in href -> link.href = "games/${props.gameId}/${href}"
                }
            }
        }

        div.querySelectorAll("h2, h3, h4").asList().forEach { heading ->
            heading as HTMLHeadingElement
            if (heading.id.isNotEmpty()) {
                heading.style.cursor = "pointer"
                heading.onclick = EventHandler {
                    setParams(
                        {
                            it.apply { set("section", heading.id) }
                        },
                        jso {
                            replace = true
                            preventScrollReset = true
                        }
                    )
                }
            }
        }

        div.querySelector(".ssd-components .title")?.let { element ->
            val span = (element.querySelector("span.ssd-luggage") as? HTMLSpanElement)
                ?: document.createElement(HTML.span).let {
                    it.style.display = "inline-block"
                    it.style.width = "40px"
                    it.style.height = "40px"
                    it.style.marginBottom = "-12px"
                    it.style.marginLeft = "12px"
                    it.className = "ssd-luggage"
                    element.appendChild(it)
                }
//            luggageOffset = span.offsetTop to span.offsetLeft
        }
    }

    useEffectOnceWithCleanup {
        fun updateLuggageOffset() {
            boxRef.current!!.querySelector(".ssd-components .title span.ssd-luggage")?.let { span ->
                span as HTMLSpanElement
                luggageOffset = span.offsetTop to span.offsetLeft
            }
        }
        updateLuggageOffset()
        val close = window.addEventHandler(Event.resize()) { updateLuggageOffset() }
        onCleanup(close)
    }

    useEffect(section) {
        if (section == null) return@useEffect
        if (section == lastScroll) return@useEffect
        val div = boxRef.current!!
        val element = div.querySelector("#$section")
        if (element != null) {
            window.scrollTo(jso {
                top = window.scrollY + element.getBoundingClientRect().top - 80
                behavior = ScrollBehavior.smooth
            })
            lastScroll = section
        }
    }

    luggageOffset?.let { (offsetTop, offsetLeft) ->
        IconButton {
            color = IconButtonColor.secondary
            onClick = { props.openPackDialog() }
            sx {
                position = Position.absolute
                top = offsetTop.px
                left = offsetLeft.px
            }
            Luggage()
        }
    }

    Box {
        sx {
            set(CustomPropertyName("--theme-primary"), theme.palette.primary.main)
            set(CustomPropertyName("--theme-secondary"), theme.palette.secondary.main)
        }
        className = ClassName("adoc adoc-fragment")
        ref = boxRef
        dangerouslySetInnerHTML = jso {
            __html = props.html
        }
    }
}

private val GameContentReferences = FC<FoundGameContentProps> { props ->
    var showGalleryIndex: Int? by useState(null)
    val noSleep = useMemo { NoSleep() }

    Box {
        sx {
            display = Display.flex
            flexDirection = FlexDirection.row
            flexWrap = FlexWrap.wrap
            justifyContent = JustifyContent.center
            alignItems = AlignItems.center
            position = Position.relative
        }

        val references = props.game.references.sorted()
        references.forEachIndexed { index, ref ->
            img {
                src = "games/${props.game.id}/R-$ref.png"
                css {
                    width = 200.px
                    margin = 16.px
                    cursor = Cursor.pointer
                }
                onClick = {
                    showGalleryIndex = index
                    noSleep.enable()
                }
            }
        }

        if (showGalleryIndex != null) {
            GameContentReferencesGallery {
                gameId = props.game.id
                this.references = references
                startIndex = showGalleryIndex!!
                onClose = {
                    showGalleryIndex = null
                    noSleep.disable()
                }
            }
        }
    }

}

private external interface GameContentReferencesGalleryProps : Props {
    var gameId: String
    var references: List<String>
    var startIndex: Int
    var onClose: () -> Unit
}

private val GameContentReferencesGallery = FC<GameContentReferencesGalleryProps> { props ->
    val rootBox = useRef<HTMLDivElement>()

    useEffectWithCleanup {
        if (document.fullscreenEnabled) {
            if (document.fullscreenElement == null) {
                rootBox.current!!.requestFullscreenAsync()
            }
            val unregister = rootBox.current!!.addEventHandler(Event.fullscreenChange()) {
                if (document.fullscreenElement == null) {
                    props.onClose()
                }
            }
            onCleanup(unregister)
        }
    }

    val isLandscape = useIsLandscape()

    var isDouble by useState(false)

    useEffect(isLandscape) {
        if (!isLandscape) {
            isDouble = false
        }
    }

    Box {
        ref = rootBox
        sx {
            position = Position.fixed
            top = 0.px
            left = 0.px
            width = 100.vw
            height = 100.vh
            zIndex = integer(10_000)
            backgroundColor = NamedColor.black
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = AlignItems.center
            justifyContent = JustifyContent.center

            ".image-gallery" {
                flexGrow = number(1.0)
            }

            if (isDouble) {
                ".image-gallery-thumbnails-wrapper" {
                    opacity = number(0.4)
                    hover {
                        opacity = number(1.0)
                        transition = 0.3.s
                    }
                }
            }
        }

        ImageGallery {
            items = props.references.map { r ->
                jso<ImageGalleryProps.Image> {
                    original = "games/${props.gameId}/R-$r.png"
                    thumbnail = "games/${props.gameId}/R-$r.png"
                }
            }.toTypedArray()
            showPlayButton = false
            showNav = isLandscape && !isDouble
            startIndex = props.startIndex
            if (isLandscape) {
                thumbnailPosition = "left"
            }
            showThumbnails = props.references.size >= 2 //&& !isDouble
            renderFullscreenButton = { toggle, _ ->
                IconButton.create {
                    onClick = {
                        if (document.fullscreenEnabled) {
                            document.exitFullscreenAsync()
                        }
                        props.onClose()
                    }
                    sx {
                        position = Position.absolute
                        if (isLandscape) {
                            bottom = 24.px
                            if (isDouble) {
                                right = (-32).px
                            } else {
                                right = 48.px
                            }
                        } else {
                            bottom = (-48).px
                            right = 0.px
                        }
                        zIndex = integer(1)
                        padding = 20.px
                        color = Color("primary.light")
                        rotate = 90.deg.unsafeCast<Rotate>()
                        transformOrigin = GeometryPosition.center
                    }

                    FullscreenExit {
                        sx {
                            width = 48.px
                            height = 48.px
                        }
                    }
                }
            }
            renderCustomControls = {
                if (isLandscape && props.references.size >= 2) {
                    IconButton.create {
                        onClick = {
                            isDouble = !isDouble
                        }
                        sx {
                            position = Position.absolute
                            top = 24.px
                            if (isDouble) {
                                right = (-32).px
                            } else {
                                right = 48.px
                            }
                            zIndex = integer(1)
                            padding = 20.px
                            color = Color("primary.light")
                        }

                        if (isDouble) {
                            CropPortrait {
                                sx {
                                    width = 32.px
                                    height = 32.px
                                }
                            }
                        } else {
                            Splitscreen {
                                sx {
                                    width = 32.px
                                    height = 32.px
                                    rotate = 90.deg.unsafeCast<Rotate>()
                                    transformOrigin = GeometryPosition.center
                                }
                            }
                        }
                    }
                } else ReactNode(emptyArray())
            }
        }

        if (isDouble) {
            ImageGallery {
                items = props.references.map { r ->
                    jso<ImageGalleryProps.Image> {
                        original = "games/${props.gameId}/R-$r.png"
                        thumbnail = "games/${props.gameId}/R-$r.png"
                    }
                }.toTypedArray()
                showPlayButton = false
                showNav = false
                thumbnailPosition = "right"
                showThumbnails = true
                showFullscreenButton = false
            }
        }
    }
}
