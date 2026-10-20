package org.tnozamas.skyatlaz.skyblock

/**
 * The raw game text [SkyBlockState] is parsed from, in the legacy form used across the mod (`§` codes). Captured
 * from the live scoreboard/tab list by [SkyBlockReader], or pushed from a saved dump by `/skyatlaz debug load`, so
 * parsing and tests never need the game.
 */
data class SkyBlockSources(
    val scoreboardTitle: String?,
    val scoreboard: List<String>,
    val tabHeader: String?,
    val tabFooter: String?,
    val tabList: List<String>,
)
