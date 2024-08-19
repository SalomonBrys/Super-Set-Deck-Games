package ui.game

import data.Game
import data.cards.*
import data.name
import emotion.react.css
import mui.material.*
import mui.material.Size
import mui.system.sx
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.img
import react.useMemo
import react.useState
import ui.packer.PackGameConfiguration
import ui.packer.PackView
import ui.useLang
import web.cssom.*
import web.cssom.None.Companion.none


external interface GamePackDialogContentProps : Props {
    var game: Game
}

val GamePackDialogContent = FC<GamePackDialogContentProps> { props ->
    val (langId, lang) = useLang()

    var selectedPlayerCounts by useState(props.game.playerCount.toSet())
    var selectedVariants by useState(props.game.cards.keys - "Base")

    val pack = useMemo(selectedPlayerCounts, selectedVariants) {
        Pack().apply { add(props.game, selectedPlayerCounts, selectedVariants) }
    }

    DialogTitle {
        +props.game.name(langId)
    }

    DialogContent {
        PackGameConfiguration {
            this.game = props.game
            this.selectedPlayerCounts = selectedPlayerCounts
            this.setSelectedPlayerCounts = { selectedPlayerCounts = it }
            this.selectedVariants = selectedVariants
            this.setSelectedVariants = { selectedVariants = it }
        }

        Divider {
            sx {
                marginTop = 16.px
            }
        }

        PackView {
            this.pack = pack
        }
    }
}