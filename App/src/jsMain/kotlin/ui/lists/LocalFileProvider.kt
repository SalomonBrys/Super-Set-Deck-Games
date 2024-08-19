package ui.lists

import data.filesystem.showOpenFilePicker
import data.filesystem.showSaveFilePicker
import js.objects.jso
import js.objects.recordOf
import js.promise.catch
import mui.material.*
import mui.system.sx
import react.FC
import react.useState
import ui.useLang
import ui.utils.useCoroutineScope
import utils.launchUndispatched
import web.cssom.integer
import web.dom.Element
import web.fs.FileSystemFileHandle


private class LocalFileProvider(
    private val openFile: FileSystemFileHandle?
) : FileProvider {

    override suspend fun getContent(): String =
        openFile?.getFile()?.text() ?: ""

    override val syncComponent = FC {}

    override suspend fun prepareWrite(): FileProvider.Writeable? {
        val saveFile = showSaveFilePicker(jso {
            excludeAcceptAllOption = true
            types = arrayOf(
                jso {
                    description = "Super-Set-Deck backup file"
                    accept = recordOf(
                        "application/ssd-back" to arrayOf(".ssdbak")
                    )
                }
            )
            suggestedName = openFile?.name ?: "MyCollection.ssdbak"
        }).catch { null }.await() ?: return null

        return FileProvider.Writeable { content ->
            val writeable = saveFile.createWritable()
            writeable.write(content)
            writeable.close()
        }
    }
}

val LocalFileInitButton: FC<FileProviderInitProps> = FC { props ->
    val (_, lang) = useLang()
    val scope = useCoroutineScope()
    var menuAnchor: Element? by useState()

    Button {
        onClick = {
            menuAnchor = it.currentTarget
        }
        variant = ButtonVariant.outlined
        color = ButtonColor.secondary
        +lang.sync__Synchronize_with_local_files
    }

    Menu {
        open = menuAnchor != null
        onClose = { menuAnchor = null }
        if (menuAnchor != null) {
            anchorEl = { menuAnchor!! }
        }
        sx { zIndex = integer(2001) }

        MenuItem {
            onClick = {
                props.synchronize(LocalFileProvider(null))
                menuAnchor = null
            }
            +lang.sync__Create_new_file
        }

        MenuItem {
            onClick = {
                scope.launchUndispatched {
                    val files = showOpenFilePicker(jso {
                        excludeAcceptAllOption = true
                        types = arrayOf(
                            jso {
                                description = lang.sync__SSD_backup_file
                                accept = recordOf(
                                    "application/ssd-back" to arrayOf(".ssdbak")
                                )
                            }
                        )
                    }).catch { null }.await()
                    if (files != null && files.isNotEmpty()) {
                        props.synchronize(LocalFileProvider(files[0]))
                    }
                }
                menuAnchor = null
            }
            +lang.sync__Open_existing_file
        }
    }
}
