package ui.utils

import js.objects.jso
import mui.material.Dialog
import mui.material.DialogProps
import mui.material.PaperProps
import mui.system.sx
import react.FC
import react.dom.html.ReactHTML
import react.useEffectWithCleanup
import react.useState
import utils.launchUndispatched
import web.cssom.px
import web.events.Event
import web.events.addEventHandler
import web.form.FormData
import web.navigator.navigator


external interface KeyboardAwareDialogProps : DialogProps

val KeyboardAwareDialog = FC<KeyboardAwareDialogProps> { props ->
    var keyboardHeight by useState(0.0)

    useEffectWithCleanup {
        val vk = navigator.virtualKeyboard ?: return@useEffectWithCleanup
        vk.overlaysContent = true
        val unregister = vk.addEventHandler(Event.ongeometrychange()) { event ->
            keyboardHeight = event.currentTarget.boundingRect.height
        }
        onCleanup { unregister() }
    }

    Dialog {
        +props

        sx {
            +props.sx
            marginTop = (-keyboardHeight / 2.0).px
        }
    }
}

fun useDialogFormPaperProps(
    onSubmit: suspend (FormData) -> Unit,
    onClose: () -> Unit
): PaperProps {
    val scope = useCoroutineScope()
    return jso {
        this.component = ReactHTML.form
        this.onSubmit = { event ->
            event.preventDefault()
            val formData = FormData(event.currentTarget)
            scope.launchUndispatched {
                onSubmit(formData)
            }
            onClose()
        }
    }
}
