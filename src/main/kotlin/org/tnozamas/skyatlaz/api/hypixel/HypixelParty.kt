package org.tnozamas.skyatlaz.api.hypixel

import java.util.UUID

/** The player's party from the Mod API. [members] maps each UUID to its role name (`LEADER`, `MOD`, `MEMBER`). */
data class HypixelParty(
    val inParty: Boolean,
    val leader: UUID?,
    val members: Map<UUID, String>,
) {
    val size: Int get() = members.size

    fun isLeader(uuid: UUID): Boolean = leader == uuid

    companion object {
        val NONE = HypixelParty(inParty = false, leader = null, members = emptyMap())
    }
}
