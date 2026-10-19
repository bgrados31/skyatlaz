package org.tnozamas.skyatlaz.core.module

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.event.Event
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration

class ModuleManagerTest {
    private class Tick : Event

    private class TestModule(
        id: String = "test.module",
    ) : Module(id, FeatureCategory.MISC) {
        var active = true
        var throwOnEnable = false
        var throwOnTick = false
        var ticks = 0
        var enables = 0
        var disables = 0
        val tracked = ArrayList<Registration>()

        override fun isActive() = active

        override fun onEnable(ctx: ModuleContext) {
            enables++
            if (throwOnEnable) error("enable failed")
            ctx.on<Tick> {
                if (throwOnTick) error("tick failed")
                ticks++
            }
            tracked.forEach(ctx::track)
        }

        override fun onDisable() {
            disables++
        }
    }

    private val failures = ArrayList<Pair<String, Throwable>>()
    private val bus = EventBus { _, error -> throw AssertionError("unexpected unhandled error", error) }
    private val manager = ModuleManager(bus) { module, error -> failures.add(module.id to error) }
    private val module = TestModule().also(manager::register)

    @Test
    fun `registered modules start disabled and cost nothing`() {
        assertFalse(manager.isRunning(module.id))
        assertFalse(bus.hasListeners(Tick::class.java))
        assertEquals(0, module.enables)
    }

    @Test
    fun `enabling registers listeners and disabling removes all of them`() {
        manager.setEnabled(module.id, true)
        bus.post(Tick())
        assertEquals(1, module.ticks)

        manager.setEnabled(module.id, false)
        bus.post(Tick())

        assertEquals(1, module.ticks)
        assertEquals(1, module.disables)
        assertFalse(bus.hasListeners(Tick::class.java))
    }

    @Test
    fun `tracked registrations from other systems are released on disable`() {
        var released = 0
        module.tracked.add { released++ }
        manager.setEnabled(module.id, true)

        manager.setEnabled(module.id, false)

        assertEquals(1, released)
    }

    @Test
    fun `activation condition gates the module`() {
        module.active = false
        manager.setEnabled(module.id, true)
        assertFalse(manager.isRunning(module.id))

        module.active = true
        manager.refreshActivation()
        assertTrue(manager.isRunning(module.id))

        module.active = false
        manager.refreshActivation()
        assertFalse(manager.isRunning(module.id))
        assertFalse(bus.hasListeners(Tick::class.java))
    }

    @Test
    fun `refresh without changes does not restart running modules`() {
        manager.setEnabled(module.id, true)
        manager.refreshActivation()
        manager.refreshActivation()

        assertEquals(1, module.enables)
    }

    @Test
    fun `a failing listener disables only its module`() {
        val healthy = TestModule("test.healthy").also(manager::register)
        manager.setEnabled(module.id, true)
        manager.setEnabled(healthy.id, true)
        module.throwOnTick = true

        bus.post(Tick())
        bus.post(Tick())

        assertFalse(manager.isRunning(module.id))
        assertNotNull(manager.failure(module.id))
        assertEquals(listOf(module.id), failures.map { it.first })
        assertTrue(manager.isRunning(healthy.id))
        assertEquals(2, healthy.ticks)
    }

    @Test
    fun `a failing enable leaves nothing registered`() {
        module.throwOnEnable = true

        manager.setEnabled(module.id, true)

        assertFalse(manager.isRunning(module.id))
        assertFalse(bus.hasListeners(Tick::class.java))
        assertEquals(1, failures.size)
    }

    @Test
    fun `a failed module stays off until toggled off and on`() {
        module.throwOnEnable = true
        manager.setEnabled(module.id, true)
        module.throwOnEnable = false

        manager.refreshActivation()
        assertFalse(manager.isRunning(module.id))

        manager.setEnabled(module.id, false)
        assertNull(manager.failure(module.id))
        manager.setEnabled(module.id, true)
        assertTrue(manager.isRunning(module.id))
    }

    @Test
    fun `module ids are validated and unique`() {
        assertThrows(IllegalArgumentException::class.java) { TestModule("Bad Id") }
        assertThrows(IllegalArgumentException::class.java) { manager.register(TestModule()) }
        assertThrows(IllegalArgumentException::class.java) { manager.setEnabled("missing", true) }
    }
}
