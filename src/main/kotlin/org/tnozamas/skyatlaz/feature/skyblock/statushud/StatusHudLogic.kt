package org.tnozamas.skyatlaz.feature.skyblock.statushud

import org.tnozamas.skyatlaz.skyblock.SkyBlockState
import org.tnozamas.skyatlaz.util.NumberStyle
import org.tnozamas.skyatlaz.util.Numbers

/** The rows the player asked for, and how numbers are written. */
data class StatusHudOptions(
    val showArea: Boolean = true,
    val showProfile: Boolean = true,
    val showLevel: Boolean = true,
    val showPurse: Boolean = true,
    val showBits: Boolean = false,
    val showBank: Boolean = false,
    val numberStyle: NumberStyle = NumberStyle.COMPACT,
)

/** One row of the card: the key to translate and the values to fill it with. */
data class StatusHudLine(
    val key: String,
    val args: List<String>,
)

/**
 * What the status HUD shows, and how. Pure: it turns a [SkyBlockState] plus the player's settings into the rows to
 * draw, so it can be unit tested without the game and reused by the HUD element.
 *
 * A row is a translation key and its arguments, never a finished sentence: the text belongs to the language file
 * and the values to the game state.
 */
object StatusHudLogic {
    /**
     * The rows to draw for [state], in reading order. Empty when the player is not on SkyBlock (yet); a row whose
     * value the scoreboard does not show is skipped instead of showing a placeholder.
     */
    fun build(
        state: SkyBlockState,
        options: StatusHudOptions,
    ): List<StatusHudLine> {
        if (!state.onSkyBlock) return emptyList()
        val lines = ArrayList<StatusHudLine>(6)
        if (options.showArea) {
            areaLine(state)?.let(lines::add)
        }
        if (options.showProfile) {
            state.profile?.let { lines += StatusHudLine(KEY_PROFILE, listOf(it)) }
        }
        if (options.showLevel) {
            state.level?.let { lines += StatusHudLine(KEY_LEVEL, listOf(it.toString())) }
        }
        if (options.showPurse) {
            state.purse?.let { lines += StatusHudLine(KEY_PURSE, listOf(amount(it, options.numberStyle))) }
        }
        if (options.showBits) {
            state.bits?.let { lines += StatusHudLine(KEY_BITS, listOf(amount(it, options.numberStyle))) }
        }
        if (options.showBank) {
            state.bank?.let { lines += StatusHudLine(KEY_BANK, listOf(amount(it, options.numberStyle))) }
        }
        return lines
    }

    /** The area row: with the sub-area when both are known, with the area alone otherwise. */
    private fun areaLine(state: SkyBlockState): StatusHudLine? {
        val area = state.area
        val subArea = state.subArea
        return when {
            area != null && subArea != null -> StatusHudLine(KEY_LOCATION, listOf(area, subArea))
            area != null -> StatusHudLine(KEY_AREA, listOf(area))
            subArea != null -> StatusHudLine(KEY_AREA, listOf(subArea))
            else -> null
        }
    }

    private fun amount(
        value: Long,
        style: NumberStyle,
    ): String =
        when (style) {
            NumberStyle.COMPACT -> Numbers.compact(value.toDouble())
            NumberStyle.GROUPED -> Numbers.grouped(value)
        }

    private const val KEY_LOCATION = "skyblock.statushud.location"
    private const val KEY_AREA = "skyblock.statushud.area"
    private const val KEY_PROFILE = "skyblock.statushud.profile"
    private const val KEY_LEVEL = "skyblock.statushud.level"
    private const val KEY_PURSE = "skyblock.statushud.purse"
    private const val KEY_BITS = "skyblock.statushud.bits"
    private const val KEY_BANK = "skyblock.statushud.bank"
}
