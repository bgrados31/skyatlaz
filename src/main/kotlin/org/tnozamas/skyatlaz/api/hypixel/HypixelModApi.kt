package org.tnozamas.skyatlaz.api.hypixel

import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.WorldLeaveEvent
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * The only class that talks to the Hypixel Mod API (users install the `hypixel-mod-api` mod; we compile against
 * the core API). It subscribes to location events, translates packets into our own data and feeds [session] on
 * the client thread. Everything else reads [session].
 */
class HypixelModApi(
    private val bus: EventBus,
    private val scheduler: Scheduler,
    private val onProblem: (String) -> Unit,
) {
    val session = HypixelSession(bus)

    fun install() {
        val api = HypixelModAPI.getInstance()
        api.createHandler(ClientboundHelloPacket::class.java) { scheduler.runOnClient { session.onHello() } }

        api.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        api.createHandler(ClientboundLocationPacket::class.java) { packet ->
            val location =
                HypixelLocation(
                    serverName = packet.serverName,
                    serverType = packet.serverType.map { it.name() }.orElse(null),
                    lobbyName = packet.lobbyName.orElse(null),
                    mode = packet.mode.orElse(null),
                    map = packet.map.orElse(null),
                )
            scheduler.runOnClient { session.onLocation(location) }
        }

        api
            .createHandler(ClientboundPartyInfoPacket::class.java) { packet ->
                val party =
                    HypixelParty(
                        inParty = packet.isInParty,
                        leader = packet.leader.orElse(null),
                        members = packet.memberMap.mapValues { (_, member) -> member.role.name },
                    )
                scheduler.runOnClient { session.onPartyInfo(party) }
            }.onError { reason ->
                scheduler.runOnClient {
                    session.onPartyError()
                    onProblem("Mod API party request failed: $reason")
                }
            }

        bus.register<WorldLeaveEvent> { session.onDisconnect() }
    }

    /**
     * Asks the server for party info; the answer arrives as a [PartyInfoEvent]. Returns false (nothing sent) when
     * not on Hypixel, a request is pending or the cooldown is active; callers then use [HypixelSession.party].
     */
    fun requestParty(): Boolean {
        if (!session.tryStartPartyRequest(scheduler.currentTick)) return false
        val sent = HypixelModAPI.getInstance().sendPacket(ServerboundPartyInfoPacket())
        if (!sent) session.onPartyError()
        return sent
    }
}
