package org.tnozamas.skyatlaz.feature.mining.hotm

import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.Skyatlaz
import org.tnozamas.skyatlaz.api.repo.HotmLayout
import org.tnozamas.skyatlaz.api.service.ServiceProfileEvent
import org.tnozamas.skyatlaz.core.debug.SnapshotCapture
import org.tnozamas.skyatlaz.core.module.FeatureCategory
import org.tnozamas.skyatlaz.core.module.Module
import org.tnozamas.skyatlaz.core.module.ModuleContext
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * Shows the mining tree of the player: the perks that matter and the ability that is equipped, as a card on the HUD.
 *
 * Hypixel rules tier: Allowed. It reads the menu of the tree that the player has open, the same information the game
 * shows, and the community layout of the tree (the names and the maximum levels of the perks). It never touches the
 * data of another player and it never asks the Hypixel API for anything: the levels it gets from our own service are
 * the levels of the player that is using it.
 *
 * The tree is an inventory, so the perks are only there while the menu is open: the module reads the open menu once
 * a second and the card follows it. When the menu is closed the card falls back to the profile of our service, so it
 * still shows the levels of the player. It costs nothing when the menu is closed (a title check per second) and
 * nothing at all when the service is off.
 */
object HotmModule : Module("mining.hotm", FeatureCategory.MINING) {
    override val config: HotmConfig = HotmConfig

    /** It only shows what the player has open, so it is safe and useful for everyone. */
    override val enabledByDefault: Boolean = true

    override fun isActive(): Boolean = Skyatlaz.SKYBLOCK.state.onSkyBlock

    override fun onEnable(ctx: ModuleContext) {
        val hud = HotmHud()
        ctx.hud(hud)
        val neu = ctx.useNeu()
        val service = ctx.useService()

        var layout: HotmLayout? = null
        var perks: List<HotmPerk> = emptyList()
        var servicePerks: List<HotmPerk> = emptyList()

        fun rebuild() {
            // The open menu is the most precise source, so it wins over the profile of the service.
            hud.update(HotmLogic.buildCard(perks.ifEmpty { servicePerks }, layout, options()))
        }

        // The layout only adds the maximum of the perks that are at their maximum; the lore of the others says it.
        neu.loadLayout { loaded ->
            layout = loaded
            rebuild()
        }

        // The profile of the service is the fallback when the menu is closed.
        ctx.on<ServiceProfileEvent> {
            val mining = service.profile?.mining
            servicePerks =
                if (mining == null) {
                    emptyList()
                } else {
                    HotmLogic.perksFromProfile(mining.perks, mining.toggles, mining.ability, layout)
                }
            rebuild()
        }

        ctx.every(READ_TICKS) {
            val container = SnapshotCapture.container(Minecraft.getInstance())
            val title = container?.title?.let { LegacyText.strip(it) }
            if (title != HotmLogic.TITLE) {
                if (perks.isNotEmpty()) {
                    perks = emptyList()
                    rebuild()
                }
                return@every
            }
            val open = HotmLogic.parse(container.slots.values.toList())
            if (open != perks) {
                perks = open
                rebuild()
            }
        }

        ctx.watch(HotmConfig.showAbility) { rebuild() }
    }

    private fun options(): HotmOptions = HotmOptions(showAbility = HotmConfig.showAbility.value)
}

/** A second between reads: the menu does not change faster than that and the tree is read only while it is open. */
private const val READ_TICKS = 20
