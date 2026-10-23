import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.util.jar.JarOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Forge's official-mapped userdev runs Minecraft under mojmap names. Trade Cycling
 * jars built for 1.20.4 and older still contain SRG bytecode plus an SRG refmap.
 * Mixin then looks for f_/m_ members the runtime already renamed. This task remaps
 * that jar to mojmap and drops the refmap so the mojmap mixin annotations apply.
 * Production installs keep using the original Modrinth jar.
 */
abstract class TradeCyclingDevRuntimeTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tradeCyclingJars: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fartJars: ConfigurableFileCollection

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val forgeJar: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val officialToSrg: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun remap() {
        val input = tradeCyclingJars.singleFile
        val output = outputJar.get().asFile
        output.parentFile.mkdirs()
        if (!srgRefmap(input)) {
            input.copyTo(output, overwrite = true)
            return
        }
        if (!officialToSrg.isPresent || !forgeJar.isPresent) {
            throw GradleException("Trade Cycling ${input.name} uses an SRG refmap, but no official-to-SRG mapping is cached for this Forge version.")
        }
        val work = temporaryDir
        val reversed = work.resolve("srg-to-official.tsrg")
        val srgLibrary = work.resolve("forge-srg.jar")
        val remapped = work.resolve("remapped.jar")
        reverseTsrg(officialToSrg.get().asFile, reversed)
        fart(
            "--input", forgeJar.get().asFile.absolutePath,
            "--output", srgLibrary.absolutePath,
            "--map", officialToSrg.get().asFile.absolutePath,
            "--threads", "2",
        )
        fart(
            "--input", input.absolutePath,
            "--output", remapped.absolutePath,
            "--map", reversed.absolutePath,
            "--lib", srgLibrary.absolutePath,
            "--threads", "2",
        )
        stripRefmap(remapped, output)
    }

    private fun fart(vararg args: String) {
        execOperations.javaexec {
            classpath = fartJars
            mainClass.set("net.minecraftforge.fart.Main")
            this.args(args.toList())
        }
    }

    private fun srgRefmap(jar: File): Boolean {
        ZipFile(jar).use { zip ->
            val entry = zip.getEntry("trade_cycling.refmap.json") ?: return false
            val text = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            return Regex("""\b[fm]_\d+_""").containsMatchIn(text)
        }
    }

    private fun stripRefmap(input: File, output: File) {
        ZipFile(input).use { zip ->
            JarOutputStream(output.outputStream()).use { out ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory || entry.name == "trade_cycling.refmap.json") {
                        continue
                    }
                    var bytes = zip.getInputStream(entry).readBytes()
                    if (entry.name == "trade_cycling.mixins.json") {
                        bytes = bytes.toString(Charsets.UTF_8)
                            .replace(Regex(""""refmap"\s*:\s*"trade_cycling\.refmap\.json"\s*,\s*"""), "")
                            .toByteArray()
                    }
                    out.putNextEntry(ZipEntry(entry.name))
                    out.write(bytes)
                    out.closeEntry()
                }
            }
        }
    }

    private fun reverseTsrg(gzipped: File, dest: File) {
        GZIPInputStream(gzipped.inputStream()).bufferedReader().use { reader ->
            dest.bufferedWriter().use { writer ->
                reader.lineSequence().forEachIndexed { index, line ->
                    if (index == 0) {
                        writer.appendLine(line)
                        return@forEachIndexed
                    }
                    val tabs = line.takeWhile { it == '\t' }.length
                    val rest = line.trim()
                    if (rest.isEmpty() || rest == "static") {
                        writer.appendLine(line)
                        return@forEachIndexed
                    }
                    val parts = rest.split(' ')
                    val reversed = when {
                        tabs == 0 && parts.size == 2 -> "${parts[1]} ${parts[0]}"
                        tabs == 1 && parts.size == 2 -> "${parts[1]} ${parts[0]}"
                        tabs == 1 && parts.size == 3 -> "${parts[2]} ${parts[1]} ${parts[0]}"
                        tabs > 1 && parts.size >= 3 && parts[0].all { it.isDigit() } ->
                            "${parts[0]} ${parts[2]} ${parts[1]}"
                        else -> rest
                    }
                    writer.append("\t".repeat(tabs))
                    writer.appendLine(reversed)
                }
            }
        }
    }
}

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
    "1.21.4" -> ForgeNode(
        "1.21.4-54.1.22",
        "54.1.22",
        21,
        "accesstransformer.cfg",
        "forge-1.21.4-1.0.18",
        legacyPack(46),
    )
    "1.21.5" -> ForgeNode(
        "1.21.5-55.1.18",
        "55.1.18",
        21,
        "accesstransformer.cfg",
        "forge-1.21.5-1.0.18",
        legacyPack(55),
    )
    "1.21.6" -> ForgeNode(
        "1.21.6-56.0.9",
        "56.0.9",
        21,
        "accesstransformer.cfg",
        "forge-1.21.6-1.0.19",
        legacyPack(63),
    )
    "1.21.9" -> ForgeNode(
        "1.21.9-59.0.5",
        "59.0.5",
        21,
        "accesstransformer.cfg",
        "forge-1.21.9-1.0.19",
        modernPack(69, 0),
    )
    // 50.0.8 is the last 1.20.6 build whose payload context still has getDirection().
    // Trade Cycling 1.0.11 calls that method, so a newer Forge disconnects the client on the first cycle.
    "1.20.4" -> ForgeNode(
        "1.20.4-49.2.13",
        "49.2.13",
        17,
        "accesstransformer-1.20.4.cfg",
        "forge-1.20.4-1.0.11",
        legacyPack(22),
    )
    "1.20.6" -> ForgeNode(
        "1.20.6-50.0.8",
        "50.0.8",
        21,
        "accesstransformer-1.20.6.cfg",
        "forge-1.20.6-1.0.11",
        legacyPack(32),
    )
    "1.21" -> ForgeNode(
        "1.21-51.0.33",
        "51.0.33",
        21,
        "accesstransformer.cfg",
        "forge-1.21.1-1.0.23",
        legacyPack(34),
    )
    "1.21.3" -> ForgeNode(
        "1.21.3-53.1.16",
        "53.1.16",
        21,
        "accesstransformer-1.21.3.cfg",
        "forge-1.21.3-1.0.15",
        legacyPack(42),
    )
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
        maven("https://maven.minecraftforge.net/")
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
            filter { includeGroup("maven.modrinth") }
        }
    }

    // Forge 1.21 (modlauncher 10.2.1) and Forge 1.20.6-50.0.8 require the module
    // name jopt.simple. Their poms pull jopt-simple 6.0-alpha-3, whose
    // Automatic-Module-Name is joptsimple, so the launcher never sees the name
    // derived from jopt-simple-5.0.4.jar. 5.0.4 is the API those launchers call.
    if (minecraftVersion == "1.21" || minecraftVersion == "1.20.6") {
        configurations.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "net.sf.jopt-simple" && requested.name == "jopt-simple") {
                    useVersion("5.0.4")
                    because("this Forge build's modlauncher requires the jopt.simple module name")
                }
            }
        }
    }

    val tradeCycling = "maven.modrinth:trade-cycling:${node.tradeCycling}"
    val tradeCyclingJars = configurations.create("tradeCyclingJars") {
        isCanBeConsumed = false
        isTransitive = false
    }
    val fartTool = configurations.create("fartTool") {
        isCanBeConsumed = false
        isTransitive = true
    }
    val tradeCyclingDevJar = layout.buildDirectory.file("generated/trade-cycling-devruntime.jar")
    val officialToSrg = File(System.getProperty("user.home"), ".gradle/caches/minecraftforge/forgegradle/mavenizer/caches/mcp")
        .walkTopDown()
        .firstOrNull { it.isFile && it.name == "official-$minecraftVersion-obf.tsrg.gz" }
    val tradeCyclingDevRuntime = tasks.register<TradeCyclingDevRuntimeTask>("tradeCyclingDevRuntime") {
        this.tradeCyclingJars.from(tradeCyclingJars)
        fartJars.from(fartTool)
        if (officialToSrg != null) {
            forgeJar.set(rootProject.file(".gradle/mavenizer/repo/net/minecraftforge/forge/${node.forgeCoordinate}/forge-${node.forgeCoordinate}.jar"))
            this.officialToSrg.set(officialToSrg)
        }
        outputJar.set(tradeCyclingDevJar)
    }

    dependencies {
        add("implementation", forgeMinecraft.dependency("net.minecraftforge:forge:${node.forgeCoordinate}"))
        add("compileOnly", tradeCycling)
        add("tradeCyclingJars", tradeCycling)
        add("fartTool", "net.minecraftforge:ForgeAutoRenamingTool:1.1.2")
        if (providers.gradleProperty("withTradeCycling").orNull == "true") {
            add("runtimeOnly", files(tradeCyclingDevJar).builtBy(tradeCyclingDevRuntime))
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
        inputs.property("forge_dep", node.forgeDep)
        inputs.property("minecraft", minecraftVersion)
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
