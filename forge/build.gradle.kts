import org.gradle.api.tasks.JavaExec

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.minecraftforge.net/")
        // loom-back-compat adds a Loom classpath dependency to every Stonecutter node.
        maven("https://maven.fabricmc.net/")
    }
    dependencies {
        classpath("net.minecraftforge.gradle:net.minecraftforge.gradle.gradle.plugin:7.0.40")
    }
}

// Cloth Config publishes a Forge jar for 1.21.1 only. The other nodes stay in the
// tree so a later backport can fill them in, and they fail the build instead of
// writing a jar that pretends the dependency exists.
val minecraftVersion = project.name
val forgeSupported = minecraftVersion == "1.21.1"

if (!forgeSupported) {
    // Stonecutter still creates a Java project for the node. Do not compile it:
    // there is no ForgeGradle classpath, and the active 1.21.11 node would compile
    // the Fabric sources. Fail the lifecycle task with the dependency reason.
    tasks.configureEach {
        if (name != "build") {
            enabled = false
        }
    }
    tasks.named("build") {
        group = "build"
        description = "Refuses to ship a Forge jar without a real Cloth Config Forge build."
        notCompatibleWithConfigurationCache(
            "Refuses a Forge jar when Cloth Config has no Forge build for this Minecraft version."
        )
        setDependsOn(emptyList<Any>())
        enabled = true
        val reason = missingCloth(minecraftVersion)
        doFirst {
            throw GradleException(reason)
        }
    }
} else {
    apply(plugin = "java")
    apply(plugin = "net.minecraftforge.gradle")

    version = "1.0.0+$minecraftVersion"
    base.archivesName.set("villager_reroller-forge")
    group = "dev.quarkos"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    val minecraftExt = extensions.getByName("minecraft")
    val fg = extensions.getByName("fg")
    val forgeMinecraft = minecraftExt as net.minecraftforge.gradle.MinecraftExtensionForProject
    forgeMinecraft.mappings("official", minecraftVersion)
    // Stonecutter's chiseled tree does not contain this file at configuration time.
    // Point at the checked-in cfg so ForgeGradle's mavenizer actually applies it.
    forgeMinecraft.accessTransformers.from(
        rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    )
    val runs = (minecraftExt as groovy.lang.GroovyObject).getProperty("runs")
        as org.gradle.api.NamedDomainObjectContainer<net.minecraftforge.gradle.SlimeLauncherOptions>
    val runDir = rootProject.layout.projectDirectory.dir("run/forge/$minecraftVersion")
    runs.configureEach {
        workingDir.set(runDir)
        systemProperty("forge.logging.console.level", "info")
    }
    val mainSource = extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java).named("main")
    // FML scans each classpath entry on its own. Classes and mods.toml have to share one directory.
    gradle.projectsEvaluated {
        val main = mainSource.get()
        main.output.setResourcesDir(main.java.destinationDirectory)
    }
    runs.register("client") {
        mods {
            register("villager_reroller") {
                source(mainSource.get())
            }
        }
    }

    repositories {
        forgeMinecraft.mavenizer(this)
        maven((fg as net.minecraftforge.gradle.ForgeGradleExtension).forgeMaven)
        maven((fg as net.minecraftforge.gradle.ForgeGradleExtension).minecraftLibsMaven)
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
            filter { includeGroup("maven.modrinth") }
        }
    }

    dependencies {
        add("implementation", forgeMinecraft.dependency("net.minecraftforge:forge:1.21.1-52.1.20"))
        add("implementation", "maven.modrinth:cloth-config:15.0.140+forge")
        val tradeCycling = "maven.modrinth:trade-cycling:forge-1.21.1-1.0.23"
        add("compileOnly", tradeCycling)
        if (providers.gradleProperty("withTradeCycling").orNull == "true") {
            add("runtimeOnly", tradeCycling)
        }
        add("testImplementation", platform("org.junit:junit-bom:5.13.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    // Forge treats a mod resource pack without pack.mcmeta as a loading error and
    // skips the pack, so language and the icon never load. Fabric does not read this file.
    val forgePack = layout.buildDirectory.dir("generated/forge-pack")
    val writeForgePack = tasks.register("writeForgePack") {
        outputs.dir(forgePack)
        doLast {
            val file = forgePack.get().asFile.resolve("pack.mcmeta")
            file.parentFile.mkdirs()
            // pack_format 34 is Minecraft 1.21 / 1.21.1.
            file.writeText(
                """
                {
                  "pack": {
                    "description": "Villager Reroller",
                    "pack_format": 34
                  }
                }
                """.trimIndent() + "\n"
            )
        }
    }

    tasks.named<ProcessResources>("processResources") {
        dependsOn(writeForgePack)
        val props = mapOf("version" to "1.0.0+$minecraftVersion")
        filesMatching("META-INF/mods.toml") {
            expand(props)
        }
        exclude("fabric.mod.json")
        exclude("**/*.accesswidener")
        exclude("accesswidener/**")
        from(forgePack)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<JavaExec>().configureEach {
        val cycling = providers.gradleProperty("withTradeCycling").orNull == "true"
        val mode = if (cycling) "trade-cycling" else "plain"
        val artifacts = "/opt/cursor/artifacts/forge-$minecraftVersion/$mode"
        systemProperty("villager_reroller.artifacts", artifacts)
        systemProperty("villager_reroller.loader", "forge")
        systemProperty("villager_reroller.timeoutScale", "8")
        if (providers.gradleProperty("harness").orNull == "true") {
            systemProperty("villager_reroller.harness", "true")
        }
        val display = providers.gradleProperty("display").orNull
        val xauth = providers.gradleProperty("xauth").orNull
        if (!display.isNullOrBlank()) {
            environment("DISPLAY", display)
        }
        if (!xauth.isNullOrBlank()) {
            environment("XAUTHORITY", xauth)
        }
        if (name == "runClient") {
            doFirst {
                val eula = workingDir.resolve("eula.txt")
                eula.parentFile?.mkdirs()
                eula.writeText("eula=true\n")
            }
        }
    }
}

fun missingCloth(version: String): String = """
Cloth Config publishes no Forge build for Minecraft $version.
Modrinth queries for loader=forge and game version $version return no files.
shedaniel cloth-config-forge stops at 17.0.144, which targets 1.21.2/1.21.3.
NeoForge cloth builds exist for this Minecraft version, and this project targets Minecraft Forge.
Refusing to produce villager_reroller-forge-1.0.0+$version.jar.
""".trim()
