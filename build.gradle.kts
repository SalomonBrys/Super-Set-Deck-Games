plugins {
    kotlin("multiplatform") version "1.7.10" apply false
    kotlin("plugin.serialization") version "1.7.10" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

tasks.register<CreateGamesJsonTask>("createGamesJson")
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

tasks.register<AsciidoctorTask>("asciidoctorAbout") {
    inputDir.set(projectDir.resolve("about"))
    attrs {
        icons("font")
    }
    options {
        headerFooter(false)
    }
}

tasks.register<Sync>("publish") {
    group = "publish"
    dependsOn(":App:jsBrowserDistribution")
    from("$rootDir/App/build/distributions")
    into("$rootDir/docs")
}
