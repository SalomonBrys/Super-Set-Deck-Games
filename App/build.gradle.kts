plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
    into(layout.buildDirectory.dir("generatedResources/about"))
}
copyAllResources.configure { dependsOn(copyAboutResources) }

val copyGamesResources = tasks.register<Copy>("copyGamesResources") {
    group = "resources"
    from(rootDir.resolve("games")) {
        include("**/*.png")
        include("**/*.pdf")
    }
    into(layout.buildDirectory.dir("generatedResources/games"))
}
copyAllResources.configure { dependsOn(copyGamesResources) }

val copyGamesData = tasks.register<Copy>("copyGamesData") {
    group = "resources"
    dependsOn(":createGamesData")
    from(rootDir.resolve("build/games-data"))
    into(layout.buildDirectory.dir("generatedResources/games"))
}
copyAllResources.configure { dependsOn(copyGamesData) }

val copyAsciidoctor = tasks.register<Copy>("copyAsciidoctor") {
    group = "resources"
    dependsOn(":asciidoctorGames", ":asciidoctorDocs")
    from(rootDir.resolve("build/asciidoctor/html"))
    into(layout.buildDirectory.dir("generatedResources"))
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
    dirs += layout.projectDirectory.dir("src/jsMain/resources").asFile
    dirs += layout.buildDirectory.dir("generatedResources").get().asFile
}

kotlin {
    js(IR) {
        useCommonJs()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
        }

        tasks.named<ProcessResources>(compilations["main"].processResourcesTaskName).configure {
            dependsOn(copyAllResources, generateResourceList)
            from(layout.buildDirectory.dir("generatedResources"))
            from(layout.buildDirectory.dir("resourcesList"))
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:1.0.0-pre.791"))

                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-js")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-preact-signals-react")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-material")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons-material")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

                implementation(npm("react-image-gallery", "1.3.0"))
                implementation(npm("qrcode", "1.5.4"))
                implementation(npm("@uriopass/nosleep.js", "0.12.2"))

                implementation(npm("mathjax-full", "3.2.2"))

//                implementation(npm("module.google-oauth-gsi", "4.0.1"))
//                implementation(npm("@googleapis/drive", "8.13.0"))
            }
        }
    }
}
