import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.21"
    id("net.fabricmc.fabric-loom") version "1.19.1"
    id("com.diffplug.spotless") version "8.10.4"
}

/** Reads a required property from gradle.properties. Single access point so types stay non-null. */
fun prop(name: String): String = providers.gradleProperty(name).get()

val targetJavaVersion = 25
val modId = "skyatlaz"

version = prop("mod_version")
group = prop("maven_group")

base {
    archivesName.set(prop("archives_base_name"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

kotlin {
    jvmToolchain(targetJavaVersion)
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
        allWarningsAsErrors.set(false)
    }
}

loom {
    mods {
        register(modId) {
            sourceSet("main")
        }
    }
}

repositories {
    maven("https://repo.hypixel.net/repository/Hypixel/") { name = "Hypixel" }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        name = "DevAuth"
        content { includeGroup("me.djtheredstoner") }
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    // Hypixel Mod API: compile against the core API, run with the Fabric distribution (users install it separately).
    compileOnly("net.hypixel:mod-api:${prop("hypixel_mod_api_version")}")
    localRuntime("maven.modrinth:hypixel-mod-api:${prop("hypixel_mod_api_fabric_version")}")

    // DevAuth: log in with a real Microsoft account in runClient. Opt-in via `skyatlaz.devauth=true`
    // in ~/.gradle/gradle.properties (never in this repo). See docs/TESTING.md.
    if (providers.gradleProperty("skyatlaz.devauth").orNull == "true") {
        localRuntime("me.djtheredstoner:DevAuth-fabric:${prop("devauth_version")}")
    }

    testImplementation(platform("org.junit:junit-bom:${prop("junit_version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val expansions =
        mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to prop("minecraft_version"),
            "loader_version" to prop("loader_version"),
            "fabric_kotlin_version" to prop("fabric_kotlin_version"),
        )
    inputs.properties(expansions)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") { expand(expansions) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlin {
        target("src/**/*.kt")
        ktlint(prop("ktlint_version"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(prop("ktlint_version"))
    }
    java {
        // Mixins are the only Java code in the project.
        target("src/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
    }
    format("misc") {
        target("*.md", "docs/**/*.md", ".gitignore", "src/**/*.json")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

/** Points git at the versioned hooks in .githooks (commit message + formatting checks). */
val installGitHooks by tasks.registering(Exec::class) {
    description = "Configures git to use the hooks in .githooks."
    group = "setup"
    commandLine("git", "config", "core.hooksPath", ".githooks")
    isIgnoreExitValue = true
}

tasks.named("compileKotlin") { dependsOn(installGitHooks) }
