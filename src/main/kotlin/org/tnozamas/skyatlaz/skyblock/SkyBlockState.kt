package org.tnozamas.skyatlaz.skyblock

/**
 * What the scoreboard and tab list say about the current SkyBlock session. Plain data: features read it (or the
 * change events [SkyBlockStateChangeEvent] / [AreaChangeEvent]) and never parse the sidebar or tab themselves.
 *
 * - [area]: the tab list's `Area:` line (`Hub`, `Private Island`, `Dwarven Mines`).
 * - [subArea]: the sidebar's location line after its glyph (`Village`, `The Forge`), more precise than [area].
 * - [levelProgress]: SkyBlock level progress as a fraction (0..1), from the tab list.
 */
data class SkyBlockState(
    val onSkyBlock: Boolean = false,
    val area: String? = null,
    val subArea: String? = null,
    val serverId: String? = null,
    val profile: String? = null,
    val level: Int? = null,
    val levelProgress: Double? = null,
    val purse: Long? = null,
    val bits: Long? = null,
    val bank: Long? = null,
) {
    companion object {
        /** Nothing known yet: not on SkyBlock or before the first parse. */
        val EMPTY = SkyBlockState()
    }
}
