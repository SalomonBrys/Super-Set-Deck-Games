import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


abstract class CreateResourceListTask : DefaultTask() {

    init {
        group = "build"
    }

    @get:Input
    val dirs = ArrayList<File>()

    @get:InputFiles
    val files get() = dirs.map { project.fileTree(it).asFileTree } .reduce { l, r -> l + r }

    @get:OutputFile
    val output = project.layout.buildDirectory.file("resourcesList/resources.txt")

    @ExperimentalStdlibApi
    @TaskAction
    fun run() {
        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        val files = dirs
            .flatMap { dir ->
                project.fileTree(dir).map { it.relativeTo(dir) }
            }
            .toSet()
            .minus(File("index.html"))
            .minus(File("CNAME"))
            .plus(File("${project.name}.js"))
            .plus(File("${project.name}.js.map"))
            .sorted()
            .toList()
        outputFile.writer().buffered().use { writer ->
            writer.appendLine(SimpleDateFormat("YYYY-MM-dd_HH:mm:ss").format(Date()))
            files.forEach { writer.appendLine(it.path) }
        }
    }

}
