package org.tnozamas.skyatlaz.feature.mining.hotm

import org.tnozamas.skyatlaz.api.repo.HotmLayout
import org.tnozamas.skyatlaz.core.debug.Snapshot
import org.tnozamas.skyatlaz.util.LegacyText

/** The key of the title of the card. */
private const val TITLE_KEY = "mining.hotm.title"

/** One row of the mining card: the key to translate and the values to fill it with. */
data class HotmLine(
    val key: String,
    val args: List<String>,
)

/** What the mining card shows. */
data class HotmContent(
    val titleKey: String,
    val lines: List<HotmLine>,
) {
    companion object {
        val EMPTY = HotmContent(TITLE_KEY, emptyList())
    }
}

/** The settings of the card. */
data class HotmOptions(
    val showAbility: Boolean = true,
)

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

    /** The perks the card shows first, in this order. The names are the ones the game uses. */
    private val HEADLINE = listOf("Mining Speed", "Mining Fortune", "Titanium Insanium", "Core of the Mountain")

    /** The row of a perk: its name, its level and its maximum. */
    private const val KEY_PERK = "mining.hotm.perk"

    /** The row of a perk whose maximum is not known: its name and its level. */
    private const val KEY_PERK_LEVEL = "mining.hotm.perk_level"

    /** The row of the pickaxe ability that is equipped. */
    private const val KEY_ABILITY = "mining.hotm.ability"

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

    /**
     * The rows of the mining card, in reading order.
     *
     * The card shows the perks that decide how fast the player mines and the ability that is equipped. Every row is a
     * translation key and its values: the names of the perks are the ones the game uses, so they are data, not text
     * of the mod.
     *
     * @param perks the perks of the tree that the open menu shows.
     * @param layout the layout of the tree (the community data); it only adds the maximum of the perks that are at
     *   their maximum, because the lore of those does not say it.
     */
    fun buildCard(
        perks: List<HotmPerk>,
        layout: HotmLayout?,
        options: HotmOptions = HotmOptions(),
    ): HotmContent {
        if (perks.isEmpty()) return HotmContent.EMPTY
        val lines = ArrayList<HotmLine>(5)
        for (name in HEADLINE) {
            val perk = perks.firstOrNull { it.name == name } ?: continue
            val max = perk.maxLevel ?: layout?.perks?.firstOrNull { it.name == name }?.maxLevel
            if (max == null) {
                lines += HotmLine(KEY_PERK_LEVEL, listOf(perk.name, perk.level?.toString() ?: "0"))
            } else {
                lines += HotmLine(KEY_PERK, listOf(perk.name, perk.level?.toString() ?: "0", max.toString()))
            }
        }
        if (options.showAbility) {
            perks.firstOrNull { it.state == HotmPerk.State.SELECTED }?.let { ability ->
                lines += HotmLine(KEY_ABILITY, listOf(ability.name))
            }
        }
        if (lines.isEmpty()) return HotmContent.EMPTY
        return HotmContent(TITLE_KEY, lines)
    }

    /**
     * The perks of the tree, read from the profile of the skyatlaz service.
     *
     * The service knows the level of every perk and which ones are giving their effect, but not how the tree is
     * called. The layout of the tree (the community data) gives the name and the maximum of every perk, so the two
     * together are the tree of the player, and the card works even when the menu is closed.
     *
     * The ability that is equipped is not a leveled perk, so it is not in the levels: it comes with the profile.
     *
     * @param levels the level of every perk, keyed by the id of the perk.
     * @param toggles which perks are giving their effect, keyed by the id of the perk.
     * @param ability the id of the pickaxe ability that is equipped.
     */
    fun perksFromProfile(
        levels: Map<String, Int>,
        toggles: Map<String, Boolean>,
        ability: String?,
        layout: HotmLayout?,
    ): List<HotmPerk> {
        if (layout == null) return emptyList()
        val perks = ArrayList<HotmPerk>(layout.perks.size)
        for (perk in layout.perks) {
            val level = levels[perk.id] ?: continue
            val state =
                when {
                    ability == perk.id -> HotmPerk.State.SELECTED
                    toggles[perk.id] == true -> HotmPerk.State.ACTIVE
                    else -> HotmPerk.State.LOCKED
                }
            perks +=
                HotmPerk(
                    name = perk.name,
                    level = level,
                    maxLevel = perk.maxLevel,
                    state = state,
                )
        }
        val equipped = ability?.let { id -> layout.perk(id) }
        if (equipped != null && perks.none { it.name == equipped.name }) {
            perks +=
                HotmPerk(
                    name = equipped.name,
                    state = HotmPerk.State.SELECTED,
                    ability = true,
                )
        }
        return perks
    }
}
