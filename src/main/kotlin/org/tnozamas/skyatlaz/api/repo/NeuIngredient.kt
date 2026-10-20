package org.tnozamas.skyatlaz.api.repo

/** One input of a NEU recipe: a SkyBlock item id (or `SKYBLOCK_COIN`) and how much of it. */
data class NeuIngredient(
    val id: String,
    val amount: Double,
) {
    companion object {
        /**
         * Parses NEU's `ID:count` form. The count is optional (drops and some trades omit it) and is a decimal for
         * coin costs, e.g. `SKYBLOCK_COIN:8.0`. Returns null for empty values and non-positive amounts.
         */
        fun parse(text: String): NeuIngredient? {
            val clean = text.trim()
            if (clean.isEmpty()) return null
            val separator = clean.lastIndexOf(':')
            val id = (if (separator > 0) clean.substring(0, separator) else clean).trim().uppercase()
            if (id.isEmpty()) return null
            val amount = if (separator > 0) clean.substring(separator + 1).trim().toDoubleOrNull() ?: 1.0 else 1.0
            if (amount <= 0) return null
            return NeuIngredient(id, amount)
        }
    }
}
