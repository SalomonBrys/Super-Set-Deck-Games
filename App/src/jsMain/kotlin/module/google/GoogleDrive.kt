package module.google

import js.array.ReadonlyArray
import js.objects.ReadonlyRecord
import js.objects.Record
import js.objects.jso
import js.objects.recordOf


private external interface JsGoogleDriveAPI {
    interface Files {

        interface FileListResult {
            val files: ReadonlyArray<File>
        }

        interface File {
            val mimeType: String
            val parents: ReadonlyArray<String>
            val size: String
            val spaces: ReadonlyArray<String>
            val id: String
            val name: String
            val trashed: Boolean
            val modifiedTime: String
        }

        interface ListArgs {
            var q: String?
            var spaces: String?
            var fields: String?
        }
        fun list(args: ListArgs): GoogleClientJs.Thenable<GoogleClientJs.Result<FileListResult>>

        interface GetArgs {
            var fileId: String?
            var fields: String?
            var alt: String?
        }
        fun get(args: GetArgs): GoogleClientJs.Thenable<GoogleClientJs.Result<dynamic>>

        interface DeleteArgs {
            var fileId: String?
        }
        fun delete(args: DeleteArgs): GoogleClientJs.Thenable<GoogleClientJs.Result<Nothing?>>
    }
    val files: Files
}

class GoogleDrive private constructor(
    private val drive: JsGoogleDriveAPI,
    private val client: GoogleClient
) {

    companion object {
        private const val discoveryDocumentUrl = "https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"

        suspend fun get(
            accessToken: String,
        ): GoogleDrive? {
            val client = GoogleClient.get() ?: return null
            client.setAccessToken(accessToken)
            val api = client.load<JsGoogleDriveAPI>(discoveryDocumentUrl, "drive") ?: return null
            return GoogleDrive(api, client)
        }

        fun Query(build: QueryBuilder.() -> Query): Query = QueryBuilder.build()

        private val fileFields = listOf("mimeType", "parents", "size", "spaces", "id", "name", "trashed", "modifiedTime").joinToString(",")
    }

    enum class Space(val id: String) {
        Drive("drive"), AppDataFolder("appDataFolder");
        companion object {
            fun fromId(id: String): Space? = entries.firstOrNull { it.id == id }
        }
    }

    value class Query(val q: String)
    object QueryBuilder {
        value class Field<T>(val field: String)
        val name get() = Field<String>("name")
        val parents get() = Field<String>("parents")
        val trashed get() = Field<Boolean>("parents")

        infix fun <T> Field<T>.isEqualTo(value: T) = Query("$field = '$value'")
        infix fun <T> T.isIn(field: Field<T>) = Query("'$this' in ${field.field}")

        infix fun Query.and(that: Query) = Query("(${this.q}) and (${that.q})")
        infix fun Query.or(that: Query) = Query("(${this.q}) and (${that.q})")
    }

    data class File(
        val mimeType: String,
        val parents: List<String>,
        val size: Long,
        val spaces: List<Space>,
        val id: String,
        val name: String,
        val trashed: Boolean,
        val modifiedTime: String,
    )

    private fun JsGoogleDriveAPI.Files.File.toKt() = File(
        mimeType = mimeType,
        size = size.toLong(),
        parents = parents.asList(),
        spaces = spaces.mapNotNull { Space.fromId(it) },
        id = id,
        name = name,
        trashed = trashed,
        modifiedTime = modifiedTime
    )

    suspend fun listFiles(
        q: Query? = null,
        spaces: List<Space>? = null,
    ): List<File> {
        val result = drive.files.list(jso {
            this.q = q?.q
            this.spaces = spaces?.joinToString(",")
            this.fields = "files($fileFields)"
        }).awaitResult().result()
        return result.files.map { it.toKt() }
    }

    private suspend fun uploadFile(
        uploadPath: String,
        uploadMethod: String,
        fileProperties: ReadonlyRecord<String, dynamic>,
        fileContentType: String,
        fileBody: String,
    ): File =
        client.multipartRequest(
            path = uploadPath,
            method = uploadMethod,
            params = recordOf(
                "uploadType" to "multipart"
            ),
            parts = listOf(
                GoogleClient.MultipartRequestPart(
                    contentType = "application/json",
                    body = JSON.stringify(fileProperties)
                ),
                GoogleClient.MultipartRequestPart(
                    contentType = fileContentType,
                    body = fileBody
                )
            )
        )


    suspend fun createFile(
        name: String,
        mimeType: String,
        parents: List<String>,
        content: String,
    ): File =
        uploadFile(
            uploadPath = "/upload/drive/v3/files",
            uploadMethod = "POST",
            fileProperties = recordOf(
                "name" to name,
                "mimeType" to mimeType,
                "parents" to parents.toTypedArray(),
            ),
            fileContentType = mimeType,
            fileBody = content,
        )

    suspend fun updateFile(
        id: String,
        mimeType: String,
        content: String,
        name: String? = null,
        parents: List<String>? = null,
    ): File {
        val properties = Record<String, dynamic>()
        if (name != null) properties["name"] = name
        if (parents != null) properties["parents"] = parents

        val fileProperties = Record<String, dynamic>()
        fileProperties["mimeType"] = mimeType
        if (name != null) fileProperties["name"] = name
        if (parents != null) fileProperties["parents"] = parents

        return uploadFile(
            uploadPath = "https://www.googleapis.com/upload/drive/v3/files/$id",
            uploadMethod = "PATCH",
            fileProperties = fileProperties,
            fileContentType = mimeType,
            fileBody = content
        )
    }

    suspend fun getFileMetadata(id: String): File? {
        return drive.files.get(jso {
            this.fileId = id
            this.fields = fileFields
        }).awaitResult().unsafeCast<JsGoogleDriveAPI.Files.File?>()?.toKt()
    }

    suspend fun getFileContent(id: String): String? {
        val result = drive.files.get(jso {
            this.fileId = id
            this.alt = "media"
        }).awaitResult()
        return result.body.unsafeCast<String?>()
    }

    suspend fun deleteFile(id: String) {
        drive.files.delete(jso {
            this.fileId = id
        }).awaitResult()
    }

//    suspend fun getFileContent(path: String): String {
//        val result = api.files.list(jso {
//            spaces = "appDataFolder"
//        }).await()
//        console.dir(result)
//        return ""
//    }

}