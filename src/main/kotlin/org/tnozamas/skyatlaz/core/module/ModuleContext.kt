package org.tnozamas.skyatlaz.core.module

import org.tnozamas.skyatlaz.core.event.Event
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * What a running module uses to hook into shared systems. Every registration is tracked and released when the
 * module stops, so modules never unregister by hand and a disabled module leaves nothing behind.
 *
 * Errors thrown by the module's listeners are routed to its manager, which disables the module.
 */
class ModuleContext internal constructor(
    @PublishedApi internal val events: EventBus,
    private val scheduler: Scheduler,
    @PublishedApi internal val onError: (Throwable) -> Unit,
) {
    private val registrations = ArrayList<Registration>()
    private var closed = false

    /** Listens to events of exactly [type] while the module runs. Lower [priority] runs first. */
    fun <E : Event> on(
        type: Class<E>,
        priority: Int = 0,
        listener: (E) -> Unit,
    ) {
        track(events.register(type, priority, onError, listener))
    }

    inline fun <reified E : Event> on(
        priority: Int = 0,
        noinline listener: (E) -> Unit,
    ) = on(E::class.java, priority, listener)

    /** Runs [action] once after [delayTicks] ticks, unless the module stops first. */
    fun later(
        delayTicks: Int,
        action: () -> Unit,
    ) {
        track(scheduler.later(delayTicks, onError, action))
    }

    /** Runs [action] every [intervalTicks] ticks while the module runs. The way to throttle expensive checks. */
    fun every(
        intervalTicks: Int,
        initialDelayTicks: Int = intervalTicks,
        action: () -> Unit,
    ) {
        track(scheduler.every(intervalTicks, initialDelayTicks, onError, action))
    }

    /** Runs [work] in the background; [onResult] runs on the client thread only if the module is still running. */
    fun <T> async(
        work: () -> T,
        onResult: (T) -> Unit,
    ) {
        scheduler.async(work, { if (!closed) onError(it) }) { if (!closed) onResult(it) }
    }

    /**
     * Ties a registration from another shared system (HUD, commands, scheduler) to the module's lifetime.
     * Registering after the module stopped (e.g. from a late async callback) releases it immediately.
     */
    fun track(registration: Registration) {
        if (closed) registration.unregister() else registrations.add(registration)
    }

    /** Releases every registration, newest first. */
    internal fun close() {
        closed = true
        for (index in registrations.indices.reversed()) registrations[index].unregister()
        registrations.clear()
    }
}
