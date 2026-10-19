package org.tnozamas.skyatlaz.core.scheduler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchedulerTest {
    private val unhandled = ArrayList<Throwable>()
    private val pending = ArrayList<Runnable>()

    /** Background work is captured and run by hand, so tests control when it "finishes". */
    private val scheduler = Scheduler({ pending.add(it) }) { unhandled.add(it) }

    private fun ticks(count: Int) = repeat(count) { scheduler.tick() }

    private fun finishBackgroundWork() {
        pending.forEach(Runnable::run)
        pending.clear()
    }

    @Test
    fun `later runs once after the delay`() {
        var runs = 0
        scheduler.later(2) { runs++ }

        ticks(2)
        assertEquals(0, runs)
        ticks(1)
        assertEquals(1, runs)
        ticks(5)
        assertEquals(1, runs)
    }

    @Test
    fun `later with zero delay runs on the next tick`() {
        var runs = 0
        scheduler.later(0) { runs++ }

        ticks(1)

        assertEquals(1, runs)
    }

    @Test
    fun `every repeats at the interval`() {
        val ranAt = ArrayList<Long>()
        scheduler.every(intervalTicks = 3, initialDelayTicks = 0) { ranAt.add(scheduler.currentTick) }

        ticks(10)

        assertEquals(listOf(0L, 3L, 6L, 9L), ranAt)
    }

    @Test
    fun `unregister stops a repeating task`() {
        var runs = 0
        val registration = scheduler.every(1) { runs++ }

        ticks(3)
        registration.unregister()
        ticks(3)

        assertEquals(2, runs)
    }

    @Test
    fun `tasks scheduled during a tick wait for the next tick`() {
        val order = ArrayList<String>()
        scheduler.later(0) {
            order.add("outer")
            scheduler.later(0) { order.add("inner") }
        }

        ticks(1)
        assertEquals(listOf("outer"), order)
        ticks(1)
        assertEquals(listOf("outer", "inner"), order)
    }

    @Test
    fun `a failing task reports to its handler and does not stop others`() {
        val own = ArrayList<Throwable>()
        var other = 0
        scheduler.every(1, onError = { own.add(it) }) { error("boom") }
        scheduler.every(1) { other++ }

        ticks(3)

        assertEquals(2, own.size)
        assertEquals(2, other)
        assertTrue(unhandled.isEmpty())
    }

    @Test
    fun `async delivers the result on the next client tick`() {
        var result: Int? = null
        scheduler.async({ 21 * 2 }) { result = it }

        finishBackgroundWork()
        assertEquals(null, result)
        ticks(1)
        assertEquals(42, result)
    }

    @Test
    fun `async errors come back to the client thread handler`() {
        val errors = ArrayList<Throwable>()
        scheduler.async({ error("network down") }, onError = { errors.add(it) }) {}

        finishBackgroundWork()
        ticks(1)

        assertEquals(1, errors.size)
    }

    @Test
    fun `many cancelled tasks are compacted away`() {
        val registrations = (1..100).map { scheduler.every(1) {} }
        var survivor = 0
        scheduler.every(1) { survivor++ }

        registrations.forEach { it.unregister() }
        ticks(2)

        assertEquals(1, survivor)
    }
}
