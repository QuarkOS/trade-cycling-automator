plugins {
    id("dev.kikugie.loom-back-compat")
}

// Stonecutter nodes share a coordinate space; setting `group` here makes the version nodes collide.
val mcLabel = when (sc.current.version) {
    "1.21.9" -> "1.21.9-1.21.10"
    "1.21.6" -> "1.21.6-1.21.8"
    else -> sc.current.version
}
version = "${property("mod.version")}+$mcLabel"
// 1.21.1 and 26.x stay on the Trade Cycling packet driver. Every other node, including 1.21.11, builds the trade picker.
val packetDriverOnly = sc.current.version == "1.21.1" || sc.current.parsed >= "26.1"
val fullPicker = !packetDriverOnly
base.archivesName = if (fullPicker && sc.current.version != "1.21.11") "villager_reroller-fabric" else property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

if (fullPicker) {
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
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    if (fullPicker) {
        // The client gametest and Mod Menu need the Fabric API mod id, not only the split modules.
        modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
    } else {
        fapi(
            "fabric-api-base",
            if (sc.current.parsed >= "26.1") "fabric-key-mapping-api-v1" else "fabric-key-binding-api-v1",
            "fabric-lifecycle-events-v1",
            "fabric-screen-api-v1",
            "fabric-events-interaction-v0",
        )
    }

    modImplementation("maven.modrinth:cloth-config:${property("deps.cloth_config")}")
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
    if (sc.current.version == "1.21.11") {
        accessWidenerPath = rootProject.file("src/main/resources/villager_reroller.accesswidener")
    } else if (fullPicker) {
        accessWidenerPath = rootProject.file("src/accesswidener/villager_reroller.pre2111.accesswidener")
    }

        runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        // Legacy nodes keep their own run directory so they do not share saves with 1.21.11.
        runDirectory = if (fullPicker && sc.current.version != "1.21.11") {
            rootProject.file("run/${sc.current.version}")
        } else {
            rootProject.file("run")
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

        val minecraftVersion = mcLabel
        if (fullPicker && sc.current.version != "1.21.11") {
            filesMatching("villager_reroller.accesswidener") {
                filter { line ->
                    line.replace(
                        "net/minecraft/world/entity/npc/villager/VillagerTrades",
                        "net/minecraft/world/entity/npc/VillagerTrades"
                    )
                }
            }
        }
        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", "${sc.properties.get<String>("mod.version")}+$minecraftVersion")
            register("minecraft", sc.properties["mod.mc_compat"])
            register("java", if (sc.current.parsed >= "26.1") ">=25" else ">=21")
        }

        // Picker builds drop the hard Trade Cycling line and keep suggests. Packet-driver builds drop the access widener.
        val droppedLine = if (fullPicker) ">=1.0.0" else "accessWidener"
        filesMatching("fabric.mod.json") {
            expand(props)
            filter(mapOf("needles" to droppedLine), dev.quarkos.gradle.SkipMatchingLines::class.java)
        }
    }

    if (packetDriverOnly) {
        named<JavaCompile>("compileJava") {
            exclude("**/picker/**")
        }
    }

    if (fullPicker && sc.current.version != "1.21.11") {
        named<JavaExec>("runClientGameTest") {
            systemProperty("villagerReroller.artifacts", "/opt/cursor/artifacts/${sc.current.version}")
        }
    }

    test {
        useJUnitPlatform()
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
