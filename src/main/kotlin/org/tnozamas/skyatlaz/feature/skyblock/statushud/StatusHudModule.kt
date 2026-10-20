package org.tnozamas.skyatlaz.feature.skyblock.statushud

import org.tnozamas.skyatlaz.SkyAtlaz
import org.tnozamas.skyatlaz.core.module.FeatureCategory
import org.tnozamas.skyatlaz.core.module.Module
import org.tnozamas.skyatlaz.core.module.ModuleContext
import org.tnozamas.skyatlaz.skyblock.SkyBlockStateChangeEvent

/**
 * Shows the player's SkyBlock status (area, profile, level, purse...) as a card on the HUD.
 *
 * Hypixel rules tier: Allowed. It only shows information the player already sees in the tab list and the sidebar,
 * in the same place the game already shows it.
 *
 * The card only exists while the player is on SkyBlock: the module stops (and its HUD element disappears) as soon
 * as the shared state says otherwise, so it costs nothing elsewhere.
 */
object StatusHudModule : Module("skyblock.statushud", FeatureCategory.SKYBLOCK) {
    override val config: StatusHudConfig = StatusHudConfig

    /** Allowed and useful for everyone, so it starts on. */
    override val enabledByDefault: Boolean = true

    override fun isActive(): Boolean = SkyAtlaz.SKYBLOCK.state.onSkyBlock

    override fun onEnable(ctx: ModuleContext) {
        val hud = StatusHud()
        ctx.hud(hud)

        val refresh = { hud.update(StatusHudLogic.build(SkyAtlaz.SKYBLOCK.state, options())) }
        ctx.on<SkyBlockStateChangeEvent> { refresh() }
        ctx.watch(StatusHudConfig.showArea) { refresh() }
        ctx.watch(StatusHudConfig.showProfile) { refresh() }
        ctx.watch(StatusHudConfig.showLevel) { refresh() }
        ctx.watch(StatusHudConfig.showPurse) { refresh() }
        ctx.watch(StatusHudConfig.showBits) { refresh() }
        ctx.watch(StatusHudConfig.showBank) { refresh() }
        ctx.watch(StatusHudConfig.numberStyle) { refresh() }

        refresh()
    }

    private fun options(): StatusHudOptions =
        StatusHudOptions(
            showArea = StatusHudConfig.showArea.value,
            showProfile = StatusHudConfig.showProfile.value,
            showLevel = StatusHudConfig.showLevel.value,
            showPurse = StatusHudConfig.showPurse.value,
            showBits = StatusHudConfig.showBits.value,
            showBank = StatusHudConfig.showBank.value,
            numberStyle = StatusHudConfig.numberStyle.value,
        )
}
