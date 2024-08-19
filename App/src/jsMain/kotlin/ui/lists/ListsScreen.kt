package ui.lists

import data.*
import kotlinx.coroutines.flow.map
import mui.icons.material.Add
import mui.icons.material.Delete
import mui.icons.material.Edit
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.router.useNavigate
import ui.*
import ui.utils.useCoroutineScope
import ui.utils.useFlow
import ui.utils.useSuspend
import utils.launchUndispatched
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.cssom.None.Companion.none
import web.cssom.atrule.maxWidth
import web.cssom.atrule.minWidth


val ListsScreen = FC("UserScreen") {
    val (_, lang) = useLang()

    val db = useSuspend { AppDatabase.getResult() }

    ContentMobile {
        mainId = ContentArea.List

        SSDAppBar {
            navigationIcon = OpenAppDrawerIcon.create()

            SSDAppBarTitle {
                label = lang.title_MyLists
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

            SyncMenu()

            when {
                db == null -> {
                    AppCircularProgress()
                }
                db.isFailure -> {
                    Typography {
                        variant = TypographyVariant.h6
                        sx {
                            color = Color("error.main")
                            textAlign = TextAlign.center
                            padding = Padding(16.px, 8.px)
                        }

                        +lang.Error_opening_database(db.exceptionOrNull()?.message ?: "Unknown error")
                    }
                    return@Box
                }
                else -> {
                    UserListsList {
                        this.db = db.getOrThrow()
                    }
                }
            }
        }
    }
}

private external interface UserListsListProps : Props {
    var db: AppDatabase
}

private val UserListsList = FC<UserListsListProps> { props ->
    var dialogShown by useState(false)
    var listToEdit: UserList? by useState()
    val lists = useFlow { props.db.watchAllUserLists().map { it.sortedBy(UserList::name) } }

    if (lists == null) {
        AppCircularProgress()
        return@FC
    }

    PutListDialog {
        open = dialogShown
        onClose = { dialogShown = false }
        list = listToEdit
        db = props.db
    }

    Paper {
        sx {
            width = 100.pct
            maxWidth = 500.px
        }

        List {
            lists.forEach { list ->
                UserListItem {
                    this.key = list.id.id
                    this.list = list
                    this.onEditList = {
                        listToEdit = list
                        dialogShown = true
                    }
                    this.db = props.db
                }
            }
        }
    }

    Box {
        sx {
            width = 100.pct
            maxWidth = 500.px
            height = 100.pct
            position = Position.fixed
            top = 0.pc
            left = auto
            right = auto
            pointerEvents = none
        }
        Fab {
            onClick = {
                listToEdit = null
                dialogShown = true
            }
            color = FabColor.secondary
            sx {
                position = Position.absolute
                bottom = 16.px
                right = 16.px
                pointerEvents = auto
            }

            Add()
        }
    }
}

private external interface UserListItemProps : Props {
    var list: UserList
    var onEditList: () -> Unit
    var db: AppDatabase
}

private val UserListItem = FC<UserListItemProps> { props ->
    val (_, lang) = useLang()
    val navigate = useNavigate()
    val scope = useCoroutineScope()
    val gameCount = useFlow { props.db.watchGameCountInUserList(props.list.id) }
    var deleteList: UserList? by useState()
    var deleteListDialogShown: Boolean by useState(false)

    ListItem {
        disablePadding = true

        if (props.list.id != AppDatabase.favourites.id) {
            secondaryAction = Box.create {
                sx {
                    display = Display.flex
                    flexDirection = FlexDirection.row
                }
                IconButton {
                    onClick = {
                        props.onEditList()
                    }
                    Edit()
                }
                IconButton {
                    onClick = {
                        if (gameCount == 0) {
                            scope.launchUndispatched {
                                props.db.deleteUserList(props.list.id)
                            }
                        } else {
                            deleteList = props.list
                            deleteListDialogShown = true
                        }
                    }
                    edge = IconButtonEdge.end
                    Delete()
                }
            }
        }

        ListItemButton {
            role = null

            onClick = {
                navigate("/games?list=${props.list.id.id}.${props.list.name}")
            }

            ListItemText {
                primary = ReactNode(
                    if (props.list.id == AppDatabase.favourites.id) lang.lists__favourites
                    else props.list.name
                )
                if (gameCount != null) {
                    secondary = ReactNode(lang.X_games(gameCount))
                }
            }
        }
    }

    DeleteListDialog {
        open = deleteListDialogShown
        onClose = { deleteListDialogShown = false }
        list = deleteList
        db = props.db
    }
}

private external interface DeleteListDialogProps : Props {
    var open: Boolean
    var onClose: () -> Unit
    var list: UserList?
    var db: AppDatabase
}

private val DeleteListDialog = FC<DeleteListDialogProps> { props ->
    val (langId, lang) = useLang()
    val scope = useCoroutineScope()
    val allGames = useGames()
    val listGameIds = useFlow(props.list) { props.list?.let { props.db.watchAllGamesInUserList(it.id) } }

    Dialog {
        open = props.open
        onClose = { _, _ -> props.onClose() }

        DialogTitle {
            +lang.deleteListDialog__delete_list(props.list?.name ?: "")
        }
        DialogContent {
            if (listGameIds != null) {
                +listGameIds.joinToString { gameId ->
                    allGames?.get(gameId)?.name(langId) ?: gameId
                }
            }
        }
        DialogActions {
            Button {
                onClick = { props.onClose() }
                +lang.dialog__cancel
            }
            Button {
                onClick = {
                    scope.launchUndispatched {
                        props.db.deleteUserList(props.list!!.id)
                    }
                    props.onClose()
                }
                autoFocus = true
                +lang.deleteListDialog__delete
            }
        }
    }
}

private val SyncMenu = FC {
    val (_, lang) = useLang()
    var syncDialogShown: Boolean by useState(false)
    val db = useSuspend { AppDatabase.get() }
    val isMobile = useMediaQuery(maxWidth(700.px))

    if (db == null) return@FC

    Button {
        sx {
            marginBottom = 16.px
        }
        variant = ButtonVariant.contained
        color = ButtonColor.secondary
        onClick = {
            syncDialogShown = true
        }
        +lang.lists__Backup_Restore
    }

    Dialog {
        open = syncDialogShown
        fullWidth = true
        fullScreen = isMobile

        sx {
            zIndex = integer(2000)
        }

        SyncScreen {
            this.db = db
            this.onClose = { syncDialogShown = false }
        }
    }
}
