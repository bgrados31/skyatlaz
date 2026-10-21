package org.tnozamas.skyatlaz.api.service

import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.scheduler.Cooldown
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * The profile of the player, read from the skyatlaz service.
 *
 * The repository only works while a feature wants the data: nobody polls when nobody asks for it, so a mod without
 * the service features costs nothing. The session token is kept until it is about to expire, so the mod
 * authenticates once and then only reads the profile.
 *
 * When the service is down the profile stays as it was and the problem is reported once in a while: a feature that
 * uses the profile simply has nothing new to show.
 */
class ServiceRepository(
    private val api: ServiceApi,
    private val scheduler: Scheduler,
    private val bus: EventBus,
    private val onProblem: (String) -> Unit,
) {
    /** The profile of the player, when it is known. */
    var profile: ServiceProfile? = null
        private set

    /** Whether some feature wants the profile right now. */
    val isActive: Boolean get() = users > 0

    private var users = 0
    private var poll: Registration? = null
    private var session: ServiceSession? = null
    private var sessionExpiresAt = 0L
    private var authenticating = false
    private val problemCooldown = Cooldown(PROBLEM_COOLDOWN_TICKS)

    /** Keeps the profile fresh until the returned registration is released. */
    fun use(): Registration {
        users++
        if (users == 1) start()
        var released = false
        return Registration {
            if (!released) {
                released = true
                users--
                if (users == 0) stop()
            }
        }
    }

    private fun start() {
        poll = scheduler.every(POLL_INTERVAL_TICKS, initialDelayTicks = 0) { refresh() }
    }

    private fun stop() {
        poll?.unregister()
        poll = null
        session = null
        sessionExpiresAt = 0L
        profile = null
    }

    private fun refresh() {
        val current = session
        if (current == null || System.currentTimeMillis() >= sessionExpiresAt - SESSION_MARGIN_MILLIS) {
            authenticate()
            return
        }
        api.fetchProfile(current) { result ->
            result.fold(
                onSuccess = { apply(it) },
                onFailure = { report(it) },
            )
        }
    }

    private fun authenticate() {
        if (authenticating) return
        authenticating = true
        api.authenticate { result ->
            authenticating = false
            result.fold(
                onSuccess = {
                    session = it
                    sessionExpiresAt = it.expiresAt
                    refresh()
                },
                onFailure = { report(it) },
            )
        }
    }

    private fun apply(profile: ServiceProfile) {
        if (this.profile == profile) return
        this.profile = profile
        bus.post(ServiceProfileEvent)
    }

    private fun report(error: Throwable) {
        if (!problemCooldown.tryUse(scheduler.currentTick)) return
        onProblem(error.message ?: error.toString())
    }

    private companion object {
        /** The service keeps a profile for five minutes, so a poll every minute is cheap and never in a hurry. */
        const val POLL_INTERVAL_TICKS = 20 * 60

        /** The session token is renewed a minute before it expires. */
        const val SESSION_MARGIN_MILLIS = 60_000L

        /** The same problem is not reported twice in five minutes. */
        const val PROBLEM_COOLDOWN_TICKS = 20 * 60 * 5
    }
}
