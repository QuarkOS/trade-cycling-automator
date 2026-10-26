import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.JavaExec
import net.neoforged.minecraftdependencies.MinecraftDistribution
import javax.inject.Inject

/**
 * NeoForge 20.2.93 publishes the userdev jar and a POM, but no Gradle module
 * metadata. ModDevGradle 2 selects that jar through the neoforge-moddev-bundle
 * capability, which later versions advertise and 20.2 does not. The jar layout
 * matches the later bundle (config.json, patches, joined.lzma).
 */
@CacheableRule
abstract class LegacyNeoForgeBundleRule : ComponentMetadataRule {
    @get:Inject
    abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) {
        val version = context.details.id.version
        val neoform = when (version) {
            "20.2.93" -> "1.20.2-20231019.002635"
            else -> return
        }
        // Same shape as the modDevBundle variant published from 20.4 onward.
        context.details.addVariant("modDevBundle") {
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, "data"))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            }
            withCapabilities {
                removeCapability("net.neoforged", "neoforge")
                addCapability("net.neoforged", "neoforge-moddev-bundle", version)
            }
            withFiles {
                addFile("neoforge-$version-userdev.jar")
            }
            withDependencies {
                add("net.neoforged:neoform:$neoform")
            }
        }
        // Maven derives a "sources" variant from the classifier jar and copies the POM's
        // Minecraft libraries onto it. NeoForm then looks for sources of those libraries.
        context.details.withVariant("sources") {
            withDependencies {
                clear()
            }
        }
        // Loader libraries NeoForm puts on its compile and runtime classpaths.
        // Taken from the 20.2.93 userdev config.json "libraries" list.
        addDependencyVariant(context, "modDevApiElements", Usage.JAVA_API, version)
        addDependencyVariant(context, "modDevRuntimeElements", Usage.JAVA_RUNTIME, version)
    }

    private fun addDependencyVariant(
        context: ComponentMetadataContext,
        variantName: String,
        usage: String,
        version: String,
    ) {
        context.details.addVariant(variantName) {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, usage))
                attribute(MinecraftDistribution.ATTRIBUTE, objects.named(MinecraftDistribution::class.java, MinecraftDistribution.CLIENT))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
            }
            withCapabilities {
                removeCapability("net.neoforged", "neoforge")
                addCapability("net.neoforged", "neoforge-dependencies", version)
            }
            withDependencies {
                USERDEV_LIBRARIES.forEach { add(it) }
            }
        }
    }

    companion object {
        private val USERDEV_LIBRARIES = listOf(
            "net.sf.jopt-simple:jopt-simple:5.0.4",
            "net.neoforged.fancymodloader:language-java:1.0.16",
            "net.neoforged:JarJarFileSystems:0.4.0",
            "net.neoforged:neoform:1.20.2-20231019.002635",
            "net.neoforged.fancymodloader:language-lowcode:1.0.16",
            "net.minecraftforge:unsafe:0.2.0",
            "net.neoforged:accesstransformers:10.0.1",
            "org.apache.maven:maven-artifact:3.8.5",
            "org.jline:jline-reader:3.12.1",
            "cpw.mods:modlauncher:10.0.9",
            "cpw.mods:securejarhandler:2.1.24",
            "com.google.j2objc:j2objc-annotations:1.3",
            "net.neoforged.fancymodloader:events:1.0.16",
            "com.electronwill.night-config:toml:3.6.4",
            "com.electronwill.night-config:core:3.6.4",
            "org.codehaus.plexus:plexus-utils:3.3.0",
            "org.jetbrains:annotations:22.0.0",
            "net.minecraftforge:srgutils:0.4.15",
            "commons-io:commons-io:2.11.0",
            "io.github.llamalad7:mixinextras-neoforge:0.3.1",
            "org.antlr:antlr4-runtime:4.13.1",
            "net.neoforged.fancymodloader:loader:1.0.16",
            "com.google.code.gson:gson:2.10",
            "org.openjdk.nashorn:nashorn-core:15.4",
            "com.machinezoo.noexception:noexception:1.7.1",
            "net.neoforged.fancymodloader:core:1.0.16",
            "com.google.errorprone:error_prone_annotations:2.11.0",
            "org.ow2.asm:asm:9.5",
            "cpw.mods:bootstraplauncher:1.1.2",
            "org.apache.logging.log4j:log4j-core:2.19.0",
            "net.neoforged:JarJarMetadata:0.4.0",
            "org.ow2.asm:asm-tree:9.5",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "org.ow2.asm:asm-commons:9.5",
            "org.ow2.asm:asm-util:9.5",
            "org.slf4j:slf4j-api:2.0.9",
            "net.neoforged.accesstransformers:at-modlauncher:10.0.1",
            "com.google.guava:failureaccess:1.0.1",
            "com.google.guava:guava:31.1-jre",
            "net.neoforged:bus:7.2.0",
            "net.neoforged:mergetool:2.0.0:api",
            "org.checkerframework:checker-qual:3.12.0",
            "org.jline:jline-terminal:3.12.1",
            "net.neoforged:neoforgespi:8.0.0",
            "org.apache.logging.log4j:log4j-api:2.19.0",
            "net.minecrell:terminalconsoleappender:1.2.0",
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.mojang:logging:1.1.1",
            "net.neoforged:coremods:6.0.2",
            "org.spongepowered:mixin:0.8.5",
            "org.apache.commons:commons-lang3:3.12.0",
            "net.neoforged.fancymodloader:earlydisplay:1.0.16",
            "net.jodah:typetools:0.6.3",
            "net.neoforged:JarJarSelector:0.4.0",
            "org.ow2.asm:asm-analysis:9.5",
            "net.neoforged.fancymodloader:language-minecraft:1.0.16",
        )
    }
}

/**
 * NeoForge builds for Minecraft 1.20.2 through 26.3. ModDevGradle supplies the
 * userdev runtime. Production jars stay on official names, so there is no
 * reobfuscation step. The sources are the shared tree, chiseled with the
 * NeoForge branch parameters.
 */

buildscript {
    repositories {
        maven("https://maven.neoforged.net/releases")
        mavenCentral()
        gradlePluginPortal()
        // loom-back-compat adds a Loom classpath dependency to every Stonecutter node.
        maven("https://maven.fabricmc.net/")
    }
    dependencies {
        classpath("net.neoforged:moddev-gradle:2.0.148")
    }
}

data class NeoForgeNode(
    val neoVersion: String,
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

fun neoDepRange(neoVersion: String): String {
    val parts = neoVersion.substringBefore('-').split('.')
    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    return "[$major.$minor,${major}.${minor + 1})"
}

val minecraftVersion = project.name
val neoForgeNode = when (minecraftVersion) {
    "1.20.2" -> NeoForgeNode("20.2.93", 17, "accesstransformer-1.20.2.cfg", "neoforge-1.20.2-1.0.8", legacyPack(18))
    "1.20.4" -> NeoForgeNode("20.4.253", 17, "accesstransformer-1.20.4.cfg", "neoforge-1.20.4-1.0.11", legacyPack(22))
    "1.20.6" -> NeoForgeNode("20.6.144", 21, "accesstransformer-1.20.6.cfg", "neoforge-1.20.6-1.0.11", legacyPack(32))
    "1.21" -> NeoForgeNode("21.0.167", 21, "accesstransformer.cfg", "neoforge-1.21.1-1.0.23", legacyPack(34))
    "1.21.1" -> NeoForgeNode("21.1.265", 21, "accesstransformer.cfg", "neoforge-1.21.1-1.0.23", legacyPack(34))
    "1.21.3" -> NeoForgeNode("21.3.100", 21, "accesstransformer-1.21.3.cfg", "neoforge-1.21.3-1.0.15", legacyPack(42))
    "1.21.4" -> NeoForgeNode("21.4.161", 21, "accesstransformer.cfg", "neoforge-1.21.4-1.0.18", legacyPack(46))
    "1.21.5" -> NeoForgeNode("21.5.101", 21, "accesstransformer.cfg", "neoforge-1.21.5-1.0.18", legacyPack(55))
    // Last published NeoForge build. 1.21.6 never received a stable release.
    "1.21.6" -> NeoForgeNode("21.6.20-beta", 21, "accesstransformer.cfg", "neoforge-1.21.6-1.0.19", legacyPack(63))
    // Last published NeoForge build. 1.21.9 never received a stable release.
    "1.21.9" -> NeoForgeNode("21.9.16-beta", 21, "accesstransformer.cfg", "neoforge-1.21.9-1.0.19", modernPack(69, 0))
    "1.21.11" -> NeoForgeNode("21.11.48", 21, "accesstransformer-1.21.11.cfg", "neoforge-1.21.11-1.0.23", modernPack(75, 0))
    "26.1.2" -> NeoForgeNode("26.1.2.124", 25, "accesstransformer-26.1.cfg", "neoforge-1.0.23+26.1.2", modernPack(84, 0))
    "26.2" -> NeoForgeNode("26.2.0.93", 25, "accesstransformer-26.1.cfg", "neoforge-1.0.23+26.2", modernPack(88, 0))
    "26.3" -> NeoForgeNode("26.3.0.99", 25, "accesstransformer-26.3.cfg", "neoforge-1.0.23+26.3", modernPack(97, 1))
    else -> null
}

if (neoForgeNode == null) {
    tasks.configureEach {
        if (name != "build") {
            enabled = false
        }
    }
    tasks.named("build") {
        group = "build"
        description = "This NeoForge node has no NeoForge build yet."
        notCompatibleWithConfigurationCache("Refuses a NeoForge jar for an unmapped Minecraft version.")
        setDependsOn(emptyList<Any>())
        enabled = true
        val reason = "No NeoForge coordinate is configured for Minecraft $minecraftVersion."
        doFirst {
            throw GradleException(reason)
        }
    }
} else {
    val node = neoForgeNode
    apply(plugin = "java")
    apply(plugin = "net.neoforged.moddev")

    version = "1.0.0+$minecraftVersion"
    base.archivesName.set("villager_reroller-neoforge")
    group = "dev.quarkos"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(node.javaVersion))
        }
    }

    val neoForge = extensions.getByType(net.neoforged.moddevgradle.dsl.NeoForgeExtension::class.java)
    neoForge.setVersion(node.neoVersion)
    neoForge.validateAccessTransformers.set(false)
    val accessTransformer = rootProject.file("src/main/resources/META-INF/${node.accessTransformer}")
    // NeoForm recompiles Minecraft after applying access transformers. Widening
    // Screen.addRenderableWidget without the CreateWorldScreen override makes javac
    // reject the protected override. Production FML applies the original file to
    // bytecode, where that override stays legal, so the jar keeps the shared cfg.
    val devAccessTransformer = layout.buildDirectory.file("generated/neoforge-at/accesstransformer.cfg")
    val devAccessTransformerText = run {
        val extra = "public net.minecraft.client.gui.screens.worldselection.CreateWorldScreen addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"
        val base = accessTransformer.readText().trimEnd()
        if (base.contains("Screen addRenderableWidget")) "$base\n$extra\n" else "$base\n"
    }
    val devAccessTransformerFile = devAccessTransformer.get().asFile
    devAccessTransformerFile.parentFile.mkdirs()
    devAccessTransformerFile.writeText(devAccessTransformerText)
    neoForge.setAccessTransformers(devAccessTransformerFile)

    val sourceSets = extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    val mainSource = sourceSets.named("main")
    val chiseledMain = layout.buildDirectory.dir("generated/stonecutter/main")
    gradle.projectsEvaluated {
        val main = mainSource.get()
        main.java.setSrcDirs(listOf(chiseledMain.get().dir("java").asFile))
        main.resources.setSrcDirs(listOf(chiseledMain.get().dir("resources").asFile))
        main.output.setResourcesDir(main.java.destinationDirectory)
    }
    tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
    tasks.named("processResources") { dependsOn("stonecutterGenerate") }

    val runDir = rootProject.layout.projectDirectory.dir("run/neoforge/$minecraftVersion")
    neoForge.mods.register("villager_reroller") {
        sourceSet(mainSource.get())
    }
    neoForge.runs.register("client") {
        client()
        gameDirectory.set(runDir)
    }

    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
            filter { includeGroup("maven.modrinth") }
        }
    }

    val tradeCycling = "maven.modrinth:trade-cycling:${node.tradeCycling}"
    dependencies {
        components {
            withModule("net.neoforged:neoforge", LegacyNeoForgeBundleRule::class.java)
        }
        add("compileOnly", tradeCycling)
        if (minecraftVersion == "1.20.2") {
            // 20.4+ reaches this through neoform-dependencies. Adding it on the
            // NeoForge variant also puts it on NeoForm's own classpath, where
            // Minecraft's strict slf4j pin fights the loader libraries.
            val minecraftLibraries = "net.neoforged:minecraft-dependencies:1.20.2"
            add("compileOnly", minecraftLibraries)
            add("runtimeOnly", minecraftLibraries)
            components {
                all {
                    allVariants {
                        withDependencies {
                            removeIf { dep ->
                                dep.group == "org.slf4j" && dep.name == "slf4j-api" &&
                                    dep.versionConstraint.strictVersion != "2.0.7"
                            }
                        }
                    }
                }
            }
        }
        if (providers.gradleProperty("withTradeCycling").orNull == "true") {
            add("runtimeOnly", tradeCycling)
        }
    }

    val legacyDeps = minecraftVersion == "1.20.2"
    fun requirement(required: Boolean): String = if (legacyDeps) {
        "mandatory = ${if (required) "true" else "false"}"
    } else {
        "type = \"${if (required) "required" else "optional"}\""
    }

    val tomlName = if (minecraftVersion == "1.20.2" || minecraftVersion == "1.20.4") {
        "mods.toml"
    } else {
        "neoforge.mods.toml"
    }
    val modsToml = """
modLoader = "javafml"
loaderVersion = "*"
license = "MIT"
issueTrackerURL = "https://github.com/QuarkOS/trade-cycling-automator/issues"

[[mods]]
modId = "villager_reroller"
version = "1.0.0+$minecraftVersion"
displayName = "Villager Reroller"
authors = "Emilio Schwaiger"
description = '''Lists every trade a villager can roll, with vanilla emerald ranges, and breaks the job site until a chosen offer is cheap enough. If Trade Cycling is installed, the list can use that mod's cycle action instead.'''
displayTest = "IGNORE_ALL_VERSION"

[[accessTransformers]]
file = "META-INF/accesstransformer.cfg"

[[dependencies.villager_reroller]]
modId = "neoforge"
${requirement(true)}
versionRange = "${neoDepRange(node.neoVersion)}"
ordering = "NONE"
side = "CLIENT"

[[dependencies.villager_reroller]]
modId = "minecraft"
${requirement(true)}
versionRange = "[$minecraftVersion]"
ordering = "NONE"
side = "CLIENT"

[[dependencies.villager_reroller]]
modId = "trade_cycling"
${requirement(false)}
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "CLIENT"
""".trimIndent() + "\n"

    tasks.named<ProcessResources>("processResources") {
        exclude("fabric.mod.json")
        exclude("**/*.accesswidener")
        exclude("accesswidener/**")
        exclude("META-INF/mods.toml")
        exclude("META-INF/neoforge.mods.toml")
        exclude("META-INF/accesstransformer.cfg")
        exclude("META-INF/accesstransformer-*.cfg")
        from(accessTransformer) {
            rename { "accesstransformer.cfg" }
            into("META-INF")
        }
        val meta = modsToml
        val pack = node.packMeta
        val tomlFileName = tomlName
        inputs.property("neoforge_meta", meta)
        inputs.property("pack_meta", pack)
        doLast {
            val root = outputs.files.files.single { it.isDirectory }
            root.mkdirs()
            val metaDir = root.resolve("META-INF")
            metaDir.mkdirs()
            metaDir.resolve(tomlFileName).writeText(meta)
            root.resolve("pack.mcmeta").writeText(pack)
        }
    }

    fun copyModsForOldLocator(version: String): Boolean = version == "1.20.2" || version == "1.20.4"

    tasks.withType<JavaExec>().configureEach {
        if (name == "runClient" && copyModsForOldLocator(minecraftVersion)) {
            val modsDir = runDir.dir("mods").asFile
            val builtJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
            dependsOn("jar")
            doFirst {
                modsDir.mkdirs()
                modsDir.listFiles()
                    ?.filter { it.name.startsWith("villager_reroller") || it.name.startsWith("trade-cycling") }
                    ?.forEach { it.delete() }
                val built = builtJar.get().asFile
                built.copyTo(modsDir.resolve(built.name), overwrite = true)
                if (providers.gradleProperty("withTradeCycling").orNull == "true") {
                    configurations.getByName("runtimeClasspath").files
                        .filter { it.name.contains("trade-cycling") && it.isFile }
                        .forEach { it.copyTo(modsDir.resolve(it.name), overwrite = true) }
                }
            }
        }
        val cycling = providers.gradleProperty("withTradeCycling").orNull == "true"
        val mode = if (cycling) "trade-cycling" else "plain"
        val artifacts = "/opt/cursor/artifacts/neoforge-$minecraftVersion/$mode"
        systemProperty("villager_reroller.artifacts", artifacts)
        systemProperty("villager_reroller.loader", "neoforge")
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
                val eula = runDir.asFile.resolve("eula.txt")
                eula.parentFile?.mkdirs()
                eula.writeText("eula=true\n")
            }
        }
    }
}
