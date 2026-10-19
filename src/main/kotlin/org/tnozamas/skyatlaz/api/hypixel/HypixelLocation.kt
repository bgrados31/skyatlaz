package org.tnozamas.skyatlaz.api.hypixel

/**
 * Where the player is on Hypixel, from the Mod API location event (sent on every server switch). Pure data.
 * - [serverType]: game type name, e.g. `SKYBLOCK` (null in limbo / unknown).
 * - [mode]: on SkyBlock, the island id: `hub`, `dynamic` (private island), `mining_3`, `dungeon`, ...
 * - [map]: human map name, e.g. `Dwarven Mines`.
 * - [lobbyName]: set while in a lobby instead of a game server.
 */
data class HypixelLocation(
    val serverName: String,
    val serverType: String?,
    val lobbyName: String?,
    val mode: String?,
    val map: String?,
) {
    val isSkyBlock: Boolean get() = serverType == SKYBLOCK && lobbyName == null

    val isLobby: Boolean get() = lobbyName != null

    /** The SkyBlock island id ([mode]) or null when not on SkyBlock. */
    val island: String? get() = if (isSkyBlock) mode else null

    companion object {
        const val SKYBLOCK = "SKYBLOCK"
    }
}
