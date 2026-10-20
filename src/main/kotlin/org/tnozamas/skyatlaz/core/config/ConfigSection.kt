package org.tnozamas.skyatlaz.core.config

/**
 * A group of settings under one key prefix, usually one per feature:
 * ```
 * object ScoreHudConfig : ConfigSection("dungeons.scorehud") {
 *     val scale = decimal("scale", 1.0, 0.5..3.0)
 *     val style = choice("style", Style.COMPACT)
 * }
 * ```
 * Register it once with the [ConfigManager] (entries are loaded from disk at that point).
 */
abstract class ConfigSection(
    val id: String,
) {
    private val declared = ArrayList<ConfigEntry<*>>()

    /** Heading used by the config screen; feature sections can override it. */
    open val titleKey: String get() = "config.section.$id"

    val entries: List<ConfigEntry<*>> get() = declared

    protected fun boolean(
        name: String,
        default: Boolean,
    ) = add(ConfigEntry.Bool(key(name), default))

    protected fun int(
        name: String,
        default: Int,
        range: IntRange,
    ) = add(ConfigEntry.Integer(key(name), default, range))

    protected fun decimal(
        name: String,
        default: Double,
        range: ClosedFloatingPointRange<Double>,
    ) = add(ConfigEntry.Decimal(key(name), default, range))

    protected fun text(
        name: String,
        default: String,
        maxLength: Int = 256,
    ) = add(ConfigEntry.Text(key(name), default, maxLength))

    protected inline fun <reified E : Enum<E>> choice(
        name: String,
        default: E,
    ) = addChoice(name, default, E::class.java)

    @PublishedApi
    internal fun <E : Enum<E>> addChoice(
        name: String,
        default: E,
        type: Class<E>,
    ) = add(ConfigEntry.Choice(key(name), default, type))

    private fun key(name: String): String {
        require(NAME_PATTERN.matches(name)) { "Invalid config entry name '$name' in section '$id'" }
        return "$id.$name"
    }

    private fun <E : ConfigEntry<*>> add(entry: E): E {
        require(declared.none { it.key == entry.key }) { "Duplicate config entry '${entry.key}'" }
        declared.add(entry)
        return entry
    }

    private companion object {
        val NAME_PATTERN = Regex("[a-z0-9_]+")
    }
}
