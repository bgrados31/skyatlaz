package org.tnozamas.skyatlaz.core.event

/**
 * Event whose vanilla behavior can be suppressed (e.g. hiding a chat line). Every listener still runs, so a
 * listener that only observes should check [isCancelled] itself.
 */
abstract class CancellableEvent : Event {
    var isCancelled: Boolean = false
        private set

    fun cancel() {
        isCancelled = true
    }
}
