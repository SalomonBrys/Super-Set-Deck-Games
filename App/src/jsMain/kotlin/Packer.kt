import androidx.compose.runtime.*
import data.Game
import data.LocalLang
import data.name
import dev.petuska.kmdc.button.MDCButton
import dev.petuska.kmdc.button.MDCButtonType
import dev.petuska.kmdc.card.MDCCard
import dev.petuska.kmdc.checkbox.MDCCheckbox
import dev.petuska.kmdc.dialog.*
import dev.petuska.kmdc.form.field.MDCFormField
import dev.petuska.kmdc.list.Divider
import dev.petuska.kmdc.select.MDCSelect
import dev.petuska.kmdc.select.anchor.Anchor
import dev.petuska.kmdc.select.menu.Menu
import dev.petuska.kmdc.select.menu.SelectItem
import dev.petuska.kmdc.select.onChange
import dev.petuska.kmdc.tooltip.MDCTooltip
import dev.petuska.kmdc.tooltip.tooltipId
import dev.petuska.kmdcx.icons.MDCIcon
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLDivElement
import utils.*

@Composable
private fun <T : Comparable<T>> CheckList(
    title: String,
    all: Set<T>,
    set: Set<T>,
    onChange: (Set<T>) -> Unit
) {
    MDCFormField(attrs = {
        style {
            marginTop(2.em)
            fontSize(1.2.em)
            fontWeight("bold")
        }
    }) {
        MDCCheckbox(
            checked = when {
                set.size == all.size -> true
                set.isEmpty() -> false
                else -> null
            },
            touch = true,
            label = title,
            attrs = {
                onChange {
                    onChange(if (it.value) all else emptySet())
                }
            }
        )
    }
    FlexRow(JustifyContent.Center, AlignItems.Center, {
        style {
            flexWrap(FlexWrap.Wrap)
        }
    }) {
        all.toList().sorted().forEach { value ->
            MDCFormField(attrs = {
                style { marginRight(2.em) }
            }) {
                MDCCheckbox(
                    checked = value in set,
                    touch = true,
                    label = value.toString(),
                    attrs = {
                        onChange {
                            onChange(if (it.value) set + value else set - value)
                        }
                    }
                )
            }
        }
    }
}

private data class Pack(
    val game: Game,
    val players: Set<Int>,
    val variants: Set<String>
)

private fun Game.toPack() = Pack(
    game = this,
    players = cards.flatMap { (_, g) -> g.flatMap { (_, p) -> p.players } }.toSet(),
    variants = (cards.keys - "Base").toSet()
)

private fun Pack.toSuits(): Map<String, MutableMap<String, Int>> {
    val suits = HashMap<String, MutableMap<String, Int>>()
    (variants + "Base").forEach { variant ->
        game.cards[variant]!!.values
            .filter { p2c -> players.any { it in p2c.players } }
            .forEach { p2c ->
                p2c.cards.forEach { (suit, cards) ->
                    val packCards = suits.getOrPut(suit) { HashMap() }
                    cards.forEach { (card, count) ->
                        packCards[card] = (packCards[card] ?: 0) + count
                    }
                }
            }
    }
    return suits
}

@Composable
private fun PackerDialog(gamesList: List<Game>, trigger: SharedFlow<Pack?>, addPack: (Pack) -> Unit) {
    var pack: Pack? by remember { mutableStateOf(null) }
    var count by remember { mutableStateOf(0) }
    var edit: Game? by remember { mutableStateOf(null) }
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        trigger.collect {
            pack = it
            edit = it?.game
            open = true
            ++count
        }
    }

    val lang = LocalLang.current

    MDCDialog(
        open = open,
        fullscreen = true,
        stacked = false,
        attrs = {
            onOpened { open = true }
            onClosed { open = false }
            onClosing {
                if (it.detail.action == "ok") {
                    pack?.let(addPack)
                    ++count
                }
            }
        }
    ) {
        Title(
            title = if (edit == null) lang.Add_game else edit!!.name,
            attrs = {
                style { marginLeft(1.em) }
            }
        )
        Content {
            FlexColumn(JustifyContent.Center, AlignItems.Center, {
                style {
                    padding(1.em)
                }
            }) {
                if (edit == null) {
                    var favs: Set<String>? by remember { mutableStateOf(null) }
                    MDCFormField {
                        MDCCheckbox(
                            checked = favs != null,
                            touch = true,
                            label = lang.Favorites_only,
                            attrs = {
                                onChange {
                                    favs = if (it.value) Cookies["favs"]?.split(",")?.map { decodeURIComponent(it) }?.toSet() else null
                                    pack = null
                                    ++count
                                }
                            }
                        )
                    }

                    key(count) {
                        val games by rememberUpdatedState(gamesList.sortedBy { it.name(lang) })
                        MDCSelect(
                            attrs = {
                                onChange {
                                    val gameId = it.detail.value
                                    pack = games.first { it.id == gameId }.toPack()
                                }
                                style { width(18.cssRem) }
                            }
                        ) {
                            Anchor(lang.Games)
                            Menu(
                                fixed = true,
                                attrs = {
                                    style { width(18.cssRem) }
                                }
                            ) {
                                SelectItem("", selected = true)
                                Divider()
                                games
                                    .filter { favs?.contains(it.id) ?: true }
                                    .forEach {
                                        SelectItem(text = it.name, value = it.id)
                                    }
                            }
                        }
                    }
                }

                val p = pack
                if (p != null) {
                    key(p.game.id) {
                        CheckList(
                            title = LocalLang.current.players.replaceFirstChar { it.uppercase() },
                            all = p.game.cards.flatMap { (_, g) -> g.flatMap { (_, p) -> p.players } }.toSet(),
                            set = p.players,
                            onChange = {
                                pack = p.copy(players = it)
                            }
                        )

                        if (p.game.cards.size > 1) {
                            CheckList(
                                title = LocalLang.current.Variants,
                                all = (p.game.cards.keys - "Base").toSet(),
                                set = p.variants,
                                onChange = { pack = p.copy(variants = it) }
                            )
                        }

                    }
                }
            }
        }

        Actions {
            Action("close", lang.Cancel)
            Action("ok", if (edit == null) lang.Add else lang.Edit, true)
        }
    }
}

@Composable
private fun PackerGamesList(packs: List<Pack>, trigger: FlowCollector<Pack?>, onDelete: (Pack) -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    FlexColumn(JustifyContent.Center, AlignItems.Center, {
        style {
            width(100.percent)
            maxWidth(25.cssRem)
        }
    }) {
        packs.forEach { pack ->
            MDCCard(attrs = {
                style {
                    width(100.percent)
                    margin(0.5.em)
                    padding(0.25.em, 0.25.em, 0.25.em, 0.75.em)
                }
            }) {
                FlexRow(JustifyContent.Center, AlignItems.Center) {
                    Span({
                        style { flex(1) }
                    }) {
                        Span({ classes("mdc-deprecated-list-item__primary-text") }) {
                            Text(pack.game.name)
                        }
                        Span({ classes("mdc-deprecated-list-item__secondary-text") }) {
                            Text("${pack.players.sorted().toShortStrings().joinToString()} ${LocalLang.current.players}")
                            if (pack.variants.isNotEmpty()) {
                                Text(" + ${pack.variants.joinToString()}")
                            }
                        }

                    }
                    MDCIconButton(
                        icon = MDCIcon.Edit,
                        attrs = {
                            onClick {
                                coroutineScope.launch {
                                    trigger.emit(pack)
                                }
                            }
                        }
                    )
                    MDCIconButton(
                        icon = MDCIcon.Delete,
                        attrs = {
                            onClick {
                                onDelete(pack)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSpan(content: @Composable () -> Unit) {
    Span({
        style {
            border(1.px, LineStyle.Solid, Color.black)
            borderRadius(3.px)
            padding(0.4.em, 0.2.em)
        }
    }) {
        content()
    }
}

private fun suitSymbol(suit: String) = when (suit) {
    "stars" -> "★"
    "spades" -> "♠"
    "hearts" -> "♥"
    "clubs" -> "♣"
    "diamonds" -> "♦"
    "florettes" -> "✿"
    "wheels" -> "⎈"
    "specials" -> ""
    else -> error("Unknown suit $suit")
}

@Composable
private fun PackerGameCards(packs: List<Pack>) {
    MDCCard(attrs = {
        style {
            width(100.percent)
            maxWidth(50.cssRem)
            marginTop(1.cssRem)
            marginBottom(2.cssRem)
            paddingLeft(1.cssRem)
            paddingRight(1.cssRem)
        }
    }) {
        val allSuits = HashMap<String, MutableMap<String, Int>>()
        packs.forEach { pack ->
            pack.toSuits().forEach { (suit, cards) ->
                val allCards = allSuits.getOrPut(suit) { HashMap() }
                cards.forEach { (card, count) ->
                    allCards[card] = maxOf(allCards[card] ?: 0, count)
                }
            }
        }

        H3({
            style {
                textAlign("center")
            }
        }) {
            val allCount = allSuits.values.sumOf { it.values.sum() }
            Text("$allCount ${LocalLang.current.Cards}")
        }

        allSuits
            .entries
            .sortedBy { (suit, _) ->
                when (suit) {
                    "stars" -> "!0"
                    "spades" -> "!1"
                    "hearts" -> "!2"
                    "clubs" -> "!3"
                    "diamonds" -> "!4"
                    "florettes" -> "!5"
                    "wheels" -> "!6"
                    else -> suit
                }
            }
            .forEach { (suit, cards) ->
                val numberCards = ArrayList<Pair<Int, Int>>()
                val specialCards = ArrayList<Pair<String, Int>>()
                cards.forEach { (card, count) ->
                    val number = card.toIntOrNull()
                    if (number != null) numberCards += number to count
                    else specialCards += (LocalLang.current.cardNames[card] ?: card) to count
                }

                val cardList = (
                        numberCards.groupBy { (_, count) -> count }
                            .map { (count, list) -> count to list.map { (value, _) -> value } .sorted().toShortStrings() }
                            .sortedBy { (count, _) -> count }
                            .flatMap { (count, list) -> list.map { it to count } }
                        ) + (
                        specialCards.sortedBy { (value, _) ->
                            when (value) {
                                "J" -> "!0"
                                "S" -> "!1"
                                "Q" -> "!2"
                                "K" -> "!3"
                                "C" -> "!4"
                                "A" -> "!5"
                                else -> value
                            }
                        }
                        )

                FlexRow(attrs = {
                    style {
                        margin(.6.em, 0.em)
                        lineHeight(2.4.em)
                    }
                }) {
                    FlexRow(JustifyContent.End, AlignItems.Start, {
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Row)
                            width(7.em)
                        }
                    }) {
                        B {
                            Text((LocalLang.current.cardNames[suit] ?: suit).replaceFirstChar { it.uppercase() })
                        }
                        Span({
                            style {
                                fontSize(2.5.em)
                            }
                        }) {
                            Text(suitSymbol(suit))
                        }
                        Text(": ")
                    }
                    P({
                        style {
                            margin(0.em)
                        }
                    }) {
                        var coma = false
                        cardList.forEach { (card, count) ->
                            if (coma) Text(", ")
                            coma = true
                            if (count > 1) B { Text("${count}×") }
                            CardSpan { Text(card) }
                        }
                    }
                }
            }
    }
}

private data class C(val name: String, val num: Int = 1)

@Suppress("DuplicatedCode")
private val cards = listOf(
    "stars" to listOf(
        listOf(C("-‽"), C("×2")),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15"), C("16"), C("17"), C("18"), C("19"), C("20"), C("21")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3), C("S"), C("C")),
        listOf(C("A")),
    ),
    "spades" to listOf(
        emptyList(),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15"), C("16"), C("17"), C("18")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3), C("S")),
        listOf(C("A")),
    ),
    "hearts" to listOf(
        emptyList(),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15"), C("16"), C("17"), C("18")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3), C("S")),
        listOf(C("A")),
    ),
    "clubs" to listOf(
        emptyList(),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15"), C("16"), C("17"), C("18")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3), C("S")),
        listOf(C("A")),
    ),
    "diamonds" to listOf(
        emptyList(),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15"), C("16"), C("17"), C("18")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3), C("S")),
        listOf(C("A")),
    ),
    "florettes" to listOf(
        emptyList(),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8"), C("9"), C("10"), C("11"), C("12"), C("13"), C("14"), C("15")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2), C("K", 3)),
        listOf(C("A")),
    ),
    "wheels" to listOf(
        listOf(C("►")),
        listOf(C("0"), C("1"), C("2"), C("3"), C("4"), C("5"), C("6"), C("7"), C("8")),
        listOf(C("J"), C("Q"), C("K", 1), C("K", 2)),
        listOf(C("A")),
    ),
)

private val specials = listOf(C("Ø"))
private val animals = listOf(
    listOf(C("Butterfly", 1), C("Butterfly", 2), C("Butterfly", 3), C("Butterfly", 4), C("Butterfly", 5), C("Butterfly", 6)),
    listOf(C("Wolf", 1), C("Wolf", 2), C("Wolf", 3), C("Wolf", 4), C("Wolf", 5), C("Wolf", 6)),
    listOf(C("Phoenix", 1), C("Phoenix", 2), C("Phoenix", 3), C("Phoenix", 4)),
    listOf(C("Snake", 1), C("Snake", 2), C("Snake", 3), C("Snake", 4)),
    listOf(C("Dragon", 1), C("Dragon", 2)),
    listOf(C("Monkey", 1), C("Monkey", 2)),
)


@Composable
private fun PackerTableCell(
    large: Boolean = false,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: @Composable () -> Unit
) {
    FlexRow(JustifyContent.Center, AlignItems.Center, {
        style {
            width(if (large) 6.cssRem else 3.cssRem)
            height(1.5.cssRem)
        }
        attrs?.invoke(this)
    }) {
        content()
    }
}

@Composable
private fun PackerTableCardCell(suit: String, card: C, packs: List<Pack>) {
    val included = packs
        .filter { pack ->
            val count = pack.toSuits()[suit]?.get(card.name) ?: 0
            card.num <= count
        }
        .map { it.game.name }
    val tooltipId = "card-$suit-${card.name}-${card.num}"
    if (included.isNotEmpty()) {
        MDCTooltip(tooltipId, attrs = {
            style {
                maxWidth("min(80%,30em)")
            }
        }) {
            Text(included.joinToString { it })
        }
    }
    PackerTableCell(
        large = suit == "specials",
        attrs = {
            style {
                cursor("pointer")
            }
            if (included.isNotEmpty()) {
                tooltipId(tooltipId)
            }
        }
    ) {
        Span({
            style {
                position(Position.Relative)
                opacity(if (included.isNotEmpty()) 1.0 else 0.2)
            }
        }) {
            if (suit == "specials") {
                Text(LocalLang.current.cardNames[card.name] ?: card.name)
            } else {
                Text(card.name)
            }

            if (included.isNotEmpty() && packs.size >= 2) {
                Span({
                    style {
                        position(Position.Absolute)
                        fontSize(0.6.em)
                        fontWeight(600)
                        top(0.em)
                        right((-1.5).em)
                    }
                }) {
                    Text(included.count().toString())
                }
            }
        }
    }
}

@Composable
private fun PackerTable(packs: List<Pack>) {
    MDCCard(attrs = {
        style {
            width(100.percent)
            maxWidth(50.cssRem)
            marginTop(1.em)
            marginBottom(2.cssRem)
            paddingTop(0.5.em)
        }
    }) {
        FlexColumn(JustifyContent.Center, AlignItems.Center) {
            FlexRow {
                for ((suit, _) in cards) {
                    FlexColumn {
                        PackerTableCell {
                            B({
                                style { fontSize(2.cssRem) }
                            }) {
                                Text(suitSymbol(suit))
                            }
                        }
                    }
                }
            }
            for (i in 0..3) {
                FlexRow {
                    for ((suit, sections) in cards) {
                        val section = sections[i]
                        FlexColumn {
                            for (card in section) {
                                PackerTableCardCell(suit, card, packs)
                            }
                            if (section.isEmpty()) {
                                PackerTableCell {}
                            }
                        }
                    }
                }
            }
            B({
                style {
                    fontSize(1.5.cssRem)
                    paddingTop(1.cssRem)
                }
            }) {
                Text(LocalLang.current.cardNames["specials"]?.replaceFirstChar { it.titlecase() } ?: "Specials")
            }
            FlexRow {
                for (card in specials) {
                    PackerTableCardCell("specials", card, packs)
                }
            }
            for (i in animals.indices step 2) {
                FlexRow(attrs = {
                    style {
                        paddingTop(0.6.em)
                    }
                }) {
                    for (j in 0..1) {
                        if (i + j >= animals.size) break
                        FlexColumn {
                            for (card in animals[i + j]) {
                                PackerTableCardCell("specials", card, packs)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Packer(games: List<Game>) {

    var packs: List<Pack> by remember { mutableStateOf(emptyList()) }
//    var packs: List<Pack> by remember { mutableStateOf(listOf(
//        Pack(games.first { it.id == "Oh_7" }, setOf(3, 4), setOf("Advanced")),
//        Pack(games.first { it.id == "Fox_in_Forest" }, setOf(2), emptySet()),
//        Pack(games.first { it.id == "Yokai_Septet" }, setOf(3, 4), setOf("Seven-Suiters")),
//    )) }

    val trigger = remember { MutableSharedFlow<Pack?>() }
    val coroutineScope = rememberCoroutineScope()

    FlexColumn(JustifyContent.Center, AlignItems.Center, {
        style {
            width(100.percent)
        }
    }) {
        PackerGamesList(packs, trigger) { packs = packs - it }

        MDCButton(
            text = LocalLang.current.Add_game,
            type = MDCButtonType.Raised,
            touch = true,
            attrs = {
                style {
                    backgroundColor(Color("var(--mdc-theme-secondary)"))
                }
                onClick { event ->
                    if (event.altKey) {
                        packs = games.map { it.toPack() }
                    } else {
                        coroutineScope.launch {
                            trigger.emit(null)
                        }
                    }
                }
            }
        )

        PackerDialog(games - packs.map { it.game }.toSet(), trigger.asSharedFlow()) { pack ->
            val index = packs.indexOfFirst { it.game.id == pack.game.id }

            packs = if (index == -1) {
                packs + pack
            } else {
                buildList {
                    addAll(packs)
                    set(index, pack)
                }
            }
        }

        if (packs.isNotEmpty()) {
            PackerGameCards(packs)
            PackerTable(packs)
        }
    }
}
