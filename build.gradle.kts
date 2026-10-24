plugins {
    `java-library`
    `maven-publish`
}

group = "zone.vao"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.17")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.17")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.0.0") { isTransitive = false }
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.4.0")
    compileOnly("com.massivecraft:Factions:1.6.9.5-U0.6.11") { isTransitive = false }
    compileOnly("com.github.angeschossen:LandsAPI:7.10.13") { isTransitive = false }
    compileOnly("com.github.TechFortress:GriefPrevention:16.18") { isTransitive = false }
    compileOnly("world.bentobox:bentobox:2.0.0-SNAPSHOT") { isTransitive = false }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifactId = "BoundaryGuard"
            pom {
                name.set("BoundaryGuard")
                description.set("A unified Bukkit protection library with optional plugin integrations.")
                url.set("https://github.com/Naimadx123/BoundaryGuard")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer { id.set("Naimadx123") }
                }
            }
        }
    }
    repositories {
        maven {
            name = "vao"
            val releases = "https://repo.vao.zone/releases"
            val snapshots = "https://repo.vao.zone/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
            credentials {
                username = providers.gradleProperty("vaoUsername")
                    .orElse(providers.environmentVariable("VAO_REPO_USERNAME")).orNull
                password = providers.gradleProperty("vaoPassword")
                    .orElse(providers.environmentVariable("VAO_REPO_PASSWORD")).orNull
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.check)
}
