package org.tnozamas.skyatlaz.api.repo

import com.google.gson.JsonObject

/**
 * A crafting-grid or forge recipe from NEU-REPO. [grid] is keyed by NEU's slots (`A1`..`C3`) and only set for
 * [NeuRecipeType.CRAFTING]; [inputs] is a flat list and only set for [NeuRecipeType.FORGE].
 */
data class NeuRecipe(
    val type: NeuRecipeType,
    val outputId: String,
    val outputCount: Int,
    val grid: Map<String, NeuIngredient>,
    val inputs: List<NeuIngredient>,
    val durationSeconds: Int?,
) {
    companion object {
        private val SLOTS = arrayOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3")

        /** Reads one entry of the `recipes` array. Types we do not model are skipped. */
        internal fun fromItem(
            json: JsonObject,
            itemId: String,
        ): NeuRecipe? {
            val outputId = json.text("overrideOutputId") ?: itemId
            val outputCount = json.int("count") ?: 1
            return when (json.text("type")?.lowercase()) {
                "crafting" -> crafting(json, outputId, outputCount)
                "forge" -> forge(json, outputId, outputCount)
                else -> null
            }
        }

        /** The legacy single `recipe` object is always an implicit crafting grid with one output. */
        internal fun crafting(
            grid: JsonObject,
            outputId: String,
            outputCount: Int,
        ): NeuRecipe? {
            val slots = LinkedHashMap<String, NeuIngredient>(SLOTS.size)
            for (slot in SLOTS) {
                val ingredient = grid.text(slot)?.let(NeuIngredient::parse) ?: continue
                slots[slot] = ingredient
            }
            if (slots.isEmpty()) return null
            return NeuRecipe(NeuRecipeType.CRAFTING, outputId, outputCount, slots, emptyList(), null)
        }

        private fun forge(
            json: JsonObject,
            outputId: String,
            outputCount: Int,
        ): NeuRecipe? {
            val array = json.get("inputs")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
            val inputs = ArrayList<NeuIngredient>(array.size())
            for (element in array) {
                if (!element.isJsonPrimitive) continue
                NeuIngredient.parse(element.asString)?.let(inputs::add)
            }
            if (inputs.isEmpty()) return null
            val duration = json.int("duration")?.takeIf { it > 0 }
            return NeuRecipe(NeuRecipeType.FORGE, outputId, outputCount, emptyMap(), inputs, duration)
        }

        private fun JsonObject.text(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

        private fun JsonObject.int(name: String): Int? = get(name)?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()
    }
}
