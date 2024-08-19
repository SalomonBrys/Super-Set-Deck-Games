package ui.packer

import data.Game
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import ui.useLang
import ui.utils.toShortStrings
import web.cssom.*


external interface PackGameConfigurationProps : Props {
    var game: Game
    var selectedPlayerCounts: Set<Int>
    var setSelectedPlayerCounts: (Set<Int>) -> Unit
    var selectedVariants: Set<String>
    var setSelectedVariants: (Set<String>) -> Unit
}

val PackGameConfiguration = FC<PackGameConfigurationProps> { props ->
    val (_, lang) = useLang()

    Typography {
        variant = TypographyVariant.h6
        sx {
            textAlign = TextAlign.center
        }
        +lang.X_players(props.selectedPlayerCounts.sorted().toShortStrings().joinToString())
    }
    Box {
        sx {
            display = Display.flex
            flexDirection = FlexDirection.row
            justifyContent = JustifyContent.center
        }

        props.game.playerCount.forEach { count ->
            Chip {
                variant = if (count in props.selectedPlayerCounts) ChipVariant.filled else ChipVariant.outlined
                color = ChipColor.secondary
                label = ReactNode(count.toString())
                if (count !in props.selectedPlayerCounts || props.selectedPlayerCounts.size >= 2) {
                    onClick = {
                        props.setSelectedPlayerCounts(
                            if (count in props.selectedPlayerCounts) props.selectedPlayerCounts - count
                            else props.selectedPlayerCounts + count
                        )
                    }
                }
                sx {
                    margin = Margin(0.px, 16.px)
                }
            }
        }
    }

    val variants = props.game.cards.keys - "Base"

    if (variants.isNotEmpty()) {
        Typography {
            variant = TypographyVariant.h6
            sx {
                textAlign = TextAlign.center
                marginTop = 16.px
            }
            +lang.With_X_variants(props.selectedVariants.size)
        }
        Box {
            sx {
                display = Display.flex
                flexDirection = FlexDirection.row
                justifyContent = JustifyContent.center
            }

            variants.sorted().forEach { variant ->
                Chip {
                    this.variant = if (variant in props.selectedVariants) ChipVariant.filled else ChipVariant.outlined
                    color = ChipColor.secondary
                    label = ReactNode(variant)
                    onClick = {
                        props.setSelectedVariants(
                            if (variant in props.selectedVariants) props.selectedVariants - variant
                            else props.selectedVariants + variant
                        )
                    }
                    sx {
                        margin = Margin(0.px, 16.px)
                    }
                }
            }
        }
    }
}
