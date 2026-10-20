package org.tnozamas.skyatlaz.api.repo

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/**
 * The layout of the Heart of the Mountain tree, as the community data describes it: every perk with its name, its
 * maximum level and the place it takes in the tree.
 */
data class HotmLayout(
    val perks: List<HotmLayoutPerk>,
) {
    /** The perk with that id, or null when the data does not know it. */
    fun perk(id: String): HotmLayoutPerk? = perks.firstOrNull { it.id == id }

    companion object {
        /** Parses `constants/hotmlayout.json`. Throws [JsonParseException] when it is not a tree layout. */
        fun parse(json: String): HotmLayout {
            val root = JsonParser.parseString(json).asJsonObject
            val perks = root.obj("hotm")?.obj("perks") ?: throw JsonParseException("The layout has no tree")
            val parsed = ArrayList<HotmLayoutPerk>(perks.size())
            for ((id, value) in perks.entrySet()) {
                val perk = value.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                parsed +=
                    HotmLayoutPerk(
                        id = id,
                        name = perk.str("name") ?: id,
                        maxLevel = perk.int("maxLevel") ?: 1,
                        x = perk.int("x") ?: 0,
                        y = perk.int("y") ?: 0,
                    )
            }
            return HotmLayout(perks = parsed)
        }

        private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject

        private fun JsonObject.str(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString

        private fun JsonObject.int(name: String): Int? = get(name)?.takeIf { it.isJsonPrimitive }?.asInt
    }
}

/** One perk of the tree layout. */
data class HotmLayoutPerk(
    /** The id of the perk, the one the profile API uses too. */
    val id: String,
    /** The name the game shows, e.g. `Mining Fortune`. */
    val name: String,
    /** The level the perk reaches at its maximum. */
    val maxLevel: Int,
    /** The column of the perk in the tree. */
    val x: Int,
    /** The row of the perk in the tree. */
    val y: Int,
)
