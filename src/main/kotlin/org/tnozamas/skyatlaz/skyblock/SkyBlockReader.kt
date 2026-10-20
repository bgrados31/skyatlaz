package org.tnozamas.skyatlaz.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.mixin.PlayerTabOverlayAccessor
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * Reads the current scoreboard and tab list into [SkyBlockSources], the shared capture used by both the live
 * tracker and `/skyatlaz debug dump`. Client thread only, and only on demand (a few times per second or on a dump),
 * never per frame.
 */
object SkyBlockReader {
    private const val SCOREBOARD_LINES = 15

    fun capture(): SkyBlockSources {
        val minecraft = Minecraft.getInstance()
        val tab = minecraft.gui.hud.tabList as PlayerTabOverlayAccessor
        return SkyBlockSources(
            scoreboardTitle = scoreboardTitle(minecraft),
            scoreboard = scoreboard(minecraft),
            tabHeader = tab.`skyatlaz$getHeader`()?.let(LegacyText::encode),
            tabFooter = tab.`skyatlaz$getFooter`()?.let(LegacyText::encode),
            tabList = tabList(minecraft),
        )
    }

    private fun scoreboardTitle(minecraft: Minecraft): String? {
        val scoreboard = minecraft.level?.scoreboard ?: return null
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)?.displayName?.let(LegacyText::encode)
    }

    /** Sidebar lines top to bottom, as rendered: team prefix + owner + suffix unless the entry has its own display. */
    private fun scoreboard(minecraft: Minecraft): List<String> {
        val scoreboard = minecraft.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        return scoreboard
            .listPlayerScores(objective)
            .filter { !it.isHidden }
            .sortedWith(compareByDescending<PlayerScoreEntry> { it.value() }.thenBy { it.owner() })
            .take(SCOREBOARD_LINES)
            .map { entry ->
                val line =
                    entry.display() ?: PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), Tr.verbatim(entry.owner()))
                LegacyText.encode(line)
            }
    }

    /** Tab entries in display order (vanilla sorts by order, team, name; Hypixel encodes columns in team names). */
    private fun tabList(minecraft: Minecraft): List<String> {
        val connection = minecraft.connection ?: return emptyList()
        val tab = minecraft.gui.hud.tabList
        return connection.listedOnlinePlayers
            .sortedWith(
                compareByDescending<PlayerInfo> { it.tabListOrder }
                    .thenBy { it.team?.name ?: "" }
                    .thenBy { it.profile.name() },
            ).map { LegacyText.encode(tab.getNameForDisplay(it)) }
    }
}
