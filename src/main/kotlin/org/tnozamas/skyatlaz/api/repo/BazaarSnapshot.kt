package org.tnozamas.skyatlaz.api.repo

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** All bazaar quotes at [lastUpdated] (epoch millis), keyed by SkyBlock item id. */
data class BazaarSnapshot(
    val lastUpdated: Long,
    val quotes: Map<String, BazaarQuote>,
) {
    companion object {
        /**
         * Streams the `/v2/skyblock/bazaar` body, reading only `quick_status` and skipping the order books (most of
         * the 3-4 MB payload). Runs on a worker thread.
         */
        fun parse(json: String): BazaarSnapshot {
            val reader = JsonReader(StringReader(json))
            var lastUpdated = 0L
            val quotes = HashMap<String, BazaarQuote>(PRODUCTS_HINT)
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "lastUpdated" -> lastUpdated = reader.nextLong()
                    "products" -> readProducts(reader, quotes)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return BazaarSnapshot(lastUpdated, quotes)
        }

        private fun readProducts(
            reader: JsonReader,
            into: MutableMap<String, BazaarQuote>,
        ) {
            reader.beginObject()
            while (reader.hasNext()) {
                val id = reader.nextName()
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "quick_status") into[id] = readQuote(reader) else reader.skipValue()
                }
                reader.endObject()
            }
            reader.endObject()
        }

        private fun readQuote(reader: JsonReader): BazaarQuote {
            var buy = 0.0
            var sell = 0.0
            var buyVolume = 0L
            var sellVolume = 0L
            var buyWeekly = 0L
            var sellWeekly = 0L
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue()
                    continue
                }
                when (name) {
                    "buyPrice" -> buy = reader.nextDouble()
                    "sellPrice" -> sell = reader.nextDouble()
                    "buyVolume" -> buyVolume = reader.nextLong()
                    "sellVolume" -> sellVolume = reader.nextLong()
                    "buyMovingWeek" -> buyWeekly = reader.nextLong()
                    "sellMovingWeek" -> sellWeekly = reader.nextLong()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return BazaarQuote(buy, sell, buyVolume, sellVolume, buyWeekly, sellWeekly)
        }

        private const val PRODUCTS_HINT = 4096
    }
}
