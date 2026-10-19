package org.tnozamas.skyatlaz.api.repo

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * Lowest BIN (buy-it-now) auction price per SkyBlock item id. Hypixel does not publish this; it comes from a
 * third-party provider (see [PriceRepository.LOWEST_BIN_URL] and docs/DECISIONS.md).
 */
class LowestBins(
    private val prices: Map<String, Double>,
) {
    val size: Int get() = prices.size

    operator fun get(id: String): Double? = prices[id]

    companion object {
        /** Streams the flat `{ "ITEM_ID": price, ... }` format most providers use. Non-numeric values are skipped. */
        fun parseFlat(json: String): LowestBins {
            val reader = JsonReader(StringReader(json))
            val prices = HashMap<String, Double>(ITEMS_HINT)
            reader.beginObject()
            while (reader.hasNext()) {
                val id = reader.nextName()
                if (reader.peek() == JsonToken.NUMBER) prices[id] = reader.nextDouble() else reader.skipValue()
            }
            reader.endObject()
            return LowestBins(prices)
        }

        private const val ITEMS_HINT = 8192
    }
}
