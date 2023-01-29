@file:OptIn(KMDCInternalAPI::class)

package utils

import androidx.compose.runtime.Composable
import dev.petuska.kmdc.core.KMDCInternalAPI
import dev.petuska.kmdc.core.MDCAttrs
import dev.petuska.kmdc.core.MDCAttrsRaw
import dev.petuska.kmdc.core.MDCContentDsl
import dev.petuska.kmdc.icon.button.MDCIconButton
import dev.petuska.kmdc.icon.button.MDCIconButtonAttrsScope
import dev.petuska.kmdc.tab.Icon
import dev.petuska.kmdc.tab.MDCTabContentScope
import dev.petuska.kmdcx.icons.MDCIcon
import dev.petuska.kmdcx.icons.MDCIconType
import dev.petuska.kmdcx.icons.mdcIcon
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLSpanElement


@Composable
public fun MDCTabContentScope.Icon(
  icon: MDCIcon,
  type: MDCIconType = MDCIconType.Filled,
  attrs: MDCAttrsRaw<HTMLSpanElement>? = null
) {
  Icon(attrs = {
    mdcIcon(type)
    attrs?.invoke(this)
  }) {
    Text(icon.type)
  }
}

@MDCContentDsl
@Composable
public fun MDCIconButton(
  icon: MDCIcon,
  on: Boolean? = null,
  touch: Boolean = false,
  attrs: MDCAttrs<MDCIconButtonAttrsScope<HTMLButtonElement>>? = null
) {
  MDCIconButton(
    on = on,
    touch = touch,
    attrs = {
      mdcIcon()
      attrs?.invoke(this)
    }
  ) {
    Text(icon.type)
  }
}
