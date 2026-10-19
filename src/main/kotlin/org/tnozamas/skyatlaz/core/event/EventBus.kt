package org.tnozamas.skyatlaz.core.event

import org.tnozamas.skyatlaz.core.error.rethrowIfFatal

/**
 * The single event bus. Designed for hot paths (chat, tick, render):
 * - listeners are kept per event class in arrays; registering copies the array, posting is a plain loop;
 * - [hasListeners] lets mixins skip building an event nobody listens to;
 * - a throwing listener never breaks the others: the error goes to the listener's own handler (a module's
 *   context disables that module) or to [onUnhandledError].
 *
 * Client thread only: register, unregister and post from the client thread. Async work posts back through the
 * scheduler.
 */
class EventBus(
    private val onUnhandledError: (event: Event, error: Throwable) -> Unit,
) {
    private val slots = HashMap<Class<out Event>, Array<Listener>>()

    private class Listener(
        val type: Class<out Event>,
        val priority: Int,
        val callback: (Event) -> Unit,
        val onError: ((Throwable) -> Unit)?,
    ) {
        /** Set on unregister so a post already iterating a snapshot skips it. */
        var removed = false
    }

    /** O(1). Call before allocating an event in a hot path. */
    fun hasListeners(type: Class<out Event>): Boolean = slots.containsKey(type)

    /**
     * Registers [listener] for events of exactly [type]. Lower [priority] runs first; equal priorities keep
     * registration order. [onError] receives exceptions thrown by this listener.
     */
    fun <E : Event> register(
        type: Class<E>,
        priority: Int = 0,
        onError: ((Throwable) -> Unit)? = null,
        listener: (E) -> Unit,
    ): Registration {
        @Suppress("UNCHECKED_CAST")
        val entry = Listener(type, priority, listener as (Event) -> Unit, onError)
        val current = slots[type]
        if (current == null) {
            slots[type] = arrayOf(entry)
        } else {
            var index = current.size
            while (index > 0 && current[index - 1].priority > priority) index--
            val next = arrayOfNulls<Listener>(current.size + 1)
            System.arraycopy(current, 0, next, 0, index)
            next[index] = entry
            System.arraycopy(current, index, next, index + 1, current.size - index)
            @Suppress("UNCHECKED_CAST")
            slots[type] = next as Array<Listener>
        }
        return Registration { remove(entry) }
    }

    inline fun <reified E : Event> register(
        priority: Int = 0,
        noinline onError: ((Throwable) -> Unit)? = null,
        noinline listener: (E) -> Unit,
    ): Registration = register(E::class.java, priority, onError, listener)

    /** Delivers [event] to every listener of its exact class and returns it (to read cancellation/results). */
    fun <E : Event> post(event: E): E {
        val listeners = slots[event.javaClass] ?: return event
        for (listener in listeners) {
            if (listener.removed) continue
            try {
                listener.callback(event)
            } catch (error: Throwable) {
                error.rethrowIfFatal()
                val handler = listener.onError
                if (handler != null) handler(error) else onUnhandledError(event, error)
            }
        }
        return event
    }

    private fun remove(entry: Listener) {
        if (entry.removed) return
        entry.removed = true
        val current = slots[entry.type] ?: return
        if (current.size == 1) {
            slots.remove(entry.type)
            return
        }
        val next = arrayOfNulls<Listener>(current.size - 1)
        var target = 0
        for (listener in current) {
            if (listener !== entry) next[target++] = listener
        }
        @Suppress("UNCHECKED_CAST")
        slots[entry.type] = next as Array<Listener>
    }
}
