package org.tnozamas.skyatlaz.ui.palette

/** One command the palette offers: what it is called, what it does and how to run it. */
data class PaletteEntry(
    /** What the player reads, e.g. `debug price <id>`. */
    val label: String,
    /** The line under it, e.g. `Show bazaar, auction and NPC prices of an item`. */
    val description: String,
    /** The command to run or to type, e.g. `/skyatlaz debug price`. */
    val command: String,
    /** True when the command needs an argument the player has to type. */
    val needsInput: Boolean,
)

/**
 * The search behind the command palette: which commands match what the player typed, and in which order. Pure, so it
 * can be tested without the game.
 *
 * The player types a few letters, not the whole name, so the match is fuzzy: the letters only have to appear in
 * order. A command whose name contains the query beats one that only matches its description.
 */
object CommandPaletteLogic {
    /**
     * The entries that match [query], best first. An empty query keeps every entry in its original order.
     */
    fun search(
        entries: List<PaletteEntry>,
        query: String,
    ): List<PaletteEntry> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return entries
        val matches = ArrayList<Match>(entries.size)
        for ((order, entry) in entries.withIndex()) {
            val score = score(entry, needle) ?: continue
            matches.add(Match(score, order, entry))
        }
        matches.sortWith(compareByDescending<Match> { it.score }.thenBy { it.order })
        return matches.map { it.entry }
    }

    private data class Match(
        val score: Int,
        val order: Int,
        val entry: PaletteEntry,
    )

    /** How well an entry matches the query, or null when it does not match at all. */
    private fun score(
        entry: PaletteEntry,
        needle: String,
    ): Int? {
        val label = entry.label.lowercase()
        val description = entry.description.lowercase()
        label.indexOf(needle).takeIf { it >= 0 }?.let { return NAME_EXACT - it }
        fuzzySpan(label, needle).takeIf { it >= 0 }?.let { return NAME_FUZZY - it }
        description.indexOf(needle).takeIf { it >= 0 }?.let { return TEXT_EXACT - it }
        if (fuzzySpan(description, needle) >= 0) return TEXT_FUZZY
        return null
    }

    /**
     * How many characters the matched letters span in [text], or -1 when they do not appear in order. A tight span
     * is a better match than a scattered one.
     */
    private fun fuzzySpan(
        text: String,
        needle: String,
    ): Int {
        var at = 0
        var first = -1
        var last = 0
        for (letter in needle) {
            val found = text.indexOf(letter, at)
            if (found < 0) return -1
            if (first < 0) first = found
            last = found
            at = found + 1
        }
        return last - first + 1
    }

    private const val NAME_EXACT = 1000
    private const val NAME_FUZZY = 600
    private const val TEXT_EXACT = 200
    private const val TEXT_FUZZY = 100
}
