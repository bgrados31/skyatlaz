package org.tnozamas.skyatlaz.core.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventBusTest {
    private class Ping(
        val value: Int = 0,
    ) : Event

    private class Other : Event

    private class Hide : CancellableEvent()

    private val unhandled = ArrayList<Throwable>()
    private val bus = EventBus { _, error -> unhandled.add(error) }

    @Test
    fun `delivers only to listeners of the exact type`() {
        val received = ArrayList<Int>()
        bus.register<Ping> { received.add(it.value) }
        bus.register<Other> { received.add(-1) }

        bus.post(Ping(7))

        assertEquals(listOf(7), received)
    }

    @Test
    fun `hasListeners follows registration`() {
        assertFalse(bus.hasListeners(Ping::class.java))
        val first = bus.register<Ping> {}
        val second = bus.register<Ping> {}
        assertTrue(bus.hasListeners(Ping::class.java))

        first.unregister()
        assertTrue(bus.hasListeners(Ping::class.java))
        second.unregister()
        assertFalse(bus.hasListeners(Ping::class.java))
    }

    @Test
    fun `lower priority runs first and ties keep registration order`() {
        val order = ArrayList<String>()
        bus.register<Ping>(priority = 10) { order.add("late") }
        bus.register<Ping> { order.add("a") }
        bus.register<Ping>(priority = -10) { order.add("early") }
        bus.register<Ping> { order.add("b") }

        bus.post(Ping())

        assertEquals(listOf("early", "a", "b", "late"), order)
    }

    @Test
    fun `unregister is idempotent and does not remove other listeners`() {
        var count = 0
        val registration = bus.register<Ping> { count += 100 }
        bus.register<Ping> { count++ }

        registration.unregister()
        registration.unregister()
        bus.post(Ping())

        assertEquals(1, count)
    }

    @Test
    fun `listener removed during dispatch is skipped in the same post`() {
        var secondCalled = false
        lateinit var second: Registration
        bus.register<Ping> { second.unregister() }
        second = bus.register<Ping> { secondCalled = true }

        bus.post(Ping())

        assertFalse(secondCalled)
    }

    @Test
    fun `a throwing listener goes to its own handler and others still run`() {
        val own = ArrayList<Throwable>()
        var after = false
        bus.register<Ping>(onError = { own.add(it) }) { error("boom") }
        bus.register<Ping> { after = true }

        bus.post(Ping())

        assertEquals(1, own.size)
        assertTrue(unhandled.isEmpty())
        assertTrue(after)
    }

    @Test
    fun `errors without a handler go to the bus fallback`() {
        bus.register<Ping> { throw NoSuchMethodError("game update") }

        bus.post(Ping())

        assertEquals(1, unhandled.size)
    }

    @Test
    fun `post returns the event so callers can read cancellation`() {
        bus.register<Hide> { it.cancel() }
        val event = Hide()

        assertSame(event, bus.post(event))
        assertTrue(event.isCancelled)
    }
}
