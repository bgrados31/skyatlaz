package org.tnozamas.skyatlaz.feature.mining.hotm

import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.SkyAtlaz
import org.tnozamas.skyatlaz.api.profile.ProfileMining
import org.tnozamas.skyatlaz.api.repo.HotmLayout
import org.tnozamas.skyatlaz.core.module.FeatureCategory
import org.tnozamas.skyatlaz.core.module.Module
import org.tnozamas.skyatlaz.core.module.ModuleContext
import org.tnozamas.skyatlaz.util.NumberStyle

/**
 * Shows the mining tree of the player: the perks that matter, the ability that is equipped and the powder that is
 * left, as a card on the HUD.
 *
 * Hypixel rules tier: Allowed. It reads the profile of the player with the API key of the player (their own data,
 * the same one the game shows in the tree) and the community layout of the tree; it never touches the data of
 * another player.
 *
 * The card needs the developer API key of the player (in the general settings). Without it the feature stays quiet
 * and the card shows nothing. The profile is read from the network once every few minutes and the card is rebuilt
 * only when it changes.
 */
object HotmModule : Module("mining.hotm", FeatureCategory.MINING) {
    override val config: HotmConfig = HotmConfig

    /** It needs the API key of the player, so it does not start on its own. */
    override val enabledByDefault: Boolean = false

    override fun isActive(): Boolean = SkyAtlaz.SKYBLOCK.state.onSkyBlock

    override fun onEnable(ctx: ModuleContext) {
        val hud = HotmHud()
        ctx.hud(hud)
        val profile = ctx.useProfile()
        val neu = ctx.useNeu()

        var layout: HotmLayout? = null
        var mining: ProfileMining? = null

        fun rebuild() {
            val tree = layout
            val data = mining
            val content =
                if (tree == null || data == null) {
                    HotmContent.EMPTY
                } else {
                    HotmLogic.buildCard(data, tree, options())
                }
            hud.update(content)
        }

        fun load() {
            neu.loadLayout { loaded ->
                layout = loaded
                rebuild()
            }
            profile.loadMining(Minecraft.getInstance().user.name) { loaded ->
                mining = loaded
                rebuild()
            }
        }

        ctx.every(REFRESH_TICKS) { load() }
        ctx.watch(HotmConfig.showPowders) { rebuild() }
        ctx.watch(HotmConfig.showAbility) { rebuild() }
        ctx.watch(HotmConfig.numberStyle) { rebuild() }

        load()
    }

    private fun options(): HotmOptions =
        HotmOptions(
            showPowders = HotmConfig.showPowders.value,
            showAbility = HotmConfig.showAbility.value,
            numberStyle = HotmConfig.numberStyle.value,
        )
}

/** Five minutes between reads; the profile of a player changes slowly. */
private const val REFRESH_TICKS = 6000
