package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject

/** The mining tree of the player, as the service reports it. */
data class MiningState(
    /** The level of every perk, keyed by the perk id. */
    val perks: Map<String, Int>,
    /** Whether every perk is toggled on, keyed by the perk id. */
    val toggles: Map<String, Boolean>,
    /** The pickaxe ability that is selected. */
    val ability: String?,
    /** The mined powder amounts, keyed by the powder id. */
    val powder: Map<String, Int>,
)

/** The profile of the player, reduced to what the features need. */
data class ServiceProfile(
    /** The uuid of the player. */
    val uuid: String,
    /** The name of the player, when the service knows it. */
    val name: String?,
    /** The state of the mining tree. */
    val mining: MiningState,
    /** The dungeon statistics, as the service sent them. */
    val dungeons: JsonObject?,
    /** The currencies of the player, as the service sent them. */
    val currencies: JsonObject?,
    /** When the service fetched the profile, in milliseconds since the epoch. */
    val fetchedAt: Long,
)
