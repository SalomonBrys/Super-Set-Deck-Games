package data.filesystem

import js.array.ReadonlyArray
import js.objects.Record
import js.promise.Promise
import web.fs.FileSystemFileHandle

external interface FilePickerOptions {
    var excludeAcceptAllOption: Boolean?
    var id: Any?
    var startIn: Any? // String | FileSystemHandle
    var types: Array<Type>

    interface Type {
        var description: String?
        var accept: Record<String, Array<String>>
    }
}

external interface OpenFilePickerOptions : FilePickerOptions {
    var multiple: Boolean?
}

external fun showOpenFilePicker(
    options: OpenFilePickerOptions? = definedExternally
): Promise<ReadonlyArray<FileSystemFileHandle>>

external interface SaveFilePickerOptions : FilePickerOptions {
    var suggestedName: String?
}
external fun showSaveFilePicker(
    options: SaveFilePickerOptions? = definedExternally
): Promise<FileSystemFileHandle>
