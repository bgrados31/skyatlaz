package org.tnozamas.skyatlaz.api.hypixel

import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.scheduler.Cooldown

/**
 * What the Mod API has told us during the current connection: are we on Hypixel, where, and with which party.
 * The single source for this data; features read it (or listen to its events) and never talk to the Mod API.
 *
 * Pure state machine fed by [HypixelModApi], so it is unit tested without the game. Client thread only.
 */
class HypixelSession(
    private val bus: EventBus,
) {
    /** True after the Mod API hello packet of the current connection. */
    var onHypixel: Boolean = false
        private set

    var location: HypixelLocation? = null
        private set

    /** Last known party; [HypixelParty.NONE] until the first answer. */
    var party: HypixelParty = HypixelParty.NONE
        private set

    private val partyCooldown = Cooldown(PARTY_COOLDOWN_TICKS)
    private var partyRequestedAt: Long? = null

    fun onHello() {
        onHypixel = true
    }

    fun onLocation(newLocation: HypixelLocation) {
        onHypixel = true
        if (newLocation == location) return
        val previous = location
        location = newLocation
        bus.post(LocationChangeEvent(previous, newLocation))
    }

    fun onDisconnect() {
        val previous = location
        onHypixel = false
        location = null
        party = HypixelParty.NONE
        partyRequestedAt = null
        partyCooldown.reset()
        if (previous != null) bus.post(LocationChangeEvent(previous, null))
    }

    /**
     * Whether a party request may be sent now: on Hypixel, no answer pending (a lost answer expires after
     * [PARTY_TIMEOUT_TICKS]) and outside the cooldown. Reserves the slot when it returns true.
     */
    fun tryStartPartyRequest(now: Long): Boolean {
        if (!onHypixel) return false
        val pendingSince = partyRequestedAt
        if (pendingSince != null && now - pendingSince < PARTY_TIMEOUT_TICKS) return false
        if (!partyCooldown.tryUse(now)) return false
        partyRequestedAt = now
        return true
    }

    fun onPartyInfo(newParty: HypixelParty) {
        partyRequestedAt = null
        party = newParty
        bus.post(PartyInfoEvent(newParty))
    }

    /** The server refused or failed a party request: free the slot, keep the cooldown. */
    fun onPartyError() {
        partyRequestedAt = null
    }

    companion object {
        /** Five seconds between party requests: the server rate-limits them and party data rarely changes. */
        const val PARTY_COOLDOWN_TICKS = 100

        const val PARTY_TIMEOUT_TICKS = 200
    }
}
