package org.tnozamas.skyatlaz.core.config

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.tnozamas.skyatlaz.core.event.Registration

/**
 * One typed setting. [value] is always valid: out-of-range or malformed input is clamped or replaced by the
 * default, never stored. Features read [value] directly (a field read) and react to [onChange] instead of polling,
 * so derived data is recomputed only when a setting actually changes.
 *
 * [key] is dot separated (`dungeons.scorehud.scale`); the config screen uses `config.<key>.name` and
 * `config.<key>.description` for its texts. Client thread only.
 */
sealed class ConfigEntry<T : Any>(
    val key: String,
    val default: T,
) {
    var value: T = default
        private set

    private val listeners = ArrayList<(T) -> Unit>()

    /** Set by the [ConfigManager] that owns the entry, to schedule a save. */
    internal var onDirty: (() -> Unit)? = null

    val nameKey: String get() = "config.$key.name"
    val descriptionKey: String get() = "config.$key.description"

    /** Stores [newValue] after validation. Listeners and saving run only when the value really changes. */
    fun set(newValue: T) {
        val sanitized = sanitize(newValue)
        if (sanitized == value) return
        value = sanitized
        onDirty?.invoke()
        for (index in listeners.indices) listeners[index](sanitized)
    }

    fun reset() = set(default)

    /** Called with the new value after every change. Modules register through their context. */
    fun onChange(listener: (T) -> Unit): Registration {
        listeners.add(listener)
        return Registration { listeners.remove(listener) }
    }

    internal abstract fun toJson(): JsonElement

    /** The stored value, or null when [json] has the wrong type (the default is kept). */
    internal abstract fun parse(json: JsonElement): T?

    /** Applies a stored value without marking the config dirty. */
    internal fun load(json: JsonElement?): Boolean {
        val parsed = json?.let { runCatching { parse(it) }.getOrNull() } ?: return false
        val sanitized = sanitize(parsed)
        if (sanitized != value) {
            value = sanitized
            for (index in listeners.indices) listeners[index](sanitized)
        }
        return sanitized == parsed
    }

    protected open fun sanitize(candidate: T): T = candidate

    class Bool internal constructor(
        key: String,
        default: Boolean,
    ) : ConfigEntry<Boolean>(key, default) {
        override fun toJson(): JsonElement = JsonPrimitive(value)

        override fun parse(json: JsonElement): Boolean? =
            if (json.isJsonPrimitive &&
                json.asJsonPrimitive.isBoolean
            ) {
                json.asBoolean
            } else {
                null
            }
    }

    class Integer internal constructor(
        key: String,
        default: Int,
        val range: IntRange,
    ) : ConfigEntry<Int>(key, default) {
        override fun toJson(): JsonElement = JsonPrimitive(value)

        override fun parse(json: JsonElement): Int? = if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) json.asInt else null

        override fun sanitize(candidate: Int): Int = candidate.coerceIn(range)
    }

    class Decimal internal constructor(
        key: String,
        default: Double,
        val range: ClosedFloatingPointRange<Double>,
    ) : ConfigEntry<Double>(key, default) {
        override fun toJson(): JsonElement = JsonPrimitive(value)

        override fun parse(json: JsonElement): Double? = if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) json.asDouble else null

        override fun sanitize(candidate: Double): Double = if (candidate.isNaN()) default else candidate.coerceIn(range)
    }

    class Text internal constructor(
        key: String,
        default: String,
        val maxLength: Int,
    ) : ConfigEntry<String>(key, default) {
        override fun toJson(): JsonElement = JsonPrimitive(value)

        override fun parse(json: JsonElement): String? = if (json.isJsonPrimitive && json.asJsonPrimitive.isString) json.asString else null

        override fun sanitize(candidate: String): String = candidate.take(maxLength)
    }

    /** Stored by constant name, so reordering the enum never changes saved choices. */
    class Choice<E : Enum<E>> internal constructor(
        key: String,
        default: E,
        val type: Class<E>,
    ) : ConfigEntry<E>(key, default) {
        override fun toJson(): JsonElement = JsonPrimitive(value.name)

        override fun parse(json: JsonElement): E? {
            if (!json.isJsonPrimitive || !json.asJsonPrimitive.isString) return null
            val name = json.asString
            return type.enumConstants.firstOrNull { it.name == name }
        }
    }
}
