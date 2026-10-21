package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject

/**
 * Reads what the skyatlaz service answers. Pure, so it is covered by unit tests.
 *
 * The service answers with the profile of the player that is asking for it, already reduced to the parts the
 * features use.
 */
fun parseServiceProfile(json: JsonObject): ServiceProfile =
    ServiceProfile(
        uuid = json.stringOrNull("uuid").orEmpty(),
        name = json.stringOrNull("name"),
        mining = parseMiningState(json.objectOrNull("mining") ?: JsonObject()),
        dungeons = json.objectOrNull("dungeons"),
        currencies = json.objectOrNull("currencies"),
        fetchedAt = json.longOrNull("fetched_at") ?: 0L,
    )

/** Reads the mining tree out of the answer of the service. */
fun parseMiningState(json: JsonObject): MiningState {
    val perks = mutableMapOf<String, Int>()
    json.objectOrNull("perks")?.entrySet()?.forEach { (perk, level) -> perks[perk] = level.asInt }
    val toggles = mutableMapOf<String, Boolean>()
    json.objectOrNull("toggles")?.entrySet()?.forEach { (perk, on) -> toggles[perk] = on.asBoolean }
    val powder = mutableMapOf<String, Int>()
    json.objectOrNull("powder")?.entrySet()?.forEach { (id, amount) -> powder[id] = amount.asInt }
    return MiningState(
        perks = perks,
        toggles = toggles,
        ability = json.stringOrNull("ability"),
        powder = powder,
    )
}

private fun JsonObject.stringOrNull(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString

private fun JsonObject.objectOrNull(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject

private fun JsonObject.longOrNull(name: String): Long? = get(name)?.takeIf { it.isJsonPrimitive }?.asLong
