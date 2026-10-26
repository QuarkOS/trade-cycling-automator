plugins {
    id("dev.kikugie.loom-back-compat")
}

// Stonecutter nodes share a coordinate space; setting `group` here makes the version nodes collide.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "villager_reroller-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.shedaniel.me/", "Shedaniel", "me.shedaniel.cloth")
}

// Client gametests ship in Fabric API from 1.21.5. Older nodes use the tick harness.
if (sc.current.parsed >= "1.21.5") {
    extensions.configure<net.fabricmc.loom.api.fabricapi.FabricApiExtension>("fabricApi") {
        configureTests {
            createSourceSet.set(true)
            modId.set("villager_reroller_test")
            enableGameTests.set(false)
            enableClientGameTests.set(true)
            eula.set(true)
            clearRunDirectory.set(false)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // The client gametest and Mod Menu need the Fabric API mod id, not only the split modules.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    modImplementation("maven.modrinth:cloth-config:${property("deps.cloth_config")}")
    // Cloth nests basic-math as a jar-in-jar. Fabric Loader does not unpack that from the
    // remapped dev classpath, so the config screen cannot open in runClient or the gametest.
    // The published jar still relies on Cloth's own nested copy.
    modLocalRuntime("me.shedaniel.cloth:basic-math:0.6.1")
    modImplementation("maven.modrinth:modmenu:${property("deps.modmenu")}")
    // Trade Cycling is optional on 1.21.11 and required by the older packet driver.
    // Compile against it, never bundle it. -PwithTradeCycling=true also puts it on the dev runtime.
    val tradeCycling = "maven.modrinth:trade-cycling:${sc.properties.get<String>("deps.trade_cycling")}"
    modCompileOnly(tradeCycling)
    if (providers.gradleProperty("withTradeCycling").orNull == "true") {
        modLocalRuntime(tradeCycling)
    }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    // 1.21.11 keeps the widener next to fabric.mod.json. Other nodes swap in a file Loom can validate.
    accessWidenerPath = rootProject.file(when {
        sc.current.parsed >= "26.3" -> "src/main/resources/accesswidener/26.3/villager_reroller.accesswidener"
        sc.current.parsed >= "26.1" -> "src/main/resources/accesswidener/26.1/villager_reroller.accesswidener"
        sc.current.version == "1.21.11" -> "src/main/resources/villager_reroller.accesswidener"
        sc.current.parsed >= "1.21.2" -> "src/main/resources/accesswidener/1.21.4/villager_reroller.accesswidener"
        sc.current.version == "1.19.4" -> "src/main/resources/accesswidener/1.19.4/villager_reroller.accesswidener"
        sc.current.version == "1.19.3" -> "src/main/resources/accesswidener/1.19.3/villager_reroller.accesswidener"
        sc.current.parsed < "1.19.3" -> "src/main/resources/accesswidener/1.19.2/villager_reroller.accesswidener"
        sc.current.version == "1.20.1" -> "src/main/resources/accesswidener/1.20.1/villager_reroller.accesswidener"
        sc.current.version == "1.20.2" -> "src/main/resources/accesswidener/1.20.2/villager_reroller.accesswidener"
        sc.current.version == "1.20.4" -> "src/main/resources/accesswidener/1.20.4/villager_reroller.accesswidener"
        sc.current.version == "1.20.6" -> "src/main/resources/accesswidener/1.20.6/villager_reroller.accesswidener"
        else -> "src/main/resources/accesswidener/1.21.1/villager_reroller.accesswidener"
    })

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        // 1.21.11 keeps the shared run/ directory this project already uses.
        runDirectory = if (sc.current.version == "1.21.11") {
            rootProject.file("run")
        } else {
            rootProject.file("run/${sc.current.version}")
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val minecraftVersion = sc.current.version
        val accessWidener = when {
            sc.current.parsed >= "26.3" -> "accesswidener/26.3/villager_reroller.accesswidener"
            sc.current.parsed >= "26.1" -> "accesswidener/26.1/villager_reroller.accesswidener"
            minecraftVersion == "1.21.11" -> "villager_reroller.accesswidener"
            sc.current.parsed >= "1.21.2" -> "accesswidener/1.21.4/villager_reroller.accesswidener"
            minecraftVersion == "1.19.4" -> "accesswidener/1.19.4/villager_reroller.accesswidener"
            minecraftVersion == "1.19.3" -> "accesswidener/1.19.3/villager_reroller.accesswidener"
            sc.current.parsed < "1.19.3" -> "accesswidener/1.19.2/villager_reroller.accesswidener"
            minecraftVersion == "1.20.1" -> "accesswidener/1.20.1/villager_reroller.accesswidener"
            minecraftVersion == "1.20.2" -> "accesswidener/1.20.2/villager_reroller.accesswidener"
            minecraftVersion == "1.20.4" -> "accesswidener/1.20.4/villager_reroller.accesswidener"
            minecraftVersion == "1.20.6" -> "accesswidener/1.20.6/villager_reroller.accesswidener"
            else -> "accesswidener/1.21.1/villager_reroller.accesswidener"
        }
        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", "${sc.properties.get<String>("mod.version")}+$minecraftVersion")
            register("minecraft", sc.properties["mod.mc_compat"])
            register("java", when {
                sc.current.parsed >= "26.1" -> ">=25"
                sc.current.parsed >= "1.20.5" -> ">=21"
                else -> ">=17"
            })
            register("accessWidener", accessWidener)
            // 1.19 and 1.19.1 Fabric API still uses mod id "fabric". 1.19.2+ renamed it to "fabric-api".
            register("fabricApiId", if (sc.current.parsed < "1.19.2") "fabric" else "fabric-api")
        }

        // Every version breaks and replaces the job site itself, so Trade Cycling stays a suggestion.
        // 1.21.11 keeps this same filter: the depends line contains ">=1.0.0", suggests does not.
        exclude("META-INF/mods.toml")
        exclude("META-INF/accesstransformer.cfg")
        filesMatching("fabric.mod.json") {
            expand(props)
            filter(mapOf("needles" to ">=1.0.0"), dev.quarkos.gradle.SkipMatchingLines::class.java)
        }
        // Drop every widener except the one this node declares. relativePath is the jar path.
        eachFile {
            val path = relativePath.pathString
            val keepRoot = accessWidener == "villager_reroller.accesswidener"
            if (path.startsWith("accesswidener/") && path != accessWidener) {
                exclude()
            }
            if (!keepRoot && path == "villager_reroller.accesswidener") {
                exclude()
            }
            if (keepRoot && path.startsWith("accesswidener/")) {
                exclude()
            }
        }
    }

    withType<JavaExec>().configureEach {
        val cycling = providers.gradleProperty("withTradeCycling").orNull == "true"
        val mode = if (cycling) "trade-cycling" else "plain"
        val artifacts = "/opt/cursor/artifacts/${sc.current.version}/$mode"
        inputs.property("villager_reroller.artifacts", artifacts)
        systemProperty("villager_reroller.artifacts", artifacts)
        systemProperty("villager_reroller.loader", "fabric")
        if (providers.gradleProperty("harness").orNull == "true") {
            systemProperty("villager_reroller.harness", "true")
        }
        // Integrated servers in this VM fall behind the client. Normal jars leave the property unset.
        systemProperty("villager_reroller.timeoutScale", "8")
        val glShim = rootProject.file("/tmp/libglx-srgb.so")
        if (sc.current.parsed >= "26.3" && glShim.exists()) {
            // 26.3's SDL3 asks for an sRGB GLX visual. Xvfb's llvmpipe visuals are sRGB=N.
            // 26.2 and earlier use GLFW, and this shim segfaults inside glXGetProcAddress there.
            environment("LD_PRELOAD", glShim.absolutePath)
            environment("LIBGL_ALWAYS_SOFTWARE", "1")
        }
        // The Gradle daemon does not see xvfb-run's DISPLAY. Pass -Pdisplay and -Pxauth from that shell.
        val display = providers.gradleProperty("display").orNull
        val xauth = providers.gradleProperty("xauth").orNull
        if (!display.isNullOrBlank()) {
            environment("DISPLAY", display)
        }
        if (!xauth.isNullOrBlank()) {
            environment("XAUTHORITY", xauth)
        }
    }

    test {
        useJUnitPlatform()
    }

    named<JavaExec>("runClient") {
        doFirst {
            val eula = workingDir.resolve("eula.txt")
            eula.parentFile?.mkdirs()
            eula.writeText("eula=true\n")
        }
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}

if (project == rootProject) {
    // Realized only when this task is selected, so a normal build does not configure it.
    tasks.register<Exec>("validateAll") {
        group = "verification"
        description = "Build every Villager Reroller jar and launch each release jar in production Minecraft, without and with Trade Cycling."
        notCompatibleWithConfigurationCache("Launches production Minecraft.")
        val jars = rootProject.allprojects.flatMap { candidate ->
            listOfNotNull(candidate.tasks.findByName("reobfJar"), candidate.tasks.findByName("jar"))
        }
        dependsOn(jars)
        workingDir = rootProject.layout.projectDirectory.asFile
        commandLine("python3", rootProject.file("tools/validate.py").absolutePath, "--skip-build")
        environment("VALIDATE_FROM_GRADLE", "1")
    }
}
