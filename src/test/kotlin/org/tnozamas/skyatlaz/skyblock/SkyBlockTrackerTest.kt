package org.tnozamas.skyatlaz.skyblock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.event.EventBus

class SkyBlockTrackerTest {
    private val bus = EventBus { _, error -> throw AssertionError(error) }
    private val tracker = SkyBlockTracker(bus)
    private val events = ArrayList<String>()
    private val states = ArrayList<SkyBlockState>()

    init {
        bus.register<AreaChangeEvent> { events += "area:${it.previous}->${it.current}" }
        bus.register<SkyBlockStateChangeEvent> {
            events += "state"
            states += it.current
        }
    }

    private fun sources(
        area: String,
        subArea: String,
    ) = SkyBlockSources(
        scoreboardTitle = "§e§lSKYBLOCK",
        scoreboard = listOf("§a §7\uE067 §a$subArea", "§fPiggy: §6638,769§6§s§6,221"),
        tabHeader = null,
        tabFooter = null,
        tabList = listOf(" Area: §7$area", " Server: §8mini1", " Profile: §aMango"),
    )

    @Test
    fun `unchanged sources do not post again`() {
        tracker.updateFromGame(sources("Hub", "Village"))
        tracker.updateFromGame(sources("Hub", "Village"))

        assertEquals(listOf("area:null->Hub", "state"), events)
        assertEquals(1, states.size)
        assertEquals("Hub", tracker.state.area)
    }

    @Test
    fun `area changes post before the state change`() {
        tracker.updateFromGame(sources("Hub", "Village"))
        tracker.updateFromGame(sources("Dwarven Mines", "The Forge"))

        assertEquals(
            listOf("area:null->Hub", "state", "area:Hub->Dwarven Mines", "state"),
            events,
        )
        assertEquals(638_769_221L, tracker.state.purse)
    }

    @Test
    fun `reset clears state and announces leaving`() {
        tracker.updateFromGame(sources("Hub", "Village"))

        tracker.reset()

        assertEquals(SkyBlockState.EMPTY, tracker.state)
        assertEquals(listOf("area:Hub->null", "state"), events.takeLast(2))
    }

    @Test
    fun `a loaded dump pauses live updates until reset`() {
        tracker.loadDump(sources("Private Island", "Your Island"))
        tracker.updateFromGame(sources("Dwarven Mines", "The Forge"))

        assertEquals("Private Island", tracker.state.area)
        assertTrue(events.none { it.contains("Dwarven Mines") })

        tracker.reset()

        assertEquals(SkyBlockState.EMPTY, tracker.state)
        tracker.updateFromGame(sources("Hub", "Village"))
        assertEquals("Hub", tracker.state.area)
    }
}
