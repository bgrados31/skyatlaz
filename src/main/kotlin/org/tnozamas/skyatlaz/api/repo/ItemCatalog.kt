package org.tnozamas.skyatlaz.api.repo

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.tnozamas.skyatlaz.util.Rarity
import java.io.StringReader

/** Every SkyBlock item Hypixel lists, keyed by id. */
class ItemCatalog(
    val lastUpdated: Long,
    private val items: Map<String, CatalogItem>,
) {
    val size: Int get() = items.size

    val ids: Set<String> get() = items.keys

    operator fun get(id: String): CatalogItem? = items[id]

    companion object {
        /** Streams `/v2/resources/skyblock/items`, keeping only the fields we use (skins and stats are skipped). */
        fun parse(json: String): ItemCatalog {
            val reader = JsonReader(StringReader(json))
            var lastUpdated = 0L
            val items = HashMap<String, CatalogItem>(ITEMS_HINT)
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "lastUpdated" -> {
                        lastUpdated = reader.nextLong()
                    }

                    "items" -> {
                        reader.beginArray()
                        while (reader.hasNext()) readItem(reader)?.let { items[it.id] = it }
                        reader.endArray()
                    }

                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()
            return ItemCatalog(lastUpdated, items)
        }

        private fun readItem(reader: JsonReader): CatalogItem? {
            var id: String? = null
            var name: String? = null
            var tier: String? = null
            var category: String? = null
            var npcSellPrice: Double? = null
            reader.beginObject()
            while (reader.hasNext()) {
                val field = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue()
                    continue
                }
                when (field) {
                    "id" -> id = reader.nextString()
                    "name" -> name = reader.nextString()
                    "tier" -> tier = reader.nextString()
                    "category" -> category = reader.nextString()
                    "npc_sell_price" -> npcSellPrice = reader.nextDouble()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            val itemId = id ?: return null
            return CatalogItem(
                id = itemId,
                name = name ?: itemId,
                rarity = tier?.let { runCatching { Rarity.valueOf(it) }.getOrNull() },
                category = category,
                npcSellPrice = npcSellPrice,
            )
        }

        private const val ITEMS_HINT = 8192
    }
}
