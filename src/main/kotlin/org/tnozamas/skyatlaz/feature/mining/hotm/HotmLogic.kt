package org.tnozamas.skyatlaz.feature.mining.hotm

import org.tnozamas.skyatlaz.core.debug.Snapshot
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * A perk of the Heart of the Mountain tree, as the game shows it in its menu.
 *
 * The tree is a container, so the perks are read from the items and their lore:
 * - a leveled perk says `Level 12/20` (the level it is at, and the maximum);
 * - a perk at its maximum only says `Level 50`;
 * - an ability says `Pickaxe Ability: ...`;
 * - the rest are one-off perks, active (`ENABLED`), equipped (`SELECTED`) or still to be bought (`Cost`).
 *
 * A perk that is still locked shows the level it would reach (`Level 1/20` for a perk with no level yet), so its level
 * is only the current one when the perk is active.
 */
data class HotmPerk(
    /** The name the game shows, e.g. `Mining Speed`. */
    val name: String,
    /** The level of a leveled perk; null for the others. */
    val level: Int? = null,
    /** The maximum level, when the lore says it. */
    val maxLevel: Int? = null,
    /** How the perk stands now. */
    val state: State,
    /** True for the pickaxe abilities. */
    val ability: Boolean = false,
) {
    /** The state of a perk. */
    enum class State {
        /** Bought and giving its effect. */
        ACTIVE,

        /** An ability, the one that is equipped. */
        SELECTED,

        /** Available but not bought yet. */
        AVAILABLE,

        /** Still to be bought. */
        LOCKED,
    }
}

/**
 * Reads the perks of the Heart of the Mountain tree out of the items of its menu. Pure, so it can be tested with a
 * captured menu (`/skyatlaz debug dump hotm`) and without the game.
 */
object HotmLogic {
    /** The title of the menu of the tree. */
    const val TITLE = "Heart of the Mountain"

    private val LEVEL = Regex("""Level (\d+)(?:/(\d+))?""")
    private const val ABILITY = "Pickaxe Ability:"
    private const val COST = "Cost"
    private const val ENABLED = "ENABLED"
    private const val SELECTED = "SELECTED"

    /**
     * The perks of the tree, in the order the menu shows them. Empty when the items are not the tree (another
     * container, or the tree seen from another menu).
     */
    fun parse(items: List<Snapshot.Item>): List<HotmPerk> {
        val perks = ArrayList<HotmPerk>()
        for (item in items) {
            val perk = parsePerk(item) ?: continue
            perks += perk
        }
        return perks
    }

    /** The perk an item is, or null when it is not a perk (a tier, a button, a crystal...). */
    fun parsePerk(item: Snapshot.Item): HotmPerk? {
        val name = LegacyText.strip(item.name).trim()
        if (name.isEmpty()) return null
        val lore = item.lore.map { LegacyText.strip(it).trim() }
        val level = lore.firstNotNullOfOrNull { line -> levelOf(line) }
        val ability = lore.any { it.contains(ABILITY) }
        val state = stateOf(lore)
        if (level == null && !ability && state == HotmPerk.State.AVAILABLE) return null
        return HotmPerk(
            name = name,
            level = level?.first,
            maxLevel = level?.second,
            state = state,
            ability = ability,
        )
    }

    /** How the perk stands, read from the marks the lore uses. */
    private fun stateOf(lore: List<String>): HotmPerk.State =
        when {
            lore.any { it.contains(ENABLED) } -> HotmPerk.State.ACTIVE
            lore.any { it.contains(SELECTED) } -> HotmPerk.State.SELECTED
            lore.any { it.startsWith(COST) } -> HotmPerk.State.LOCKED
            else -> HotmPerk.State.AVAILABLE
        }

    /** The level of a lore line, e.g. `Level 12/20` -> 12 and 20. */
    private fun levelOf(line: String): Pair<Int, Int?>? {
        val match = LEVEL.find(line) ?: return null
        val level = match.groupValues[1].toIntOrNull() ?: return null
        val max = match.groupValues[2].toIntOrNull()
        return level to max
    }
}
