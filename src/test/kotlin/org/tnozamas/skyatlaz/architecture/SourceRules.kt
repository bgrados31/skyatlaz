package org.tnozamas.skyatlaz.architecture

import java.io.File

/** Shared helpers for text-based rule tests. Fast: plain file scanning, no compiler involved. */
object SourceRules {
    const val BASE_PACKAGE = "org.tnozamas.skyatlaz"
    val mainKotlin = File("src/main/kotlin")
    val mainJava = File("src/main/java")
    val langFile = File("src/main/resources/assets/skyatlaz/lang/en_us.json")

    /** A source file with its path relative to the source root, using '/' separators. */
    data class Source(
        val relativePath: String,
        val lines: List<String>,
    )

    fun kotlinSources(): List<Source> = sources(mainKotlin, "kt")

    fun javaSources(): List<Source> = sources(mainJava, "java")

    private fun sources(
        root: File,
        extension: String,
    ): List<Source> =
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == extension }
            .map { Source(it.relativeTo(root).invariantSeparatorsPath, it.readLines()) }
            .toList()

    /** Lines that match [regex], ignoring comment lines, formatted as `path:line: text`. */
    fun violations(
        sources: List<Source>,
        regex: Regex,
        allowed: (Source) -> Boolean = { false },
    ): List<String> =
        sources.filterNot(allowed).flatMap { source ->
            codeLines(source)
                .filter { regex.containsMatchIn(it.value) }
                .map { "${source.relativePath}:${it.index}: ${it.value.trim()}" }
        }

    /** Non-comment lines of [source], indexed by their 1-based line number. */
    fun codeLines(source: Source): List<IndexedValue<String>> =
        source.lines
            .withIndex()
            .filterNot { (_, line) -> line.trim().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") } }
            .map { IndexedValue(it.index + 1, it.value) }

    fun inPackage(
        source: Source,
        subPackage: String,
    ): Boolean = source.relativePath.startsWith(BASE_PACKAGE.replace('.', '/') + "/" + subPackage.replace('.', '/') + "/")
}
