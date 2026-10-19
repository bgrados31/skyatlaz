package org.tnozamas.skyatlaz.i18n

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.tnozamas.skyatlaz.SkyAtlaz

/**
 * Single entry point for every user-facing text. Keys are relative to the mod namespace:
 * `tr("dungeons.score.title")` resolves `skyatlaz.dungeons.score.title` in `lang/en_us.json`.
 *
 * Never build user-facing text with `Component.literal` or string literals. See docs/I18N.md.
 */
object Tr {
    private const val PREFIX = SkyAtlaz.MOD_ID + "."

    /** Full translation key for [key]. */
    fun key(key: String): String = PREFIX + key

    /** Translatable component for [key], with optional format [args]. */
    fun tr(
        key: String,
        vararg args: Any,
    ): MutableComponent = Component.translatable(PREFIX + key, *args)

    /**
     * Text that is not language and must not be translated: command syntax, ids, numbers, separators. Never use it
     * for words a player reads as a sentence.
     */
    fun verbatim(text: String): MutableComponent = Component.literal(text)
}
