plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
}

sourceSets.main {
    java {
        srcDir("src/main/java")
        srcDir("../src/main/java")
        include("dev/quarkos/villagerreroller/paper/**")
        include("dev/quarkos/villagerreroller/net/RerollCodec.java")
        include("dev/quarkos/villagerreroller/server/RerollRules.java")
        include("dev/quarkos/villagerreroller/server/ServerSettings.java")
    }
}

tasks.jar {
    archiveBaseName.set("villager_reroller-paper")
    archiveVersion.set("1.0.0")
}
