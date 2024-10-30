import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import utils.attr
import utils.elements
import utils.retry
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.xml.parsers.DocumentBuilderFactory


abstract class CreateGamesDataTask : DefaultTask() {

    init {
        group = "build"
    }

    @get:InputFiles
    val inputs = project.fileTree("${project.projectDir}/games") {
        include("**/game.yaml", "**/*.adoc")
    }

    @get:OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("games-data")

    @get:OutputDirectory
    val cacheDir = project.layout.buildDirectory.dir("cache-games-data")

    private val yamlLoader = Load(
        LoadSettings.builder()
            .setAllowDuplicateKeys(false)
            .build()
    )

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

    private val httpClient by lazy { HttpClient.newHttpClient() }

    data class BggGame(
        val item: Game.BggItem,
        val thumbnail: String,
        val image: String
    )

    private fun getBgg(bggId: Long, gameId: String): BggGame =
        downloadBggFile(
            url = "https://boardgamegeek.com/xmlapi2/thing?id=$bggId&stats=1",
            fileName = "$gameId.xml"
        ) { bytes ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(bytes))
            val domItem = document.elements("item").first()
            BggGame(
                item = Game.BggItem(
                    bggId = bggId,
                    names = domItem.elements("name").let { names ->
                        ( listOf(names.first { it.attr("type") == "primary" }) + names.filter { it.attr("type") == "alternate" } )
                            .map { it.attr("value") }
                    },
                    yearPublished = domItem.elements("yearpublished").first().attr("value").toInt(),
                    playingTime = domItem.elements("playingtime").first().attr("value").toInt(),
                    minAge = domItem.elements("minage").first().attr("value").toInt(),
                    rating = domItem.elements("statistics").first()
                        .elements("ratings").first()
                        .elements("average").first()
                        .attr("value").toDouble()
                ),
                thumbnail = domItem.elements("thumbnail").first().textContent,
                image = domItem.elements("image").first().textContent
            )
        }

    private fun <R> downloadBggFile(url: String, fileName: String, transform: (ByteArray) -> R): R {
        val cacheFile = cacheDir.get().asFile.resolve(fileName)
        val bytes =
            if (cacheFile.exists()) cacheFile.readBytes()
            else {
                httpClient.send(
                    HttpRequest.newBuilder(URI(url)).GET().build(),
                    BodyHandlers.ofByteArray()
                ).body()
            }
        val result = transform(bytes)
        if (!cacheFile.exists()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.writeBytes(bytes)
        }
        return result
    }

    @ExperimentalStdlibApi
    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun run() {
        outputDir.get().asFile.mkdirs()

        val games = inputs.files
            .filter { it.name == "game.yaml" }
            .sortedBy { it.parentFile.name }
            .map { yamlFile ->
                val dir = yamlFile.parentFile
                try {
                    print("${dir.name}..")
                    System.out.flush()
                    val map = yamlLoader.loadAllFromReader(yamlFile.bufferedReader()).first() as Map<String, Any>

                    val bggId = map["bggId"].toString().trim().toLong()

                    val bgg = try {
                        retry(50) {
                            print(".")
                            getBgg(bggId, dir.name)
                        }
                    } catch (e: Throwable) {
                        System.err.println("Error parsing https://boardgamegeek.com/xmlapi2/thing?id=$bggId&stats=1")
                        throw e
                    }

                    outputDir.get().file(dir.name).asFile.mkdirs()

                    val thumbnailExt = bgg.thumbnail.split(".").last()
                    val imageExt = bgg.image.split(".").last()

                    downloadBggFile(
                        url = bgg.thumbnail,
                        fileName = "${dir.name}-thumbnail.$thumbnailExt"
                    ) { bytes ->
                        outputDir.get().file("${dir.name}/thumbnail.${thumbnailExt}").asFile.outputStream().use { output ->
                            output.write(bytes)
                        }
                    }

                    downloadBggFile(
                        url = bgg.image,
                        fileName = "${dir.name}-image.$thumbnailExt"
                    ) { bytes ->
                        outputDir.get().file("${dir.name}/image.${imageExt}").asFile.outputStream().use { output ->
                            output.write(bytes)
                        }
                    }

                    val gamePlayerCount = when (val players = map["players"]) {
                        is String -> players.split(",").map { it.trim().toInt() }
                        is Number -> listOf(players.toInt())
                        else -> error("Bad player type")
                    }
                    val refs = dir.listFiles()!!
                        .mapNotNull { Regex("R-(.+)\\.png").matchEntire(it.name) }
                        .map { it.groupValues[1] }

                    val game = Game(
                        id = dir.name,
                        names = dir.listFiles()!!
                            .filter { it.extension == "adoc" }
                            .map { it.nameWithoutExtension to it.useLines { s -> s.first().removePrefix("= ") } }
                            .toMap(),
                        designers = Game.Designers(
                            authors = map["authors"]?.let {
                                (it as? String) ?: error("Bad authors")
                                it.split(",").map { it.trim() }
                            },
                            adaptedBy = map["adaptedBy"]?.let {
                                (it as? String) ?: error("Bad adaptedBy")
                                it.split(",").map { it.trim() }
                            },
                            tradition = (map["tradition"] as? String)?.trim(),
                        ),
                        types = (map["type"] as? List<String>) ?: error("Bad type."),
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
                        references = refs,
                        thumbnailExt = thumbnailExt,
                        imageExt = imageExt,
                        bgg = bgg.item,
                    )
                    println("OK")
                    game
                } catch (ex: Exception) {
                    throw RuntimeException("Could not game ${dir.name}.\n${ex.message}", ex)
                }
        }

        outputDir.get().file("games.json").asFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(games))
    }

}
