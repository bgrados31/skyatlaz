package org.tnozamas.skyatlaz.api.profile

import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.api.repo.NeuRepository
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.util.Numbers

/**
 * `/skyatlaz debug profile`: reads the profile of the player and shows the mining tree it has, so the answer of the
 * API can be checked without the card. Joins the existing `debug` group.
 */
class ProfileDebugCommands(
    private val profiles: ProfileClient,
    private val neu: NeuRepository,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("profile") {
                    description("command.debug.profile")
                    executes { read() }
                }
            }
        }
    }

    private fun read() {
        val name = Minecraft.getInstance().user.name
        profiles.loadMining(name) { mining ->
            if (mining == null) {
                Chat.warning(Tr.tr("debug.profile.failed"))
                return@loadMining
            }
            neu.loadLayout { layout ->
                Chat.info(
                    Tr.tr(
                        "debug.profile.powders",
                        Tr.verbatim(Numbers.grouped(mining.mithrilPowder)),
                        Tr.verbatim(Numbers.grouped(mining.gemstonePowder)),
                        Tr.verbatim(Numbers.grouped(mining.glacitePowder)),
                    ),
                )
                for (perk in mining.perks) {
                    val known = layout?.perk(perk.id)
                    val name = known?.name ?: perk.id
                    val level = if (known == null) "${perk.level}" else "${perk.level}/${known.maxLevel}"
                    Chat.info(Tr.tr("debug.profile.perk", Tr.verbatim(name), Tr.verbatim(level)))
                }
            }
        }
    }
}
