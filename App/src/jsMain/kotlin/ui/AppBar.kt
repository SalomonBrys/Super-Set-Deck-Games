package ui

import mui.icons.material.ArrowBack
import mui.icons.material.Home
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.system.PropsWithSx
import mui.system.sx
import react.FC
import react.Props
import react.PropsWithChildren
import react.ReactNode
import react.dom.aria.ariaLabel
import react.router.useLocation
import react.router.useNavigate
import web.cssom.*
import web.cssom.Auto.Companion.auto


external interface SSDAppBarProps: PropsWithChildren {
    var navigationIcon: ReactNode?
}

val SSDAppBar = FC<SSDAppBarProps>("SSDAppBar") { props ->

    Box {
        sx {
            gridArea = ContentArea.Header
            maxWidth = ContentSize.App.maxWidth
        }
        AppBar {
            sx {
                zIndex = integer(1_500)
                maxWidth = ContentSize.App.maxWidth
                left = auto
                right = auto
            }
            position = AppBarPosition.fixed

            Toolbar {
                if (props.navigationIcon != null) {
                    +props.navigationIcon
                }

                Box {
                    sx {
                        display = Display.flex
                        flexDirection = FlexDirection.row
                        alignItems = AlignItems.center
                        justifyContent = JustifyContent.center
                        flexGrow = number(1.0)
                    }
                    if (props.children != null) {
                        +props.children
                    }
                }

                LangMenu()
            }
        }
        Toolbar {
            sx {
                backgroundColor = Color("grey.light")
            }
        }
    }

}

external interface SSDAppBarTitleProps : Props, PropsWithSx {
    var label: String
}

val SSDAppBarTitle = FC<SSDAppBarTitleProps>("SSDAppBarTitle") { props ->
    Typography {
        variant = TypographyVariant.h6
        noWrap = true
        sx {
            flexGrow = number(1.0)
            textAlign = TextAlign.center
            +props.sx
        }

        +props.label
    }
}

val SSDAppBarBackIcon = FC("SSDAppBarBackIcon") {
    val location = useLocation()
    val navigate = useNavigate()

    val hasPrevious = location.state is NavFromApp

    IconButton {
        edge = IconButtonEdge.start
        size = Size.large
        color = IconButtonColor.inherit
        ariaLabel = "back"
        onClick = {
            if (hasPrevious) {
                navigate(-1)
            } else {
                navigate("/games")
            }
        }

        if (hasPrevious) {
            ArrowBack()
        } else {
            Home()
        }
    }
}