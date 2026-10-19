package org.tnozamas.skyatlaz.util

/** SkyBlock item rarities, lowest first ([ordinal] compares tiers). [display] is how lore writes them. */
enum class Rarity(
    val display: String,
) {
    COMMON("COMMON"),
    UNCOMMON("UNCOMMON"),
    RARE("RARE"),
    EPIC("EPIC"),
    LEGENDARY("LEGENDARY"),
    MYTHIC("MYTHIC"),
    DIVINE("DIVINE"),
    SPECIAL("SPECIAL"),
    VERY_SPECIAL("VERY SPECIAL"),
    ULTIMATE("ULTIMATE"),
    ADMIN("ADMIN"),
    ;

    companion object {
        private val BY_DISPLAY = entries.associateBy { it.display }

        fun fromDisplay(text: String): Rarity? = BY_DISPLAY[text]
    }
}
