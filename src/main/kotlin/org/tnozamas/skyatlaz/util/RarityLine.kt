package org.tnozamas.skyatlaz.util

/**
 * The rarity line at the bottom of SkyBlock lore, e.g. `a SHINY MYTHIC DUNGEON CHESTPLATE a`:
 * - `a ... a`: obfuscated markers of a recombobulated item;
 * - `SHINY`, `DUNGEON`: flags; [category]: the rest (`SWORD`, `ACCESSORY`, `ITEM`...), null when absent.
 * Also accepts `Rarity: LEGENDARY` (menus) and shard lines ending in `(ID C9)`.
 */
data class RarityLine(
    val rarity: Rarity,
    val category: String?,
    val dungeon: Boolean,
    val shiny: Boolean,
    val recombobulated: Boolean,
) {
    companion object {
        private val NAMES = Rarity.entries.sortedByDescending { it.display.length }.joinToString("|") { it.display }
        private val PATTERN =
            Regex(
                """(?:Rarity: )?(?<recomb>a )?(?<shiny>SHINY )?(?<rarity>$NAMES)(?<dungeon> DUNGEON)?""" +
                    """(?: (?<category>[A-Z][A-Z ]*?))??(?: a)?(?: \(ID \w\d+\))?""",
            )

        /** Parses one lore line (legacy codes allowed). Null when it is not a rarity line. */
        fun parse(line: String): RarityLine? {
            val text = LegacyText.strip(line).trim()
            if (text.isEmpty() || (!text[0].isUpperCase() && !text.startsWith("a "))) return null
            val match = PATTERN.matchEntire(text) ?: return null
            val groups = match.groups
            return RarityLine(
                rarity = Rarity.fromDisplay(groups["rarity"]!!.value) ?: return null,
                category = groups["category"]?.value?.takeIf { it.isNotBlank() },
                dungeon = groups["dungeon"] != null,
                shiny = groups["shiny"] != null,
                recombobulated = groups["recomb"] != null,
            )
        }

        /** The rarity line of an item: the last lore line that parses (it is the last non-empty one in practice). */
        fun fromLore(lore: List<String>): RarityLine? {
            for (index in lore.indices.reversed()) {
                parse(lore[index])?.let { return it }
            }
            return null
        }
    }
}
