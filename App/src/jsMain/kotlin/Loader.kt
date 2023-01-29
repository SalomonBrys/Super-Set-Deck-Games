import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Img


@Composable
fun Loader() {
    Img(src = "loader.gif") {
        style {
            width(200.px)
            height(200.px)
            display(DisplayStyle.Block)
            property("margin", "auto")
        }
    }
}