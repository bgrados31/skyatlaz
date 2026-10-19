package org.tnozamas.skyatlaz.architecture

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.tnozamas.skyatlaz.architecture.SourceRules.inPackage
import org.tnozamas.skyatlaz.architecture.SourceRules.violations
import java.io.File

/**
 * Project rules enforced at build time (`./gradlew check`), so no contributor, human or AI, can
 * silently break the architecture. Pattern rules live in config/source-rules.json (shared with the
 * Claude Code hooks); structural rules live here. See docs/ARCHITECTURE.md and docs/CONVENTIONS.md.
 */
class ArchitectureTest {
    private val kotlin = SourceRules.kotlinSources()
    private val java = SourceRules.javaSources()

    private fun assertNone(
        rule: String,
        found: List<String>,
    ) = assertTrue(found.isEmpty()) { "$rule\n" + found.joinToString("\n") }

    @TestFactory
    fun `pattern rules from config`(): List<DynamicTest> =
        JsonParser.parseString(File("config/source-rules.json").readText()).asJsonObject.getAsJsonArray("rules").map { element ->
            val rule = element.asJsonObject
            val regex = Regex(rule["pattern"].asString)
            val extensions = rule.getAsJsonArray("extensions").map { it.asString }
            val allowedIn = rule.getAsJsonArray("allowedIn").map { it.asString }
            DynamicTest.dynamicTest(rule["id"].asString) {
                val sources = kotlin.filter { "kt" in extensions } + java.filter { "java" in extensions }
                assertNone(
                    "[${rule["id"].asString}] ${rule["message"].asString}",
                    violations(sources, regex) { source -> allowedIn.any { inPackage(source, it) } },
                )
            }
        }

    @Test
    fun `all sources live in the base package`() {
        val prefix = SourceRules.BASE_PACKAGE.replace('.', '/') + "/"
        assertNone(
            "Every source file must be under ${SourceRules.BASE_PACKAGE}:",
            (kotlin + java).map { it.relativePath }.filterNot { it.startsWith(prefix) },
        )
    }

    @Test
    fun `java is only used for mixins`() {
        assertNone(
            "Java files are only allowed in the mixin package:",
            java.filterNot { inPackage(it, "mixin") }.map { it.relativePath },
        )
    }

    @Test
    fun `features do not depend on other features`() {
        val featureImport = Regex("""^import ${Regex.escape(SourceRules.BASE_PACKAGE)}\.feature\.([a-z0-9_]+)\.([a-z0-9_]+)""")
        val found =
            kotlin.filter { inPackage(it, "feature") }.flatMap { source ->
                // org/tnozamas/skyatlaz/feature/<category>/<feature>/...
                val own =
                    source.relativePath
                        .split('/')
                        .drop(4)
                        .take(2)
                source.lines.mapNotNull { line ->
                    featureImport
                        .find(line)
                        ?.takeIf { it.groupValues.drop(1) != own }
                        ?.let { "${source.relativePath}: ${line.trim()}" }
                }
            }
        assertNone("Features must communicate through core events/state, never import each other:", found)
    }
}
