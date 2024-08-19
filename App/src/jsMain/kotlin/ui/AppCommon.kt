package ui

import mui.material.CircularProgress
import mui.material.CircularProgressColor
import mui.system.sx
import react.FC
import web.cssom.Auto
import web.cssom.Display
import web.cssom.Margin
import web.cssom.em


val AppCircularProgress = FC {
    CircularProgress {
        size = 4.em
        color = CircularProgressColor.primary
        sx {
            margin = Margin(4.em, Auto.auto)
            display = Display.block
        }
    }
}