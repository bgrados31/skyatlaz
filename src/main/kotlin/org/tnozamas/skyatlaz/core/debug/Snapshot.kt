package org.tnozamas.skyatlaz.core.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Everything a parser might need from one moment on Hypixel, captured by `/skyatlaz debug dump` and used as a
 * test fixture (docs/TESTING.md). Pure data: no Minecraft types, so tests read it without starting the game.
 * Texts keep legacy `§` codes (see `util.LegacyText`) because Hypixel patterns depend on them.
 */
data class Snapshot(
    val name: String,
    val capturedAt: String,
    val server: String?,
    val scoreboardTitle: String?,
    val scoreboard: List<String>,
    val tabHeader: String?,
    val tabFooter: String?,
    val tabList: List<String>,
    val chat: List<String>,
    val heldItem: Item?,
    val container: Container?,
) {
    /** An item with its SkyBlock data (`customData` is the SNBT of `minecraft:custom_data`). */
    data class Item(
        val id: String,
        val count: Int,
        val name: String,
        val lore: List<String>,
        val customData: String?,
    )

    /** An open chest-like menu: title, row count and the non-empty slots by index. */
    data class Container(
        val title: String,
        val rows: Int,
        val slots: Map<Int, Item>,
    )

    fun toJson(): String = GSON.toJson(write())

    private fun write(): JsonObject =
        JsonObject().apply {
            addProperty("format", FORMAT)
            addProperty("name", name)
            addProperty("capturedAt", capturedAt)
            addProperty("server", server)
            addProperty("scoreboardTitle", scoreboardTitle)
            add("scoreboard", strings(scoreboard))
            addProperty("tabHeader", tabHeader)
            addProperty("tabFooter", tabFooter)
            add("tabList", strings(tabList))
            add("chat", strings(chat))
            add("heldItem", heldItem?.let(::writeItem))
            add(
                "container",
                container?.let { container ->
                    JsonObject().apply {
                        addProperty("title", container.title)
                        addProperty("rows", container.rows)
                        add(
                            "slots",
                            JsonObject().apply {
                                container.slots.toSortedMap().forEach { (slot, item) -> add(slot.toString(), writeItem(item)) }
                            },
                        )
                    }
                },
            )
        }

    companion object {
        /** Bump when the shape changes; [fromJson] keeps reading older dumps. */
        const val FORMAT = 1

        private val GSON =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()

        /** Reads a dump; missing fields become empty or null, so old fixtures keep working. */
        fun fromJson(json: String): Snapshot {
            val root = JsonParser.parseString(json).asJsonObject
            return Snapshot(
                name = root.string("name") ?: "",
                capturedAt = root.string("capturedAt") ?: "",
                server = root.string("server"),
                scoreboardTitle = root.string("scoreboardTitle"),
                scoreboard = root.stringList("scoreboard"),
                tabHeader = root.string("tabHeader"),
                tabFooter = root.string("tabFooter"),
                tabList = root.stringList("tabList"),
                chat = root.stringList("chat"),
                heldItem = root.obj("heldItem")?.let(::readItem),
                container =
                    root.obj("container")?.let { container ->
                        Snapshot.Container(
                            title = container.string("title") ?: "",
                            rows = container.get("rows")?.asInt ?: 0,
                            slots =
                                container
                                    .obj("slots")
                                    ?.entrySet()
                                    ?.associate { (slot, item) -> slot.toInt() to readItem(item.asJsonObject) }
                                    ?: emptyMap(),
                        )
                    },
            )
        }

        private fun writeItem(item: Item): JsonObject =
            JsonObject().apply {
                addProperty("id", item.id)
                addProperty("count", item.count)
                addProperty("name", item.name)
                add("lore", strings(item.lore))
                addProperty("customData", item.customData)
            }

        private fun readItem(json: JsonObject) =
            Item(
                id = json.string("id") ?: "minecraft:air",
                count = json.get("count")?.asInt ?: 1,
                name = json.string("name") ?: "",
                lore = json.stringList("lore"),
                customData = json.string("customData"),
            )

        private fun strings(values: List<String>) = JsonArray().apply { values.forEach(::add) }

        private fun JsonObject.present(key: String): JsonElement? = get(key)?.takeUnless { it.isJsonNull }

        private fun JsonObject.string(key: String): String? = present(key)?.asString

        private fun JsonObject.obj(key: String): JsonObject? = present(key)?.asJsonObject

        private fun JsonObject.stringList(key: String): List<String> = present(key)?.asJsonArray?.map { it.asString } ?: emptyList()
    }
}
