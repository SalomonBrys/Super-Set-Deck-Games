package ui.packer

import data.Game
import data.cards.Pack
import data.get
import data.name
import data.useGames
import js.objects.jso
import js.objects.recordOf
import mui.icons.material.Delete
import mui.icons.material.Edit
import mui.material.*
import mui.system.sx
import react.*
import react.router.dom.useSearchParams
import react.router.useNavigate
import ui.OpenAppDrawerIcon
import ui.SSDAppBar
import ui.SSDAppBarTitle
import ui.useLang
import ui.utils.toShortStrings
import web.cssom.*


private data class PackedGame(
    val game: Game,
    val playerCounts: Set<Int>,
    val variants: Set<String>,
)

val PackerScreen = FC("PackerScreen") {
    val (langId, lang) = useLang()
    val games = useGames()
    val navigate = useNavigate()
    val (params, setParams) = useSearchParams()
    var packedGameToEdit: PackedGame? by useState()
    var dialogShown by useState(false)

    val packedGames = params.getAll("g")
        .mapNotNull {
            val specs = it.split("*")
            val game = games?.get(specs[0]) ?: return@mapNotNull null
            val playerCounts = specs.getOrNull(1)?.split(".")?.mapNotNullTo(HashSet()) { it.toIntOrNull() }?.takeIf { it.isNotEmpty() } ?: game.playerCount.toSet()
            val variants = specs.getOrNull(2)?.let {
                when (it) {
                    "" -> game.cards.keys - "Base"
                    "-" -> emptySet()
                    else -> it.split(".").toSet()
                }
            } ?: (game.cards.keys - "Base")
            PackedGame(game, playerCounts, variants)
        }

    val pack = useMemo(packedGames) {
        Pack().apply {
            packedGames.forEach {
                add(it.game, it.playerCounts, it.variants)
            }
        }
    }

    fun setPackedGames(games: List<PackedGame>) {
        setParams(
            {
                recordOf(
                    "g" to games.sortedBy { it.game.id }.map {
                        buildString {
                            append(it.game.id)
                            append("*")
                            if (it.playerCounts != it.game.playerCount.toSet()) {
                                append(it.playerCounts.joinToString("."))
                            }
                            append("*")
                            if (it.variants != (it.game.cards.keys - "Base")) {
                                if (it.variants.isEmpty()) {
                                    append("-")
                                } else {
                                    append(it.variants.sorted().joinToString("."))
                                }
                            }
                        }
                    }.toTypedArray(),
                )
            },
            jso {
                replace = true
            }
        )
    }

    useEffect {
        if (params.has("all") && games != null) {
            setPackedGames(games.map {
                PackedGame(it, it.playerCount.toSet(), it.cards.keys - "Base")
            })
        }
    }

    SSDAppBar {
        navigationIcon = OpenAppDrawerIcon.create()

        SSDAppBarTitle {
            label = "Travel Packer"
        }
    }

    Box {
        sx {
            display = Display.flex
            width = 100.pct
            flexDirection = FlexDirection.column
            justifyContent = JustifyContent.center
            alignItems = AlignItems.center
            padding = Padding(16.px, 0.px)
        }

        Button {
            variant = ButtonVariant.contained
            color = ButtonColor.secondary
            onClick = {
                packedGameToEdit = null
                dialogShown = true
            }

            +"Add Game to Pack"
        }

        Paper {
            sx {
                width = 100.pct
                maxWidth = 500.px
                marginTop = 16.px
            }

            if (packedGames.isNotEmpty()) {
                List {
                    packedGames.sortedBy { it.game.name(langId) }.forEach { packedGame ->
                        ListItem {
                            disablePadding = true
                            secondaryAction = Box.create {
                                sx {
                                    display = Display.flex
                                    flexDirection = FlexDirection.row
                                }
                                IconButton {
                                    onClick = {
                                        packedGameToEdit = packedGame
                                        dialogShown = true
                                    }
                                    Edit()
                                }
                                IconButton {
                                    onClick = {
                                        setPackedGames(packedGames - packedGame)
                                    }
                                    edge = IconButtonEdge.end
                                    Delete()
                                }
                            }

                            ListItemButton {
                                onClick = {
                                    navigate("/games/${packedGame.game.id}")
                                }

                                ListItemText {
                                    primary = ReactNode(packedGame.game.name(langId))
                                    secondary = ReactNode(buildString {
                                         append(lang.X_players(packedGame.playerCounts.sorted().toShortStrings().joinToString()))
                                        if ((packedGame.game.cards.keys - "Base").isNotEmpty()) {
                                            append(". ")
                                            append(lang.With_X_variants(packedGame.variants.count()))
                                        }
                                        append(".")
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

        Box {
            sx {
                marginTop = 16.px
                width = 100.pct
                maxWidth = 680.px
            }
            PackView {
                this.pack = pack
                this.gamesInTable = true
            }
        }
    }

    Dialog {
        open = dialogShown
        onClose = { _, _ -> dialogShown = false }
        fullWidth = true
        sx {
            zIndex = integer(2000)
        }

        if (packedGameToEdit != null) {
            EditGameDialogContent {
                key = packedGameToEdit!!.game.id
                this.packedGame = packedGameToEdit!!
                editPack = { packedGame ->
                    dialogShown = false
                    setPackedGames(packedGames - packedGameToEdit!! + packedGame)
                }
            }
        } else {
            AddGameDialogContent {
                alreadySelectedGames = packedGames.map { it.game.id }.toSet()
                addGameToPack = { packedGame ->
                    dialogShown = false
                    setPackedGames(packedGames + packedGame)
                }
            }
        }
    }
}

private external interface AddGameDialogContentProps : Props {
    var alreadySelectedGames: Set<String>
    var addGameToPack: (PackedGame) -> Unit
}

private val AddGameDialogContent = FC<AddGameDialogContentProps> { props ->
    val (langId, _) = useLang()
    val games = useGames()
    var selectedGame: Game? by useState()

    var selectedPlayerCounts: Set<Int> by useState(emptySet())
    var selectedVariants: Set<String> by useState(emptySet())

    if (games == null) return@FC

    DialogTitle {
        +"Add Game to Pack"
    }
    DialogContent {
        FormControl {
            margin = FormControlMargin.normal
            fullWidth = true
            InputLabel {
                id = "add-game-label"
                +"Game"
            }
            Select {
                labelId = "add-game-label"
                label = ReactNode("Game")
                value = selectedGame?.id ?: ""
                onChange = { event, _ ->
                    val game = games[event.target.value]
                    if (game != null) {
                        selectedGame = game
                        selectedPlayerCounts = game.playerCount.toSet()
                        selectedVariants = game.cards.keys - "Base"
                    }
                }
                MenuProps = jso {
                    sx {
                        zIndex = integer(2500)
                    }
                }

                games.filterNot { it.id in props.alreadySelectedGames }.sortedBy { it.name(langId) }.forEach {
                    MenuItem {
                        value = it.id
                        +it.name(langId)
                    }
                }
            }
        }

        if (selectedGame != null) {
            PackGameConfiguration {
                this.game = selectedGame!!
                this.selectedPlayerCounts = selectedPlayerCounts
                this.setSelectedPlayerCounts = { selectedPlayerCounts = it }
                this.selectedVariants = selectedVariants
                this.setSelectedVariants = { selectedVariants = it }
            }
        }
    }
    DialogActions {
        Button {
            color = ButtonColor.secondary
            onClick = { props.addGameToPack(PackedGame(selectedGame!!, selectedPlayerCounts, selectedVariants)) }

            +"Add to Pack"
        }
    }
}

private external interface EditGameDialogContentProps : Props {
    var packedGame: PackedGame
    var editPack: (PackedGame) -> Unit
}

private val EditGameDialogContent = FC<EditGameDialogContentProps> { props ->
    val (langId, _) = useLang()
    var selectedPlayerCounts: Set<Int> by useState(props.packedGame.playerCounts)
    var selectedVariants: Set<String> by useState(props.packedGame.variants)

    DialogTitle {
        +props.packedGame.game.name(langId)
    }

    DialogContent {
        PackGameConfiguration {
            this.game = props.packedGame.game
            this.selectedPlayerCounts = selectedPlayerCounts
            this.setSelectedPlayerCounts = { selectedPlayerCounts = it }
            this.selectedVariants = selectedVariants
            this.setSelectedVariants = { selectedVariants = it }
        }
    }
    DialogActions {
        Button {
            color = ButtonColor.secondary
            onClick = { props.editPack(PackedGame(props.packedGame.game, selectedPlayerCounts, selectedVariants)) }

            +"Edit"
        }
    }

}