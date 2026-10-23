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

data class ForgeNode(
    val forgeCoordinate: String,
    val forgeDep: String,
    val javaVersion: Int,
    val accessTransformer: String,
    val tradeCycling: String,
    val packMeta: String,
)

fun legacyPack(format: Int): String = """
{
  "pack": {
    "description": "Villager Reroller",
    "pack_format": $format
  }
}
""".trimIndent() + "\n"

fun modernPack(major: Int, minor: Int): String = """
{
  "pack": {
    "description": "Villager Reroller",
    "min_format": [$major, $minor],
    "max_format": [$major, $minor]
  }
}
""".trimIndent() + "\n"

val minecraftVersion = project.name
val forgeNode = when (minecraftVersion) {
    "1.21.1" -> ForgeNode(
        "1.21.1-52.1.20",
        "52.1.20",
        21,
        "accesstransformer.cfg",
        "forge-1.21.1-1.0.23",
        legacyPack(34),
    )
    "1.21.11" -> ForgeNode(
        "1.21.11-61.2.4",
        "61.2.4",
        21,
        "accesstransformer-1.21.11.cfg",
        "forge-1.21.11-1.0.23",
        modernPack(75, 0),
    )
    "26.1.2" -> ForgeNode(
        "26.1.2-64.1.5",
        "64.1.5",
        25,
        "accesstransformer-26.1.cfg",
        "forge-1.0.23+26.1.2",
        modernPack(84, 0),
    )
    "26.2" -> ForgeNode(
        "26.2-65.1.5",
        "65.1.5",
        25,
        "accesstransformer-26.1.cfg",
        "forge-1.0.23+26.2",
        modernPack(88, 0),
    )
    "26.3" -> ForgeNode(
        "26.3-66.1.0",
        "66.1.0",
        25,
        "accesstransformer-26.3.cfg",
        "forge-1.0.23+26.3",
        modernPack(97, 1),
    )
    else -> null
}

if (forgeNode == null) {
    tasks.configureEach {
        if (name != "build") {
            enabled = false
        }
    }
    tasks.named("build") {
        group = "build"
        description = "This Forge node has no Minecraft Forge build yet."
        notCompatibleWithConfigurationCache("Refuses a Forge jar for an unmapped Minecraft version.")
        setDependsOn(emptyList<Any>())
        enabled = true
        val reason = "No Forge coordinate is configured for Minecraft $minecraftVersion."
        doFirst {
            throw GradleException(reason)
        }
    }
} else {
    val node = forgeNode
    apply(plugin = "java")
    apply(plugin = "net.minecraftforge.gradle")

    version = "1.0.0+$minecraftVersion"
    base.archivesName.set("villager_reroller-forge")
    group = "dev.quarkos"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(node.javaVersion))
        }
    }

    val minecraftExt = extensions.getByName("minecraft")
    val fg = extensions.getByName("fg")
    val forgeMinecraft = minecraftExt as net.minecraftforge.gradle.MinecraftExtensionForProject
    forgeMinecraft.mappings("official", minecraftVersion)
    // Stonecutter's chiseled tree does not contain this file at configuration time.
    // Point at the checked-in cfg so ForgeGradle's mavenizer actually applies it.
    val accessTransformer = rootProject.file("src/main/resources/META-INF/${node.accessTransformer}")
    forgeMinecraft.accessTransformers.from(accessTransformer)
    val runs = (minecraftExt as groovy.lang.GroovyObject).getProperty("runs")
        as org.gradle.api.NamedDomainObjectContainer<net.minecraftforge.gradle.SlimeLauncherOptions>
    val runDir = rootProject.layout.projectDirectory.dir("run/forge/$minecraftVersion")
    runs.configureEach {
        workingDir.set(runDir)
        systemProperty("forge.logging.console.level", "info")
    }
    val sourceSets = extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    val mainSource = sourceSets.named("main")
    // The Forge branch's 1.21.11 node is Stonecutter's active project, so it would
    // compile the shared Fabric sources. Every Forge node compiles the chiseled tree.
    val chiseledMain = layout.buildDirectory.dir("generated/stonecutter/main")
    gradle.projectsEvaluated {
        val main = mainSource.get()
        main.java.setSrcDirs(listOf(chiseledMain.get().dir("java").asFile))
        main.resources.setSrcDirs(listOf(chiseledMain.get().dir("resources").asFile))
        main.output.setResourcesDir(main.java.destinationDirectory)
    }
    tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
    tasks.named("processResources") { dependsOn("stonecutterGenerate") }
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
        add("implementation", forgeMinecraft.dependency("net.minecraftforge:forge:${node.forgeCoordinate}"))
        val tradeCycling = "maven.modrinth:trade-cycling:${node.tradeCycling}"
        add("compileOnly", tradeCycling)
        if (providers.gradleProperty("withTradeCycling").orNull == "true") {
            add("runtimeOnly", tradeCycling)
        }
        add("testImplementation", platform("org.junit:junit-bom:5.13.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    val forgePack = layout.buildDirectory.dir("generated/forge-pack")
    val writeForgePack = tasks.register("writeForgePack") {
        outputs.dir(forgePack)
        doLast {
            val file = forgePack.get().asFile.resolve("pack.mcmeta")
            file.parentFile.mkdirs()
            file.writeText(node.packMeta)
        }
    }

    tasks.named<ProcessResources>("processResources") {
        dependsOn(writeForgePack)
        val props = mapOf(
            "version" to "1.0.0+$minecraftVersion",
            "forge_dep" to node.forgeDep,
            "minecraft" to minecraftVersion,
        )
        filesMatching("META-INF/mods.toml") {
            expand(props)
        }
        exclude("fabric.mod.json")
        exclude("**/*.accesswidener")
        exclude("accesswidener/**")
        exclude("META-INF/accesstransformer.cfg")
        exclude("META-INF/accesstransformer-*.cfg")
        from(accessTransformer) {
            rename { "accesstransformer.cfg" }
            into("META-INF")
        }
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
        val glShim = rootProject.file("/tmp/libglx-srgb.so")
        if (minecraftVersion == "26.3" && glShim.exists()) {
            environment("LD_PRELOAD", glShim.absolutePath)
            environment("LIBGL_ALWAYS_SOFTWARE", "1")
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
