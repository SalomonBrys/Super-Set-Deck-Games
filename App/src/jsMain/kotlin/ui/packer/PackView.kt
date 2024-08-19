package ui.packer

import data.cards.*
import data.name
import emotion.react.css
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.dom.html.ReactHTML
import react.useMemo
import ui.useLang
import web.cssom.*
import web.cssom.None.Companion.none


external interface PackViewProps : Props {
    var pack: Pack
    var gamesInTable: Boolean?
}

val PackView = FC<PackViewProps> { props ->
    val (langId, _) = useLang()

    val (packGroups, packCards) = useMemo(props.pack) {
        props.pack.groups() to props.pack.cards()
    }

    Box {
        sx {
            marginTop = 16.px
            padding = Padding(0.px, 4.px)
        }
        packGroups.forEach { (suit, suitGroups) ->
            Box {
                component = ReactHTML.p
                sx {
                    fontWeight = FontWeight.bold
                }

                Box {
                    component = ReactHTML.span
                    sx {
                        fontSize = 1.4.em
                        color = Color(suitDarkColor(suit))
                    }
                    suitSymbol(suit).takeIf { it.isNotEmpty() } ?.let {
                        +"$it "
                    }
                }
                suitGroups.forEachIndexed { index, (count, card) ->
                    if (index != 0) {
                        +", "
                    }
                    if (count >= 2) {
                        +"${count}×"
                    }
                    Box {
                        component = ReactHTML.span
                        sx {
                            display = Display.inlineFlex
                            justifyContent = JustifyContent.center
                            alignItems = AlignItems.center
                            height = 2.em
                            backgroundColor = Color(suitDarkColor(suit))
                            color = NamedColor.white
                            borderRadius = 4.px
                            padding = Padding(0.px, 4.px)
                            fontWeight = FontWeight.bold
                            verticalAlign = VerticalAlign.middle
                        }
                        if (suit == "specials" && card.length > 1) {
                            ReactHTML.img {
                                css {
                                    width = 24.px
                                    height = 32.px
                                }
                                src = "cards/$card-white.svg"
                            }
                        } else {
                            +card
                        }
                    }
                }
            }
        }
    }

    Divider()

    Table {
        size = Size.small
        sx {
            marginTop = 8.px
        }

        TableHead {
            TableRow {
                CardSuits.forEach { suit ->
                    TableCell {
                        align = TableCellAlign.center
                        sx {
                            color = Color(suitDarkColor(suit))
                            fontSize = 1.4.em
                            borderBottom = none
                            if (suit !in props.pack.suits) {
                                opacity = number(0.2)
                            }
                            padding = Padding(4.px, 8.px)
                        }
                        +suitSymbol(suit)
                    }
                }
            }
        }
        TableBody {
            CardRows.forEach { values ->
                TableRow {
                    CardSuits.forEach { suit ->
                        TableCell {
                            align = TableCellAlign.center
                            sx {
                                borderBottom = none
                                padding = Padding(4.px, 8.px)
                                position = Position.relative
                            }

                            val pairs = values.intersect(CardValues[suit]!!)
                            check(pairs.size in 0..1) { "Row contains ${pairs.size} values for $suit: $pairs" }
                            if (pairs.isNotEmpty()) {
                                val (value, count) = pairs.single()
                                val cardGames = packCards[Pack.PackedCard(suit, value, count)]

                                Tooltip {
                                    if (cardGames != null) {
                                        title = ReactNode(cardGames.joinToString { it.name(langId) })
                                    }

                                    Box {
                                        component = ReactHTML.span
                                        sx {
                                            display = Display.inlineFlex
                                            justifyContent = JustifyContent.center
                                            alignItems = AlignItems.center
                                            height = 2.em
                                            padding = Padding(0.px, 4.px)

                                            if (cardGames != null) {
                                                backgroundColor = Color(suitDarkColor(suit))
                                                color = NamedColor.white
                                                borderRadius = 4.px
                                                fontWeight = FontWeight.bold
                                            } else {
                                                opacity = number(0.2)
                                            }
                                        }
                                        if (suit == "specials" && value.length > 1) {
                                            ReactHTML.img {
                                                css {
                                                    width = 24.px
                                                    height = 32.px
                                                }
                                                if (cardGames != null) {
                                                    src = "cards/$value-white.svg"
                                                } else {
                                                    src = "cards/$value-black.svg"
                                                }
                                            }
                                        } else {
                                            +value
                                        }
                                    }
                                }

                                if (props.gamesInTable == true && cardGames != null) {
                                    Typography {
                                        component = ReactHTML.span
                                        variant = TypographyVariant.caption
                                        sx {
                                            display = Display.inlineBlock
                                            position = Position.absolute
                                            bottom = 0.px
                                            paddingLeft = 2.px
                                            fontSize = 0.6.em
                                            opacity = number(0.6)
                                        }

                                        +(cardGames.count().toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
