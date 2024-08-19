import org.asciidoctor.*
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

@Suppress("LeakingThis")
abstract class AsciidoctorTask : DefaultTask() {

    init {
        group = "build"
    }

    @get:InputDirectory @get:Incremental
    abstract val inputDir: DirectoryProperty

    @get:Input @get:Optional
    abstract val backend: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @Suppress("DEPRECATION")
    protected val adocOptionsMap: Map<String, Any> get() = newOptionsBuilder().build().map()

    private val optionActions = ArrayList<Action<OptionsBuilder>>()

    fun options(action: Action<OptionsBuilder>) {
        optionActions.add(action)
    }

    private val attrActions = ArrayList<Action<AttributesBuilder>>()

    fun attrs(action: Action<AttributesBuilder>) {
        attrActions.add(action)
    }

    private fun newOptionsBuilder(): OptionsBuilder = Options.builder()
        .safe(SafeMode.UNSAFE)
        .backend(backend.get())
        .attributes(
            Attributes.builder()
                .apply { attrActions.forEach { it.execute(this) } }
                .build()
        )
        .apply { optionActions.forEach { it.execute(this) } }

    init {
        group = "build"
        backend.convention("html")
        outputDir.convention(project.layout.buildDirectory.dir(backend.map { "asciidoctor/$it/${inputDir.asFile.get().name}" }))
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        Asciidoctor.Factory.create().use { adoc ->
            inputChanges.getFileChanges(inputDir)
                .filter {  it.file.isFile && it.file.extension  == "adoc" }
                .forEach {
                    val file = it.file.relativeTo(inputDir.get().asFile)
                    print("$file...")
                    System.out.flush()
                    val output = if (file.parentFile != null) {
                        outputDir.get().asFile.resolve(file.parentFile.resolve(file.nameWithoutExtension + "." + backend.get()))
                    } else {
                        outputDir.get().asFile.resolve(file.nameWithoutExtension + "." + backend.get())
                    }
                    output.parentFile.mkdirs()
                    adoc.convertFile(
                        it.file,
                        newOptionsBuilder()
                            .toFile(output)
                            .build()
                    )
                    println("OK")
                }
        }
    }
}