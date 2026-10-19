package org.tnozamas.skyatlaz.core.scheduler

/**
 * Allows an action at most once every [ticks] client ticks (rate-limited requests, repeated warnings). Pass the
 * scheduler's `currentTick`; no clock reads or allocations.
 */
class Cooldown(
    private val ticks: Int,
) {
    private var nextAllowedTick = Long.MIN_VALUE

    fun isReady(now: Long): Boolean = now >= nextAllowedTick

    /** Returns true and starts the cooldown if ready; false while cooling down. */
    fun tryUse(now: Long): Boolean {
        if (!isReady(now)) return false
        nextAllowedTick = now + ticks
        return true
    }

    fun reset() {
        nextAllowedTick = Long.MIN_VALUE
    }
}
