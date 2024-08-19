package ui.lists

import data.AppDatabase
import data.SsdBak
import data.UserList
import js.date.Date
import js.intl.DateStyle
import js.intl.DateTimeFormat
import js.intl.TimeStyle
import js.objects.jso
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mui.icons.material.ArrowCircleLeft
import mui.icons.material.ArrowCircleRight
import mui.icons.material.Close
import mui.icons.material.PauseCircle
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.FC
import react.Props
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.span
import react.useState
import ui.useLang
import ui.utils.useCoroutineScope
import utils.launchUndispatched
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.dom.Element
import kotlin.math.max


external interface SyncScreenProps : Props {
    var db: AppDatabase
    var onClose: () -> Unit
}

val SyncScreen = FC<SyncScreenProps> { props ->
    val (_, lang) = useLang()

    Box {
        sx {
            display = Display.flex
            flexDirection = FlexDirection.column
            width = 100.pct
        }

        Toolbar {
            Typography {
                variant = TypographyVariant.h6
                sx {
                    flexGrow = number(1.0)
                }

                +lang.lists__Backup_Restore
            }

            IconButton {
                edge = IconButtonEdge.end
                ariaLabel = "close"
                onClick = { props.onClose() }

                Close()
            }
        }

        SyncPilot()
    }
}

external interface FileProviderInitProps : Props {
    var synchronize: (FileProvider) -> Unit
}

external interface FileProviderSyncProps : Props {
    var restartConfig: (suspend () -> Unit) -> Unit
}

interface FileProvider {
    suspend fun getContent(): String
    val syncComponent: FC<FileProviderSyncProps>
    suspend fun prepareWrite(): Writeable?
    fun interface Writeable {
        suspend fun write(content: String)
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

private sealed interface SyncScreenStep {
    data object SelectProvider : SyncScreenStep
    data object Loading : SyncScreenStep
    data class ConfigureSynchronization(val provider: FileProvider, val bak: SsdBak, val current: SsdBak, val config: Map<UserList.ID, Sync>? = null) : SyncScreenStep
    data class Error(val error: String) : SyncScreenStep
    data object Done : SyncScreenStep
}

private val SyncPilot = FC {
    val scope = useCoroutineScope()
    var step: SyncScreenStep by useState(SyncScreenStep.SelectProvider)
    val (_, lang) = useLang()

    fun selectProvider(provider: FileProvider, preWork: suspend () -> Unit = {}) {
        scope.launchUndispatched {
            step = SyncScreenStep.Loading
            preWork()
            val bak = async {
                val string = provider.getContent().trim()
                if (string.isNotEmpty()) {
                    json.decodeFromString(SsdBak.serializer(), string)
                } else {
                    SsdBak(1, emptyMap())
                }
            }
            val current = async {
                val db = AppDatabase.getResult().getOrThrow()
                SsdBak(
                    version = 1,
                    lists = db.getAllUpdatedUserLists().associate { (update, listId, content) ->
                        listId to SsdBak.UserList(
                            update = update.getTime().toLong(),
                            content = content?.let { (list, games) ->
                                SsdBak.UserList.Content(
                                    name = list.name,
                                    games = games
                                )
                            }
                        )
                    }
                )
            }
            step = try {
                if (bak.await() == current.await()) {
                    SyncScreenStep.Done
                } else {
                    SyncScreenStep.ConfigureSynchronization(provider, bak.await(), current.await())
                }
            } catch (t: Throwable) {
                SyncScreenStep.Error(t.message ?: "unknown error")
            }
        }
    }

    fun synchronize(provider: FileProvider, config: Map<UserList.ID, Sync>, bak: SsdBak, current: SsdBak) {
        val newBakList = bak.lists.toMutableMap()
        scope.launchUndispatched {
            step = SyncScreenStep.Loading
            val writeable = if (config.any { (_, sync) -> sync.direction == Sync.Direction.CURRENT_TO_BAK }) {
                val writeable = provider.prepareWrite()
                if (writeable == null) {
                    step = SyncScreenStep.ConfigureSynchronization(provider, bak, current, config)
                    return@launchUndispatched
                }
                writeable
            } else null
            val db = AppDatabase.getResult().getOrThrow()
            config.forEach { (listId, sync) ->
                when (sync.direction) {
                    Sync.Direction.CURRENT_TO_BAK -> {
                        newBakList[listId] = current.lists[listId]!!
                    }
                    Sync.Direction.BAK_TO_CURRENT -> {
                        val bakList = bak.lists[listId]!!
                        db.putUserList(
                            date = Date(bakList.update.toDouble()),
                            list = UserList(id = listId, name = bakList.content!!.name),
                            games = bakList.content.games
                        )
                    }
                    null -> {}
                }
            }
            if (newBakList != bak.lists) {
                writeable?.write(
                    json.encodeToString(
                        SsdBak.serializer(),
                        SsdBak(
                            version = 1,
                            lists = newBakList
                        )
                    )
                )
            }
            step = SyncScreenStep.Done
        }
    }

    when (val s = step) {
        SyncScreenStep.SelectProvider -> Box {
            sx {
                display = Display.flex
                flexDirection = FlexDirection.column
                justifyContent = JustifyContent.center
                alignItems = AlignItems.center
                padding = 16.px
            }

            GoogleDriveInitButton {
                synchronize = { selectProvider(it) }
            }
            Box {
                sx {
                    height = 32.px
                    display = Display.flex
                    justifyContent = JustifyContent.center
                    alignItems = AlignItems.center
                }
                +lang.sync__or
            }
            LocalFileInitButton {
                synchronize = { selectProvider(it) }
            }
        }
        SyncScreenStep.Loading -> {
            CircularProgress {
                size = 4.em
                color = CircularProgressColor.primary
                sx {
                    margin = Margin(4.em, auto)
                    display = Display.block
                }
            }
        }

        is SyncScreenStep.ConfigureSynchronization -> {
            s.provider.syncComponent {
                this.restartConfig = {
                    selectProvider(s.provider, it)
                }
            }
            ConfigureSynchronizationContent {
                bak = s.bak
                current = s.current
                synchronize = { synchronize(s.provider, it, s.bak, s.current) }
                syncConfig = s.config
            }
        }

        is SyncScreenStep.Error -> {
            Typography {
                variant = TypographyVariant.h6
                sx {
                    color = Color("error.main")
                    textAlign = TextAlign.center
                    padding = 16.px
                }
                +s.error
            }
        }

        is SyncScreenStep.Done -> {
            Typography {
                variant = TypographyVariant.h6
                sx {
                    color = Color("success.main")
                    textAlign = TextAlign.center
                    padding = 16.px
                }
                +lang.sync__Everything_is_up_to_date
            }
        }
    }
}

@Serializable
private data class Sync(
    val bak: SsdBak.UserList?,
    val current: SsdBak.UserList?,
    val direction: Direction?,
    val possibleDirections: List<Direction>?,
) {
    enum class Direction {
        CURRENT_TO_BAK,
        BAK_TO_CURRENT,
    }
}

private fun Sync.moreRecentUpdate(): Long =
    max(bak?.update ?: 0, current?.update ?: 0)

private external interface ConfigureSynchronizationContentProps : Props {
    var bak: SsdBak
    var current: SsdBak
    var syncConfig: Map<UserList.ID, Sync>?
    var synchronize: (Map<UserList.ID, Sync>) -> Unit
}

private fun initialSyncConfig(
    bak: SsdBak,
    current: SsdBak,
): Map<UserList.ID, Sync> =
    (bak.lists.keys + current.lists.keys)
        .distinct()
        .mapNotNull { id ->
            val bakList = bak.lists[id]
            val bakUpdate = bakList?.update ?: 0L
            val curList = current.lists[id]
            val curUpdate = curList?.update ?: 0L
            when {
                bakList?.content == null && curList?.content == null -> when {
                    bakUpdate == curUpdate -> null
                    else -> id to Sync(
                        bak = bakList,
                        current = curList,
                        direction = if (curUpdate >= bakUpdate) Sync.Direction.CURRENT_TO_BAK else Sync.Direction.BAK_TO_CURRENT,
                        possibleDirections = null
                    )
                }
                bakUpdate == curUpdate && bakList?.content == curList?.content -> id to Sync(
                    bak = bakList,
                    current = curList,
                    direction = null,
                    possibleDirections = null
                )
                else -> id to Sync(
                    bak = bakList,
                    current = curList,
                    direction = if (curUpdate >= bakUpdate) Sync.Direction.CURRENT_TO_BAK else Sync.Direction.BAK_TO_CURRENT,
                    possibleDirections = when {
                        bakList == null && curList != null -> listOf(Sync.Direction.CURRENT_TO_BAK)
                        bakList != null && curList == null -> listOf(Sync.Direction.BAK_TO_CURRENT)
                        else -> listOf(Sync.Direction.CURRENT_TO_BAK, Sync.Direction.BAK_TO_CURRENT)
                    }
                )
            }
        }
        .toMap()

private val ConfigureSynchronizationContent = FC<ConfigureSynchronizationContentProps> { props ->
    val (_, lang) = useLang()
    var syncConfig by useState { props.syncConfig ?: initialSyncConfig(props.bak, props.current) }

    Button {
        variant = ButtonVariant.contained
        onClick = { props.synchronize(syncConfig) }
        sx {
            maxWidth = 256.px
            margin = auto
        }
        +lang.sync__Synchronize
    }

    Table {
        TableHead {
            TableRow {
                TableCell {
                    align = TableCellAlign.center
                    Typography {
                        variant = TypographyVariant.h5
                        +lang.sync__Application
                    }
                }
                TableCell { +"" }
                TableCell {
                    align = TableCellAlign.center
                    Typography {
                        variant = TypographyVariant.h5
                        +lang.sync__Backup
                    }
                }
            }
        }
        TableBody {
            syncConfig
                .filterValues { it.possibleDirections != null }
                .entries
                .sortedBy { it.value.moreRecentUpdate() }
                .forEach { (listId, sync) ->
                    TableRow {
                        TableCell {
                            align = TableCellAlign.center
                            sync.current.let { current ->
                                SyncContent {
                                    this.listId = listId
                                    this.list = current
                                    if (sync.direction == Sync.Direction.BAK_TO_CURRENT) {
                                        this.overridden = sync.bak
                                    }
                                }
                            }
                        }
                        TableCell {
                            align = TableCellAlign.center
                            if (sync.possibleDirections != null) {
                                SyncButton {
                                    direction = sync.direction
                                    possibleDirections = sync.possibleDirections
                                    onChange = { dir ->
                                        syncConfig = syncConfig.toMutableMap().also {
                                            it[listId] = it[listId]!!.copy(direction = dir)
                                        }
                                    }
                                }
                            }
                        }
                        TableCell {
                            align = TableCellAlign.center
                            sync.bak.let { bak ->
                                SyncContent {
                                    this.listId = listId
                                    this.list = bak
                                    if (sync.direction == Sync.Direction.CURRENT_TO_BAK) {
                                        this.overridden = sync.current
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

private external interface SyncButtonProps : Props {
    var direction: Sync.Direction?
    var possibleDirections: List<Sync.Direction>
    var onChange: (Sync.Direction?) -> Unit
}

private val SyncButton = FC<SyncButtonProps> { props ->
    var menuAnchor by useState<Element?>(null)

    IconButton {
        size = Size.large
        color = IconButtonColor.secondary
        onClick = { menuAnchor = it.currentTarget }

        when (props.direction) {
            Sync.Direction.CURRENT_TO_BAK -> ArrowCircleRight { fontSize = SvgIconSize.large }
            Sync.Direction.BAK_TO_CURRENT -> ArrowCircleLeft { fontSize = SvgIconSize.large }
            null -> PauseCircle { fontSize = SvgIconSize.large }
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

        props.possibleDirections.forEach { d ->
            MenuItem {
                onClick = {
                    props.onChange(d)
                    menuAnchor = null
                }
                when (d) {
                    Sync.Direction.CURRENT_TO_BAK -> ArrowCircleRight {
                        fontSize = SvgIconSize.large
                        color = SvgIconColor.secondary
                    }
                    Sync.Direction.BAK_TO_CURRENT -> ArrowCircleLeft {
                        fontSize = SvgIconSize.large
                        color = SvgIconColor.secondary
                    }
                }
            }
        }
        MenuItem {
            onClick = {
                props.onChange(null)
                menuAnchor = null
            }
            PauseCircle {
                fontSize = SvgIconSize.large
                color = SvgIconColor.secondary
            }
        }
    }
}

private external interface SyncContentProps : Props {
    @Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")
    var listId: UserList.ID
    var list: SsdBak.UserList?
    var overridden: SsdBak.UserList?
}

private sealed class SyncType {
    abstract val list: SsdBak.UserList?
    data class Create(override val list: SsdBak.UserList) : SyncType()
    data class Delete(override val list: SsdBak.UserList) : SyncType()
    data class Override(override val list: SsdBak.UserList, val over: SsdBak.UserList) : SyncType()
    data class Leave(override val list: SsdBak.UserList?) : SyncType()
}

private val SyncContent = FC<SyncContentProps> { props ->
    val (langId, lang) = useLang()

    val type = when {
        props.list?.content != null && props.overridden?.content != null -> SyncType.Override(props.list!!, props.overridden!!)
        props.list == null && props.overridden != null -> SyncType.Create(props.overridden!!)
        props.list != null && props.overridden != null && props.overridden!!.content == null -> SyncType.Delete(props.list!!)
        else -> SyncType.Leave(props.list)
    }

    val typeList = type.list ?: return@FC

    Typography {
        variant = TypographyVariant.h6
        sx {
            when (type) {
                is SyncType.Create -> opacity = number(0.5)
                is SyncType.Delete -> textDecoration = TextDecoration.lineThrough
                else -> {}
            }
        }

        if (props.listId == AppDatabase.favourites.id ) {
            +lang.lists__favourites
        } else {
            Box {
                component = span
                sx {
                    if (type is SyncType.Override && type.list.content?.name != type.over.content?.name) {
                        textDecoration = TextDecoration.lineThrough
                    }
                }
                +(typeList.content?.name ?: "")
            }
            if (type is SyncType.Override && type.list.content?.name != type.over.content?.name) {
                +" ▶ "
                +(type.over.content?.name ?: "")
            }
        }
    }
    Typography {
        variant = TypographyVariant.subtitle1
        sx {
            when (type) {
                is SyncType.Create -> opacity = number(0.5)
                is SyncType.Delete -> textDecoration = TextDecoration.lineThrough
                else -> {}
            }
        }

        val df = DateTimeFormat(langId, jso {
            dateStyle = DateStyle.medium
            timeStyle = TimeStyle.short
        })

        Box {
            component = span
            sx {
                if (type is SyncType.Override && type.list.update != type.over.update) {
                    textDecoration = TextDecoration.lineThrough
                }
            }
            +df.format(Date(typeList.update.toDouble()))
        }
        if (type is SyncType.Override && type.list.update != type.over.update) {
            +" ▶ "
            +df.format(Date(type.over.update.toDouble()))
        }
    }
    if (typeList.content != null) {
        Typography {
            variant = TypographyVariant.subtitle2
            sx {
                when (type) {
                    is SyncType.Create -> opacity = number(0.5)
                    is SyncType.Delete -> textDecoration = TextDecoration.lineThrough
                    else -> {}
                }
            }

            Box {
                component = span
                sx {
                    if (type is SyncType.Override && type.list.content?.games != type.over.content?.games) {
                        textDecoration = TextDecoration.lineThrough
                    }
                }
                +lang.X_games(typeList.content.games.size)
            }
            if (type is SyncType.Override && type.list.content?.games != type.over.content?.games) {
                +" ▶ "
                +lang.X_games(type.over.content?.games?.size ?: 0)
            }
        }
    }
}
