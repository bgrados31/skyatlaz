package org.tnozamas.skyatlaz.i18n

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.architecture.SourceRules
import org.tnozamas.skyatlaz.core.command.CommandCategory

/** Every key used through [Tr] must exist in the English language file. */
class TranslationKeysTest {
    private val definedKeys =
        Regex(""""(skyatlaz\.[^"]+)"\s*:""")
            .findAll(SourceRules.langFile.readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `used keys exist in en_us`() {
        // description(...) declares command help keys (core.command.CommandBuilder).
        val usage = Regex("""\b(?:tr|key|description)\("([^"$]+)"""")
        val missing =
            SourceRules.kotlinSources().flatMap { source ->
                SourceRules
                    .codeLines(source)
                    .flatMap { (_, line) -> usage.findAll(line).map { it.groupValues[1] }.toList() }
                    .filterNot { "skyatlaz.$it" in definedKeys }
                    .map { "${source.relativePath}: $it" }
            }
        assertTrue(missing.isEmpty()) { "Missing keys in en_us.json:\n" + missing.joinToString("\n") }
    }

    @Test
    fun `generated keys exist in en_us`() {
        val generated = CommandCategory.entries.map { it.translationKey }
        val missing = generated.filterNot { "skyatlaz.$it" in definedKeys }
        assertTrue(missing.isEmpty()) { "Missing generated keys in en_us.json:\n" + missing.joinToString("\n") }
    }

    @Test
    fun `language file keys are namespaced`() {
        val all = Regex(""""([^"]+)"\s*:""").findAll(SourceRules.langFile.readText()).map { it.groupValues[1] }.toList()
        assertTrue(all.all { it.startsWith("skyatlaz.") }) { "All keys must start with 'skyatlaz.'" }
        assertTrue(all.size == all.toSet().size) { "Duplicate keys in en_us.json" }
    }
}
