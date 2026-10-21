package org.tnozamas.skyatlaz.core.debug

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.mixin.PlayerTabOverlayAccessor
import org.tnozamas.skyatlaz.util.LegacyText
import java.time.Instant

/** Reads the current game state into a [Snapshot]. Client thread only; runs on demand (debug), never per tick. */
object SnapshotCapture {
    private const val SCOREBOARD_LINES = 15
    private const val SLOTS_PER_ROW = 9

    fun capture(
        name: String,
        recentChat: List<String>,
    ): Snapshot {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        val tab = minecraft.gui.hud.tabList as PlayerTabOverlayAccessor
        return Snapshot(
            name = name,
            capturedAt = Instant.now().toString(),
            server = minecraft.currentServer?.ip,
            scoreboardTitle = scoreboardTitle(minecraft),
            scoreboard = scoreboard(minecraft),
            tabHeader = tab.`skyatlaz$getHeader`()?.let(LegacyText::encode),
            tabFooter = tab.`skyatlaz$getFooter`()?.let(LegacyText::encode),
            tabList = tabList(minecraft),
            chat = recentChat,
            heldItem = player?.mainHandItem?.let(ItemSnapshots::capture),
            container = container(minecraft),
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

    /** The open chest-like menu without the player's own inventory slots. */
    fun container(minecraft: Minecraft): Snapshot.Container? {
        val screen = minecraft.gui.screen() as? AbstractContainerScreen<*> ?: return null
        val slots = HashMap<Int, Snapshot.Item>()
        var size = 0
        for (slot in screen.menu.slots) {
            if (slot.container is Inventory) continue
            size++
            ItemSnapshots.capture(slot.item)?.let { slots[slot.index] = it }
        }
        return Snapshot.Container(LegacyText.encode(screen.title), (size + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW, slots)
    }
}
