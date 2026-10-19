package org.tnozamas.skyatlaz.core.debug

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import org.tnozamas.skyatlaz.i18n.Tr

/**
 * Opens a captured container as a local chest screen, so container overlays (HOTM tree, menus, solvers) can be
 * built and checked offline against real Hypixel items. The menu id matches no server menu, so clicks are ignored
 * by the server.
 */
object SnapshotContainerScreen {
    private const val OFFLINE_MENU_ID = 100
    private const val SLOTS_PER_ROW = 9
    private val MENU_TYPES =
        listOf(
            MenuType.GENERIC_9x1,
            MenuType.GENERIC_9x2,
            MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4,
            MenuType.GENERIC_9x5,
            MenuType.GENERIC_9x6,
        )

    fun open(container: Snapshot.Container) {
        val minecraft = Minecraft.getInstance()
        val inventory = minecraft.player?.inventory ?: return
        val rows = container.rows.coerceIn(1, MENU_TYPES.size)
        val items = SimpleContainer(rows * SLOTS_PER_ROW)
        for ((slot, item) in container.slots) {
            if (slot < items.containerSize) items.setItem(slot, ItemSnapshots.restore(item))
        }
        val menu = ChestMenu(MENU_TYPES[rows - 1], OFFLINE_MENU_ID, inventory, items, rows)
        minecraft.gui.setScreen(ContainerScreen(menu, inventory, Tr.verbatim(container.title)))
    }
}
