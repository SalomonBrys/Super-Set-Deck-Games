import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File


abstract class CreateGamesJsonTask : DefaultTask() {

    init {
        group = "build"
    }

    @get:InputFiles
    val inputs = project.fileTree("${project.projectDir}/games") {
        include("**/game.yaml", "**/*.adoc")
    }

    @get:OutputFile
    val output = project.buildDir.resolve("games-json/games.json")

    private val yamlLoader = Load(
        LoadSettings.builder()
            .setAllowDuplicateKeys(false)
            .build()
    )

    private fun File.getRefCards(): List<String> =
        listFiles()!!
            .mapNotNull { Regex("R-(.+)\\.png").matchEntire(it.name) }
            .map { it.groupValues[1] }

    private fun String.expandCardValues(): List<String> =
        Regex("([0-9]+)-([0-9]+)").matchEntire(this)?.let { match ->
            IntRange(match.groupValues[1].toInt(), match.groupValues[2].toInt()).map { it.toString() }
        } ?: Regex("([0-9a-zA-Z]+)\\*([0-9]+)").matchEntire(this)?.let { match ->
            Array(match.groupValues[2].toInt()) { match.groupValues[1] }.toList()
        } ?: listOf(this)

    private fun String.expandValues(): List<Int> =
        Regex("([0-9]+)-([0-9]+)").matchEntire(this)?.let { match ->
            IntRange(match.groupValues[1].toInt(), match.groupValues[2].toInt()).toList()
        } ?: split(",").map { it.trim().toInt() }

    @ExperimentalStdlibApi
    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun run() {
        val games = inputs.files
            .filter { it.name == "game.yaml" }
            .map { yamlFile ->
                val dir = yamlFile.parentFile
                try {
                    val map = yamlLoader.loadAllFromReader(yamlFile.bufferedReader()).first() as Map<String, Any>

                    val gamePlayerCount = when (val players = map["players"]) {
                        is String -> players.split(",").map { it.trim().toInt() }
                        is Number -> listOf(players.toInt())
                        else -> error("Bad player type")
                    }
                    val refs = dir.listFiles()!!
                        .mapNotNull { Regex("R-(.+)\\.png").matchEntire(it.name) }
                        .map { it.groupValues[1] }

                    Game(
                        id = dir.name,
                        names = dir.listFiles()!!
                            .filter { it.extension == "adoc" }
                            .map { it.nameWithoutExtension to it.useLines { s -> s.first().removePrefix("= ") } }
                            .toMap(),
                        types = (map["type"] as? List<String>) ?: error("Bad type type."),
                        playerCount = gamePlayerCount,
                        cards = (map["cards"] as Map<String, Map<String, Map<String, *>>>).mapValues { (_, value) ->
                            value.map { (key, value) ->
                                if (!key.endsWith("-players")) error("Bad card players key")
                                val players = key.removeSuffix("-players")
                                players to Game.P2C(
                                    players = if (players == "all") gamePlayerCount else players.expandValues(),
                                    cards = value.mapValues { (_, cards) ->
                                        cards.toString().split(",").map { it.trim() }
                                            .flatMap { it.expandCardValues() }
                                            .groupingBy { it }
                                            .eachCount()
                                    }
                                )
                            }.toMap()
                        },
                        references = refs
                    )
                } catch (ex: Exception) {
                    throw RuntimeException("Could not game ${dir.name}.\n${ex.message}", ex)
                }
        }

        output.writeText(Gson().toJson(games))
    }

}
