plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "multigainer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.fancyinnovations.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("de.oliver:FancyHolograms:2.10.0")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Database connection pooling and MariaDB driver
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    runServer {
        minecraftVersion("1.21.11")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}