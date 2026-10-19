package org.tnozamas.skyatlaz.api.hypixel

import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * `/skyatlaz debug hypixel`: prints what the Mod API reports (location) and requests the party. Joins the existing
 * `debug` group, so register it after the core debug commands.
 */
class HypixelDebugCommands(
    private val api: HypixelModApi,
    private val bus: EventBus,
) {
    private var pendingPartyReply: Registration? = null

    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("hypixel") {
                    description("command.debug.hypixel")
                    executes { report() }
                }
            }
        }
    }

    private fun report() {
        val session = api.session
        val location = session.location
        if (!session.onHypixel || location == null) {
            Chat.warning(Tr.tr("debug.hypixel.not_connected"))
            return
        }
        Chat.info(
            Tr.tr(
                "debug.hypixel.location",
                Tr.verbatim(location.serverName),
                Tr.verbatim(location.serverType ?: "-"),
                Tr.verbatim(location.mode ?: "-"),
                Tr.verbatim(location.map ?: "-"),
            ),
        )
        if (pendingPartyReply == null && api.requestParty()) {
            pendingPartyReply =
                bus.register<PartyInfoEvent> { event ->
                    pendingPartyReply?.unregister()
                    pendingPartyReply = null
                    val party = event.party
                    if (party.inParty) {
                        Chat.info(Tr.tr("debug.hypixel.party", Tr.verbatim(party.size.toString())))
                    } else {
                        Chat.info(Tr.tr("debug.hypixel.no_party"))
                    }
                }
        }
    }
}
