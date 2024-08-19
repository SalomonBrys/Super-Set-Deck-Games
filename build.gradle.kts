plugins {
    kotlin("multiplatform") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

tasks.register<CreateGamesDataTask>("createGamesData")
tasks.register<AsciidoctorTask>("asciidoctorGames") {
    inputDir.set(projectDir.resolve("games"))
    attrs {
        icons("font")
        math("asciimath")
    }
    options {
        headerFooter(false)
    }
}

tasks.register<AsciidoctorTask>("asciidoctorDocs") {
    inputDir.set(projectDir.resolve("docs"))
    attrs {
        icons("font")
    }
    options {
        headerFooter(false)
    }
}
