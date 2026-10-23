pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks fabric-loom-remap (obfuscated 1.21.x) or fabric-loom (unobfuscated 26.x) per version.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    // Provisions Java 25 for the 26.x nodes when only Java 21 is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Mirrors the Fabric builds of the current Trade Cycling release (1.0.22).
        versions("1.20.2", "1.20.4", "1.20.6", "1.21", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1.2", "26.2", "26.3")
        vcsVersion = "1.21.11"
        // Sibling branch. Fabric project paths stay :1.21.11 so the existing tasks keep working.
        branch("forge") {
            versions("1.20.2", "1.20.4", "1.20.6", "1.21", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1.2", "26.2", "26.3")
        }
    }
}

rootProject.name = "trade-cycling-automator"
