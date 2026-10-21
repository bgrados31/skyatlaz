package org.tnozamas.skyatlaz.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/** The profile of the player, read from the service. */
class ServiceRepositoryTest {
    private val background = ArrayList<Runnable>()
    private val scheduler = Scheduler({ background.add(it) }) { throw AssertionError(it) }
    private val bus = EventBus { _, error -> throw AssertionError(error) }
    private val problems = ArrayList<String>()

    /** A service that answers what the test wants. */
    private class FakeApi : ServiceApi {
        var session = ServiceSession("token", Long.MAX_VALUE)
        var profile =
            ServiceProfile(
                uuid = "01234567-89ab-cdef-0123-456789abcdef",
                name = "TNOZamas",
                mining = MiningState(mapOf("mining_speed" to 50), mapOf("mining_speed" to true), "pickobulus", emptyMap()),
                dungeons = null,
                currencies = null,
                fetchedAt = 1,
            )
        var authCalls = 0
        var profileCalls = 0
        var failure: Throwable? = null

        override fun authenticate(callback: (Result<ServiceSession>) -> Unit) {
            authCalls++
            val error = failure
            if (error != null) callback(Result.failure(error)) else callback(Result.success(session))
        }

        override fun fetchProfile(
            session: ServiceSession,
            callback: (Result<ServiceProfile>) -> Unit,
        ) {
            profileCalls++
            val error = failure
            if (error != null) callback(Result.failure(error)) else callback(Result.success(profile))
        }
    }

    private val api = FakeApi()
    private val repository = ServiceRepository(api, scheduler, bus, onProblem = { problems.add(it) })

    /** Runs the pending background work and one client tick. */
    private fun tick(ticks: Int = 1) {
        repeat(ticks) {
            val pending = background.toList()
            background.clear()
            pending.forEach { it.run() }
            scheduler.tick()
        }
    }

    @Test
    fun `the profile arrives and the event is posted`() {
        var events = 0
        bus.register<ServiceProfileEvent> { events++ }

        val use = repository.use()
        tick()

        assertNotNull(repository.profile)
        assertEquals(
            50,
            repository.profile
                ?.mining
                ?.perks
                ?.get("mining_speed"),
        )
        assertEquals(1, events)

        use.unregister()
    }

    @Test
    fun `the session is reused between polls`() {
        val use = repository.use()
        tick()
        val authentications = api.authCalls

        tick(20 * 60)

        assertEquals(authentications, api.authCalls, "the session should be reused")
        assertTrue(api.profileCalls > 1, "the profile should be read again")

        use.unregister()
    }

    @Test
    fun `a broken service keeps the last profile and reports the problem once`() {
        val use = repository.use()
        tick()
        assertNotNull(repository.profile)

        api.failure = IllegalStateException("the service is down")
        tick(20 * 60)

        assertNotNull(repository.profile, "the last known profile should stay")
        assertEquals(1, problems.size, "the problem should be reported once, not every poll")

        use.unregister()
    }

    @Test
    fun `nothing polls when nobody uses the profile`() {
        assertNull(repository.profile)
        assertTrue(!repository.isActive)

        tick(20 * 60)

        assertEquals(0, api.authCalls, "the service should not be asked for anything")
        assertNull(repository.profile)
    }

    @Test
    fun `the profile is forgotten when the last user is gone`() {
        val use = repository.use()
        tick()
        assertNotNull(repository.profile)

        use.unregister()

        assertNull(repository.profile)
        assertTrue(!repository.isActive)
    }
}
