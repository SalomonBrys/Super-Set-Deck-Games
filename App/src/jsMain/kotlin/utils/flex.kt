package utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement


@Composable
fun FlexDiv(
    dir: FlexDirection,
    justifyContent: JustifyContent? = null,
    alignItems: AlignItems? = null,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: @Composable () -> Unit
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(dir)
            if (justifyContent != null) { justifyContent(justifyContent) }
            if (alignItems != null) { alignItems(alignItems) }
        }
        attrs?.invoke(this)
    }) {
        content()
    }
}

@Composable
fun FlexRow(
    justifyContent: JustifyContent? = null,
    alignItems: AlignItems? = null,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: @Composable () -> Unit
) = FlexDiv(FlexDirection.Row, justifyContent, alignItems, attrs, content)

@Composable
fun FlexColumn(
    justifyContent: JustifyContent? = null,
    alignItems: AlignItems? = null,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: @Composable () -> Unit
) = FlexDiv(FlexDirection.Column, justifyContent, alignItems, attrs, content)
