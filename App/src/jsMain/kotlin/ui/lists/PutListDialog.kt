package ui.lists

import data.AppDatabase
import data.UserList
import js.objects.jso
import mui.material.*
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import ui.useLang
import ui.utils.KeyboardAwareDialog
import ui.utils.useDialogFormPaperProps
import web.cssom.px
import web.html.ButtonType


external interface PutListDialogProps : Props {
    var open: Boolean
    var onClose: () -> Unit
    var list: UserList?
    var onPut: ((UserList) -> Unit)?
    var db: AppDatabase
}

val PutListDialog = FC<PutListDialogProps> { props ->
    val (_, lang) = useLang()
    val dialogPaperProps = useDialogFormPaperProps(
        onSubmit = { data ->
            val name = data["name"] as String
            if (props.list == null) {
                val newList = props.db.createUserList(UserList(id = UserList.ID.empty, name = name))
                props.onPut?.invoke(newList)
            } else {
                val newList = props.db.updateUserList(props.list!!.id) { it.copy(name = name) }
                if (newList != null) {
                    props.onPut?.invoke(newList)
                }
            }
        },
        onClose = props.onClose
    )

    KeyboardAwareDialog {
        open = props.open
        onClose = { _, _ -> props.onClose() }
        PaperProps = dialogPaperProps

        DialogTitle {
            if (props.list == null) +lang.putListDialog__New_game_list
            else +lang.putListDialog__Rename_list(props.list!!.name)
        }
        DialogContent {
            TextField {
                margin = FormControlMargin.dense
                autoFocus = true
                fullWidth = true
                required = true
                inputProps = jso {
                    asDynamic().enterKeyHint = "done"
                }
                sx {
                    width = 256.px
                }
                name = "name"
                variant = FormControlVariant.standard
                label = ReactNode(lang.putListDialog__List_name)
                if (props.list != null) {
                    defaultValue = props.list!!.name
                }
            }
        }
        DialogActions {
            Button {
                onClick = { props.onClose() }
                +lang.dialog__cancel
            }
            Button {
                type = ButtonType.submit
                if (props.list == null) +lang.putListDialog__create
                else +lang.putListDialog__rename
            }
        }
    }
}
