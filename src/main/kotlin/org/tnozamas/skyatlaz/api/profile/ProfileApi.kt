package org.tnozamas.skyatlaz.api.profile

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/**
 * A perk of the Heart of the Mountain, as the profile API gives it: the level of the perk and whether it is
 * switched on.
 */
data class ProfilePerk(
    /** The id of the perk in the NEU data (`mining_fortune`, `mole`, `core_of_the_mountain`...). */
    val id: String,
    /** The level of the perk. */
    val level: Int,
    /** Whether the perk gives its effect. */
    val enabled: Boolean,
)

/**
 * What the profile of the player says about the mining tree: the perks of the Heart of the Mountain and the
 * powders that are left.
 */
data class ProfileMining(
    /** Every perk of the tree that the profile mentions. */
    val perks: List<ProfilePerk>,
    /** The id of the pickaxe ability that is equipped, when the profile says one. */
    val selectedAbility: String?,
    val mithrilPowder: Long,
    val gemstonePowder: Long,
    val glacitePowder: Long,
)

/**
 * Reads the profile of the player out of the answer of the Hypixel profile API.
 *
 * The answer holds every profile of the player (`profiles`), each one with its members. The mining tree is split
 * in five sub-trees (`nodes.mining_1` ... `nodes.mining_5`), each one with the level of its perks and, for every
 * perk, whether it is on (`toggle_<id>`).
 *
 * Pure: it only needs the JSON, so it is tested without the game and without the network.
 */
object ProfileApi {
    /** The endpoint of the profile of a player. It needs the developer API key in the `API-Key` header. */
    const val PROFILE_URL = "https://api.hypixel.net/v2/skyblock/profiles"

    /** The name of the header that carries the developer API key. */
    const val API_KEY_HEADER = "API-Key"

    /** The five sub-trees of the mining tree. */
    private val TREE_SLOTS = 1..5

    /**
     * The mining tree of the profile of the player.
     *
     * @param json the answer of the API.
     * @param uuid the uuid of the player; the member of a co-op profile that is not the player is ignored.
     * @throws JsonParseException when the answer is not a profile (an error of the API, for example).
     */
    fun parseMining(
        json: String,
        uuid: String,
    ): ProfileMining {
        val root = JsonParser.parseString(json).asJsonObject
        if (root.get("success")?.asBoolean == false) {
            throw JsonParseException(root.str("cause") ?: "The API refused the request")
        }
        val member = member(root, uuid)
        val perks = LinkedHashMap<String, ProfilePerk>()
        for (slot in TREE_SLOTS) {
            val tree = member?.obj("skill_tree")?.obj("nodes")?.obj("mining_$slot") ?: continue
            for ((key, value) in tree.entrySet()) {
                val id = key.removePrefix(TOGGLE_PREFIX)
                val current = perks[id]
                val enabled = current?.enabled ?: true
                val level = current?.level ?: 0
                if (key.startsWith(TOGGLE_PREFIX)) {
                    perks[id] = ProfilePerk(id, level, enabled && value.asBoolean)
                } else {
                    val newLevel = value.asInt
                    perks[id] = ProfilePerk(id, maxOf(level, newLevel), enabled)
                }
            }
        }
        val core = member?.obj("mining_core")
        val tree = member?.obj("skill_tree")
        val slot = tree?.obj("selected_skill_tree_slot")?.int("mining") ?: 1
        return ProfileMining(
            perks = perks.values.toList(),
            selectedAbility = tree?.obj("selected_ability")?.str("mining_$slot"),
            mithrilPowder = core?.long("powder_mithril") ?: 0,
            gemstonePowder = core?.long("powder_gemstone") ?: 0,
            glacitePowder = core?.long("powder_glacite") ?: 0,
        )
    }

    /** The member of the selected profile that belongs to the player. */
    private fun member(
        root: JsonObject,
        uuid: String,
    ): JsonObject? {
        val profiles = root.get("profiles")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        val profile =
            profiles
                .filter { it.isJsonObject }
                .map { it.asJsonObject }
                .firstOrNull { it.get("selected")?.asBoolean == true }
                ?: profiles.firstOrNull { it.isJsonObject }?.asJsonObject
                ?: return null
        val members = profile.obj("members") ?: return null
        return members.obj(uuid) ?: members
            .entrySet()
            .firstOrNull()
            ?.value
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
    }

    private const val TOGGLE_PREFIX = "toggle_"

    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.str(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.long(name: String): Long? = get(name)?.takeIf { it.isJsonPrimitive }?.asLong

    private fun JsonObject.int(name: String): Int? = get(name)?.takeIf { it.isJsonPrimitive }?.asInt
}
