package org.tnozamas.skyatlaz.ui.render

import net.minecraft.network.chat.Component

/**
 * A piece of text that is built when its value changes and reused every frame.
 *
 * HUD elements draw on every frame, so building the component each time would allocate on the render path. The
 * cache holds the last value and its component: the same value returns the same component without rebuilding it.
 *
 * Not thread safe: it belongs to the client thread, like everything that renders.
 */
class CachedText<T>(
    private val build: (T) -> Component,
) {
    private var cached: Component = Component.empty()
    private var last: Any? = null
    private var hasValue = false

    /** The component for [value], rebuilt only when the value changed. */
    fun of(value: T): Component {
        if (!hasValue || last != value) {
            hasValue = true
            last = value
            cached = build(value)
        }
        return cached
    }

    /** Forgets the cached value, so the next call rebuilds it. */
    fun invalidate() {
        hasValue = false
        last = null
        cached = Component.empty()
    }
}
