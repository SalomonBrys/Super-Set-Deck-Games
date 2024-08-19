package ui

import js.objects.jso
import mui.material.styles.createTheme


val appTheme = createTheme(options = jso {
    typography = jso {
        fontFamily = "Picon"
    }
    palette = jso {
        primary = jso {
            main = "#ffa726"
            light = "#ffb851"
            dark = "#b2741a"
            contrastText = "#fff"
        }
        secondary = jso {
            main = "#00acc1"
            light = "#33bccd"
            dark = "#007887"
            contrastText = "#fff"
        }
        background = jso {
            default = "#ffffff"
        }
        grey = jso {
            light = "#eee"
        }
        asDynamic().custom = jso {
            fadeOnLightGrey = "#fff"
        }
    }
})
