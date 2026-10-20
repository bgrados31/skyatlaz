package org.tnozamas.skyatlaz.api.repo

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * One item page from NEU-REPO: display facts plus the crafting/forge recipes NEU knows about. Text is stored
 * without legacy color codes, ready for the theme.
 */
data class NeuItem(
    val id: String,
    val displayName: String,
    val vanillaId: String?,
    val lore: List<String>,
    val recipes: List<NeuRecipe>,
) {
    companion object {
        /** Parses one `items/<ID>.json` page. Throws [JsonParseException] when it is not an item page. */
        fun parse(json: String): NeuItem {
            val root = JsonParser.parseString(json).asJsonObject
            val id = root.text("internalname") ?: throw JsonParseException("NEU item has no internalname")
            val recipes = ArrayList<NeuRecipe>(2)
            root.get("recipe")?.takeIf { it.isJsonObject }?.asJsonObject?.let { grid ->
                NeuRecipe.crafting(grid, id, 1)?.let(recipes::add)
            }
            root.get("recipes")?.takeIf { it.isJsonArray }?.asJsonArray?.let { array ->
                for (element in array) {
                    if (!element.isJsonObject) continue
                    NeuRecipe.fromItem(element.asJsonObject, id)?.let(recipes::add)
                }
            }
            return NeuItem(
                id = id,
                displayName = LegacyText.strip(root.text("displayname") ?: id),
                vanillaId = root.text("itemid"),
                lore = root.strings("lore").map(LegacyText::strip),
                recipes = recipes.toList(),
            )
        }

        private fun JsonObject.text(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

        private fun JsonObject.strings(name: String): List<String> {
            val array = get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
            val values = ArrayList<String>(array.size())
            for (element in array) {
                if (element.isJsonPrimitive) values.add(element.asString)
            }
            return values
        }
    }
}
