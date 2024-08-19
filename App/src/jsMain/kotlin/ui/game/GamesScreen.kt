package ui.game

import react.*
import react.router.useNavigate
import ui.useIsMobile


val GamesScreen = FC("GamesListScreen") {
    val isMobile = useIsMobile()
    if (isMobile) {
        GamesScreenMobile()
    } else {
        GamesScreenLarge()
    }
}

private val onGameFilterContext = createContext<(Filters) -> Unit>()

fun useOnGameFilter(): (Filters) -> Unit {
    val navigate = useNavigate()
    val spec = useContext(onGameFilterContext)
    if (spec != null) return spec
    return {
        navigate("/games?types=$it")
    }
}

external interface OnGameFilterProviderProps : PropsWithChildren {
    var onGameFilter: (Filters) -> Unit
}

val OnGameFilterProvider = FC<OnGameFilterProviderProps> { props ->
    onGameFilterContext(props.onGameFilter) {
        +props.children
    }
}
