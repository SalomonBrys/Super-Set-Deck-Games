plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose") version "1.2.0"
}

val copyAllResources = tasks.register("copyAllResources") {
    group = "build"
}

val copyAboutResources = tasks.register<Copy>("copyAboutResources") {
    group = "resources"
    from(rootDir.resolve("about")) {
        include("**/*.png")
        include("**/*.pdf")
    }
    into(buildDir.resolve("generatedResources/about"))
}
copyAllResources.configure { dependsOn(copyAboutResources) }

val copyGamesResources = tasks.register<Copy>("copyGamesResources") {
    group = "resources"
    from(rootDir.resolve("games")) {
        include("**/*.png")
        include("**/*.pdf")
    }
    into(buildDir.resolve("generatedResources/games"))
}
copyAllResources.configure { dependsOn(copyGamesResources) }

val copyGamesJson = tasks.register<Copy>("copyGamesJson") {
    group = "resources"
    dependsOn(":createGamesJson")
    from(rootDir.resolve("build/games-json/games.json"))
    into(buildDir.resolve("generatedResources/games"))
}
copyAllResources.configure { dependsOn(copyGamesJson) }

val copyAsciidoctor = tasks.register<Copy>("copyAsciidoctor") {
    group = "resources"
    dependsOn(":asciidoctorGames", ":asciidoctorAbout")
    from(rootDir.resolve("build/asciidoctor/html"))
    into(buildDir.resolve("generatedResources"))
}
copyAllResources.configure { dependsOn(copyAsciidoctor) }

val copyServiceWorker = tasks.register<Copy>("copyServiceWorker") {
    dependsOn(":ServiceWorker:jsBrowserProductionWebpack")
    group = "resources"
    from(rootDir.resolve("ServiceWorker/build/distributions"))
    into(buildDir.resolve("generatedResources"))
}
copyAllResources.configure { dependsOn(copyServiceWorker) }

val generateResourceList = tasks.register<CreateResourceListTask>("generateResourceList") {
    dependsOn(copyAllResources)
    dirs += file("$projectDir/src/jsMain/resources")
    dirs += file("$buildDir/generatedResources")
}

kotlin {
    js(IR) {
        useCommonJs()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }

        @Suppress("UnstableApiUsage")
        (tasks[compilations["main"].processResourcesTaskName] as ProcessResources).apply {
            dependsOn(copyAllResources, generateResourceList)
            from(buildDir.resolve("generatedResources"))
            from(buildDir.resolve("resourcesList"))
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.web.svg)
                implementation(compose.runtime)

                implementation("dev.petuska:kmdc:0.0.5")
                implementation("dev.petuska:kmdcx:0.0.5")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

                implementation("app.softwork:routing-compose:0.2.9")

                implementation(devNpm("sass-loader", "^13.0.0"))
                implementation(devNpm("sass", "^1.52.1"))

                implementation(npm("@uriopass/nosleep.js", "0.12.1"))
                implementation(npm("qrcode", "1.5.1"))

                implementation(npm("mathjax-full", "3.2.2"))
//                implementation(npm("mathjax", "2.7.9"))
            }
        }

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("org.jetbrains.compose.web.ExperimentalComposeWebApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}
