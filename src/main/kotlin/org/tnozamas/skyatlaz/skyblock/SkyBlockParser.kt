package org.tnozamas.skyatlaz.skyblock

import org.tnozamas.skyatlaz.util.LegacyText
import org.tnozamas.skyatlaz.util.Numbers

/**
 * Turns the raw scoreboard/tab text of a [SkyBlockSources] into a [SkyBlockState]. Pure and allocation-light, so
 * it runs once per changed source (the tracker), not per frame.
 *
 * Hypixel mixes the sidebar lines with formatting codes to keep them unique, sometimes inside a word or a number
 * (`Piggy: §6301,788§j§6,444`); every line is therefore stripped before matching. The sidebar location line uses a
 * private-use glyph (U+E067) instead of a letter, which is how it is found without depending on the area name.
 */
internal object SkyBlockParser {
    private const val SKYBLOCK_TITLE = "SKYBLOCK"
    private const val PURSE = "Purse:"
    private const val PIGGY = "Piggy:"
    private const val BITS = "Bits:"
    private const val AREA = "Area:"
    private const val DUNGEON = "Dungeon:"
    private const val SERVER = "Server:"
    private const val PROFILE = "Profile:"
    private const val SB_LEVEL = "SB Level:"
    private const val BANK = "Bank:"

    private const val GLYPH_START = '\uE000'
    private const val GLYPH_END = '\uF8FF'

    private val LEVEL = Regex("""SB Level:\s*\[(?<level>\d+)]\s*(?<xp>\d+)\s*/\s*(?<max>\d+)""")

    fun parse(sources: SkyBlockSources): SkyBlockState {
        val sidebar = parseSidebar(sources.scoreboardTitle, sources.scoreboard)
        val tab = parseTab(sources.tabList)
        return SkyBlockState(
            onSkyBlock = sidebar.onSkyBlock || tab.area != null,
            area = tab.area,
            subArea = sidebar.subArea,
            serverId = tab.serverId,
            profile = tab.profile,
            level = tab.level,
            levelProgress = tab.levelProgress,
            purse = sidebar.purse,
            bits = sidebar.bits,
            bank = tab.bank,
        )
    }

    private fun parseSidebar(
        title: String?,
        lines: List<String>,
    ): SidebarInfo {
        var subArea: String? = null
        var purse: Long? = null
        var bits: Long? = null
        for (line in lines) {
            if (subArea == null) {
                val glyph = line.indexOfFirst { it in GLYPH_START..GLYPH_END }
                if (glyph >= 0) subArea = LegacyText.strip(line.substring(glyph + 1)).trim().ifEmpty { null }
            }
            val plain = LegacyText.strip(line).trim()
            when {
                plain.startsWith(PURSE) -> purse = numberAfter(plain, PURSE)
                plain.startsWith(PIGGY) -> purse = numberAfter(plain, PIGGY)
                plain.startsWith(BITS) -> bits = numberAfter(plain, BITS)
            }
        }
        return SidebarInfo(
            onSkyBlock = title?.let(LegacyText::strip)?.trim() == SKYBLOCK_TITLE,
            subArea = subArea,
            purse = purse,
            bits = bits,
        )
    }

    private fun parseTab(lines: List<String>): TabInfo {
        var area: String? = null
        var serverId: String? = null
        var profile: String? = null
        var level: Int? = null
        var levelProgress: Double? = null
        var bank: Long? = null
        for (line in lines) {
            val plain = LegacyText.strip(line).trim()
            when {
                area == null && (plain.startsWith(AREA) || plain.startsWith(DUNGEON)) -> {
                    area = textAfter(plain, if (plain.startsWith(AREA)) AREA else DUNGEON)
                }

                serverId == null && plain.startsWith(SERVER) -> {
                    serverId = textAfter(plain, SERVER)
                }

                profile == null && plain.startsWith(PROFILE) -> {
                    profile = profileAfter(plain)
                }

                level == null && plain.startsWith(SB_LEVEL) -> {
                    LEVEL.find(plain)?.let { match ->
                        level = match.groups["level"]?.value?.toIntOrNull()
                        val xp = match.groups["xp"]?.value?.toDoubleOrNull()
                        val max = match.groups["max"]?.value?.toDoubleOrNull()
                        if (xp != null && max != null && max > 0) levelProgress = xp / max
                    }
                }

                bank == null && plain.startsWith(BANK) -> {
                    bank = numberAfter(plain, BANK)
                }
            }
        }
        return TabInfo(area, serverId, profile, level, levelProgress, bank)
    }

    private fun numberAfter(
        text: String,
        prefix: String,
    ): Long? = Numbers.parseLong(text.substring(prefix.length))

    private fun textAfter(
        text: String,
        prefix: String,
    ): String? = text.substring(prefix.length).trim().ifEmpty { null }

    /** The profile name without the marker Hypixel appends (`Mango ♲` -> `Mango`). */
    private fun profileAfter(text: String): String? =
        text
            .substring(PROFILE.length)
            .trim()
            .trimEnd { it == ' ' || it == '♲' || it == 'Ⓑ' || it == '☀' }
            .ifEmpty { null }

    private data class SidebarInfo(
        val onSkyBlock: Boolean,
        val subArea: String?,
        val purse: Long?,
        val bits: Long?,
    )

    private data class TabInfo(
        val area: String?,
        val serverId: String?,
        val profile: String?,
        val level: Int?,
        val levelProgress: Double?,
        val bank: Long?,
    )
}
