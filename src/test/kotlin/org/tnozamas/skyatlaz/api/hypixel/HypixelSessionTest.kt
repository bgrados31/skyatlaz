package org.tnozamas.skyatlaz.api.hypixel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.event.EventBus
import java.util.UUID

class HypixelSessionTest {
    private val bus = EventBus { _, error -> throw AssertionError(error) }
    private val session = HypixelSession(bus)
    private val changes = ArrayList<Pair<HypixelLocation?, HypixelLocation?>>()

    init {
        bus.register<LocationChangeEvent> { changes.add(it.previous to it.current) }
    }

    private val hub = HypixelLocation("mini12A", "SKYBLOCK", null, "hub", "Hub")
    private val mines = HypixelLocation("mini40C", "SKYBLOCK", null, "mining_3", "Dwarven Mines")
    private val lobby = HypixelLocation("lobby3", "SKYBLOCK", "skyblocklobby1", null, null)

    @Test
    fun `location changes post one event each`() {
        session.onLocation(hub)
        session.onLocation(hub)
        session.onLocation(mines)

        assertEquals(listOf(null to hub, hub to mines), changes)
        assertEquals("mining_3", session.location?.island)
        assertTrue(session.onHypixel)
    }

    @Test
    fun `skyblock island only on skyblock game servers`() {
        assertEquals("hub", hub.island)
        assertTrue(lobby.isLobby)
        assertFalse(lobby.isSkyBlock)
        assertNull(lobby.island)
        assertNull(HypixelLocation("mini1", "BEDWARS", null, "BEDWARS_EIGHT_ONE", "Lighthouse").island)
    }

    @Test
    fun `disconnect clears everything and announces leaving`() {
        session.onLocation(hub)
        session.onPartyInfo(HypixelParty(true, UUID.randomUUID(), emptyMap()))

        session.onDisconnect()

        assertFalse(session.onHypixel)
        assertNull(session.location)
        assertEquals(HypixelParty.NONE, session.party)
        assertEquals(hub to null, changes.last())
    }

    @Test
    fun `party requests need hypixel, respect the cooldown and never overlap`() {
        assertFalse(session.tryStartPartyRequest(0))

        session.onHello()
        assertTrue(session.tryStartPartyRequest(0))
        assertFalse(session.tryStartPartyRequest(1)) // pending

        session.onPartyInfo(HypixelParty.NONE)
        assertFalse(session.tryStartPartyRequest(2)) // cooldown
        assertTrue(session.tryStartPartyRequest(HypixelSession.PARTY_COOLDOWN_TICKS.toLong()))
    }

    @Test
    fun `a lost party answer stops blocking after the timeout`() {
        session.onHello()
        session.tryStartPartyRequest(0)

        assertFalse(session.tryStartPartyRequest(HypixelSession.PARTY_TIMEOUT_TICKS - 1L))
        assertTrue(session.tryStartPartyRequest(HypixelSession.PARTY_TIMEOUT_TICKS.toLong()))
    }

    @Test
    fun `party info is stored and announced`() {
        val leader = UUID.randomUUID()
        val party = HypixelParty(true, leader, mapOf(leader to "LEADER", UUID.randomUUID() to "MEMBER"))
        var announced: HypixelParty? = null
        bus.register<PartyInfoEvent> { announced = it.party }

        session.onPartyInfo(party)

        assertEquals(party, session.party)
        assertEquals(party, announced)
        assertEquals(2, party.size)
        assertTrue(party.isLeader(leader))
    }
}
