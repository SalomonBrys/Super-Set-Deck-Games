package ui.game

import ui.ContentArea
import ui.ContentMobile
import ui.OpenAppDrawerIcon
import ui.SSDAppBar
import ui.SSDAppBarBackIcon
import ui.SSDAppBarTitle
import data.name
import data.useGame
import react.FC
import react.create
import react.router.useOutlet
import react.router.useParams
import react.useState
import ui.useLang


val GamesScreenMobile = FC("GamesListMobile") {
    val outlet = useOutlet()
    var count by useState<Int?>(null)
    val (_, lang) = useLang()

    ContentMobile {
        mainId = if (outlet != null) ContentArea.Content else ContentArea.List

        if (outlet != null) {
            GameContentMobileHeader()
            +outlet
        } else {
            SSDAppBar {
                navigationIcon = OpenAppDrawerIcon.create()
                SSDAppBarTitle {
                    label = if (count != null) lang.X_games(count!!) else ""
                }
            }
            GamesList {
                stickyFilters = true
                onCount = { count = it }
            }
        }
    }

}

private val GameContentMobileHeader = FC("GameMobileHeader") {
    val (langId, _) = useLang()
    val params = useParams()
    val gameId = params["id"]!!
    val game = useGame(gameId)

    SSDAppBar {
        navigationIcon = SSDAppBarBackIcon.create()

        SSDAppBarTitle {
            label = game?.name(langId) ?: ""
        }

        GameShareMenu {
            this.gameId = gameId
            this.gameName = game?.name(langId) ?: ""
        }
    }
}
