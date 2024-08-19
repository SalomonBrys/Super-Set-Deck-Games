package ui.lists

import js.date.Date
import js.intl.DateStyle
import js.intl.DateTimeFormat
import js.intl.TimeStyle
import js.objects.jso
import module.google.GoogleAuth
import module.google.GoogleDrive
import mui.material.*
import mui.system.sx
import react.FC
import react.useMemo
import react.useState
import ui.useLang
import ui.utils.useCoroutineScope
import utils.launchUndispatched
import web.cssom.*

class GoogleDriveFileProvider(private val drive: GoogleDrive) : FileProvider {
    private var file: GoogleDrive.File? = null

    private companion object {
        private const val SSD_FILE_NAME = "Super-Set-Deck-Collections.json"
        private const val SSD_FILE_TYPE = "application/json"
        private const val SSD_FILE_DIR = "appDataFolder"
    }

    override val syncComponent: FC<FileProviderSyncProps> = FC { props ->
        val (langId, lang) = useLang()
        val dtf = useMemo(langId) {
            DateTimeFormat(langId, jso {
                dateStyle = DateStyle.medium
                timeStyle = TimeStyle.short
            })
        }
        var confirmOpen by useState(false)

        Box {
            sx {
                backgroundColor = Color("secondary.light")
                border = Border(2.px, LineStyle.solid)
                borderColor = Color("secondary.dark")
                borderRadius = 8.px
                color = Color("secondary.contrastText")
                padding = 4.px
                margin = 8.px
                textAlign = TextAlign.center
            }
            if (file == null) {
                +lang.sync__A_new_file_will_be_created_in_your_drive_AppData
            } else {
                +lang.sync__Last_Google_Drive_backup(dtf.format(Date(file!!.modifiedTime)))
                Button {
                    variant = ButtonVariant.outlined
                    color = ButtonColor.error
                    onClick = { confirmOpen = true }
                    sx {
                        marginLeft = 32.px
                    }
                    +lang.deleteListDialog__delete
                }
            }
        }

        Dialog {
            open = confirmOpen
            onClose = { _, _ -> confirmOpen = false }
            sx {
                zIndex = integer(4000)
            }

            DialogTitle {
                +lang.sync__Delete_Drive_backup
            }
            DialogContent {
                if (file != null) {
                    +lang.sync__Last_Google_Drive_backup(dtf.format(Date(file!!.modifiedTime)))
                }
            }
            DialogActions {
                Button {
                    onClick = { confirmOpen = false }
                    color = ButtonColor.secondary
                    +lang.dialog__cancel
                }
                Button {
                    color = ButtonColor.error
                    onClick = {
                        confirmOpen = false
                        props.restartConfig {
                            drive.deleteFile(file!!.id)
                            file = null
                        }
                    }
                    +lang.deleteListDialog__delete
                }
            }
        }
    }

    override suspend fun getContent(): String {
        val list = drive.listFiles(
            q = GoogleDrive.Query { (name isEqualTo SSD_FILE_NAME) and (SSD_FILE_DIR isIn parents) and (trashed isEqualTo false) },
            spaces = listOf(GoogleDrive.Space.AppDataFolder)
        )
        if (list.size >= 2) {
            console.warn("Found multiple Super-Set-Deck-Collections.json files in drive appDataFolder, will take the first.")
        }
        file = list.firstOrNull()

        return file?.let { drive.getFileContent(it.id) } ?: ""
    }

    override suspend fun prepareWrite() = FileProvider.Writeable {
        if (file == null) {
            drive.createFile(
                name = SSD_FILE_NAME,
                mimeType = SSD_FILE_TYPE,
                parents = listOf(SSD_FILE_DIR),
                content = it
            )
        } else {
            drive.updateFile(
                id = file!!.id,
                mimeType = SSD_FILE_TYPE,
                content = it
            )
        }
    }
}

private object GoogleKeys {
    val clientId = "269700352625-hqcmod7k8527ennfnb040jk6ef3s5m8t.apps.googleusercontent.com"
    val apiScopes = listOf(
        "https://www.googleapis.com/auth/drive.appdata"
    )
}

val GoogleDriveInitButton: FC<FileProviderInitProps> = FC { props ->
    val scope = useCoroutineScope()
    val (_, lang) = useLang()

    Button {
        onClick = {
            GoogleAuth.requestAccessToken(
                clientId = GoogleKeys.clientId,
                scopes = GoogleKeys.apiScopes
            ) { accessToken ->
                scope.launchUndispatched {
                    val drive = GoogleDrive.get(accessToken)
                    if (drive != null) {
                        props.synchronize(GoogleDriveFileProvider(drive))
                    }
                }
            }
        }
        variant = ButtonVariant.outlined
        color = ButtonColor.secondary

        +lang.sync__Synchronize_with_Google_Drive
    }

}
