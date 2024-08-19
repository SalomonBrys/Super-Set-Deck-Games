package ui

import js.objects.jso
import mui.icons.material.*
import mui.material.*
import mui.material.List
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.dom.aria.ariaLabel
import react.router.useNavigate
import web.cssom.*


private val openAppDrawerContext = createRequiredContext<() -> Unit>()

val ProvideAppDrawer = FC<PropsWithChildren>("ProvideDrawer") { props ->
    var drawerShown by useState(false)

    Drawer {
        open = drawerShown
        onClose = { _, _ -> drawerShown = false }
        sx {
            zIndex = integer(2_500)
        }

        DrawerContent {
            closeDrawer = { drawerShown = false }
        }
    }

    openAppDrawerContext({ drawerShown = true }) {
        +props.children
    }
}

external interface DrawerContentProps : Props {
    var closeDrawer: () -> Unit
}

private val DrawerContent = FC<DrawerContentProps>("DrawerContent") { props ->
    val (_, lang) = useLang()
    val navigate = useNavigate()

    Box {
        sx {
            width = 256.px
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        Typography {
            variant = TypographyVariant.h5
            sx {
                display = Display.block
                width = 100.pct
                textAlign = TextAlign.center
                padding = 16.px
            }
            +"Super-Set Deck‽"
        }

        List {
            ListItemButton {
                onClick = {
                    navigate("/games", jso { state = NavFromApp })
                    props.closeDrawer()
                }
//                selected = true
                ListItemIcon {
                    Casino()
                }
                ListItemText {
                    +lang.title_Games
                }
            }
            ListItemButton {
                onClick = {
                    navigate("/lists", jso { state = NavFromApp })
                    props.closeDrawer()
                }
                ListItemIcon {
                    ListAlt()
                }
                ListItemText {
                    +lang.title_MyLists
                }
            }
            ListItemButton {
                onClick = {
                    navigate("/packer", jso { state = NavFromApp })
                    props.closeDrawer()
                }
                ListItemIcon {
                    Luggage()
                }
                ListItemText {
                    +lang.title_TravelPacker
                }
            }
            ListItemButton {
                onClick = {
                    navigate("/about", jso { state = NavFromApp })
                    props.closeDrawer()
                }
                ListItemIcon {
                    Help()
                }
                ListItemText {
                    +lang.title_About
                }
            }
            ListItemButton {
                onClick = {
                    navigate("/changelog", jso { state = NavFromApp })
                    props.closeDrawer()
                }
                ListItemIcon {
                    Update()
                }
                ListItemText {
                    +lang.title_Changelog
                }
            }
        }
    }
}

val OpenAppDrawerIcon = FC {
    val openAppDrawer = useRequiredContext(openAppDrawerContext)

    IconButton {
        sx { marginRight = 2.px }
        edge = IconButtonEdge.start
        size = Size.large
        color = IconButtonColor.inherit
        ariaLabel = "menu"
        onClick = { openAppDrawer() }

        mui.icons.material.Menu()
    }
}
