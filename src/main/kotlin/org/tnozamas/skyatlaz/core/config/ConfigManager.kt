package org.tnozamas.skyatlaz.core.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.TreeMap

/**
 * Owns every setting and the config file:
 * ```
 * { "version": 1, "values": { "dungeons.scorehud.scale": 1.2, "module.dungeons.scorehud.enabled": true } }
 * ```
 * - Keys are flat and sorted, so the file is easy to read and migrations are simple key edits.
 * - Values of keys nobody registered (a removed feature, an older version) are kept, never dropped.
 * - Older files are upgraded with [migrations]; a file that cannot be parsed is moved aside and defaults are used.
 * - Changes are saved [SAVE_DELAY_TICKS] after the last edit, off the client thread, with an atomic replace.
 *   [saveNow] writes synchronously (client shutdown).
 *
 * [onProblem] reports recoverable issues (corrupt file, failed write). Client thread only.
 */
class ConfigManager(
    private val file: Path,
    private val migrations: List<ConfigMigration>,
    private val scheduler: Scheduler?,
    private val onProblem: (message: String, error: Throwable?) -> Unit,
) {
    private val entries = LinkedHashMap<String, ConfigEntry<*>>()
    private var stored = JsonObject()
    private var loaded = false
    private var saveScheduled = false

    /** Registers every entry of [section]; if the file was already loaded, stored values apply immediately. */
    fun register(section: ConfigSection) {
        for (entry in section.entries) register(entry)
    }

    fun register(entry: ConfigEntry<*>) {
        require(entry.key !in entries) { "Config entry '${entry.key}' is registered twice" }
        entries[entry.key] = entry
        entry.onDirty = ::requestSave
        if (loaded) apply(entry)
    }

    fun entry(key: String): ConfigEntry<*>? = entries[key]

    /** Reads the file (if any), migrates it and applies stored values to registered entries. */
    fun load() {
        stored = read() ?: JsonObject()
        loaded = true
        var repaired = false
        for (entry in entries.values) repaired = !apply(entry) || repaired
        if (repaired) requestSave()
    }

    /** Writes the current state now, on the calling thread. */
    fun saveNow() {
        val json = serialize()
        try {
            write(json)
        } catch (error: Exception) {
            onProblem("Could not save config to $file", error)
        }
    }

    /** Debounced background save. Without a scheduler (tests), callers save explicitly. */
    private fun requestSave() {
        val scheduler = scheduler ?: return
        if (saveScheduled) return
        saveScheduled = true
        scheduler.later(SAVE_DELAY_TICKS) {
            saveScheduled = false
            val json = serialize()
            scheduler.async({ write(json) }, { onProblem("Could not save config to $file", it) }) {}
        }
    }

    /** Applies the stored value of [entry]. Returns false when the stored value was invalid and got replaced. */
    private fun apply(entry: ConfigEntry<*>): Boolean {
        val json = stored.get(entry.key) ?: return true
        return entry.load(json)
    }

    private fun serialize(): String {
        // Start from what was stored so unknown keys survive, then overwrite with live values.
        val values = TreeMap<String, JsonElement>()
        for ((key, value) in stored.entrySet()) values[key] = value
        for (entry in entries.values) values[entry.key] = entry.toJson()
        stored = JsonObject().also { json -> values.forEach { (key, value) -> json.add(key, value) } }
        val root = JsonObject()
        root.addProperty(VERSION_KEY, CURRENT_VERSION)
        root.add(VALUES_KEY, stored)
        return GSON.toJson(root)
    }

    private fun read(): JsonObject? {
        if (!Files.exists(file)) return null
        val root =
            try {
                JsonParser.parseString(Files.readString(file)).asJsonObject
            } catch (error: Exception) {
                val backup = file.resolveSibling("${file.fileName}.broken-${System.currentTimeMillis()}")
                runCatching { Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING) }
                onProblem("Config file was unreadable, moved to ${backup.fileName}; using defaults", error)
                return null
            }
        val values = root.getAsJsonObject(VALUES_KEY) ?: JsonObject()
        val version = root.get(VERSION_KEY)?.takeIf { it.isJsonPrimitive }?.asInt ?: CURRENT_VERSION
        if (version > CURRENT_VERSION) {
            onProblem("Config file is from a newer SkyAtlaz (version $version); unknown settings are kept", null)
        }
        for (step in version until CURRENT_VERSION) {
            migrations.filter { it.fromVersion == step }.forEach { it.migrate(values) }
        }
        return values
    }

    private fun write(json: String) {
        Files.createDirectories(file.parent)
        val temp = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temp, json)
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        /** Bump when stored keys or formats change, and add a [ConfigMigration] from the previous version. */
        const val CURRENT_VERSION = 1

        /** One second: a burst of edits (dragging a slider) becomes one write. */
        const val SAVE_DELAY_TICKS = 20

        private const val VERSION_KEY = "version"
        private const val VALUES_KEY = "values"
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }
}
