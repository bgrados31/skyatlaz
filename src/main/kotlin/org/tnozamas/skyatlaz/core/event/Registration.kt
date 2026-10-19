package org.tnozamas.skyatlaz.core.event

/**
 * Handle returned by anything that registers a callback in a shared system. [unregister] is idempotent.
 * Modules never call it directly: `ModuleContext` tracks registrations and releases them on disable.
 */
fun interface Registration {
    fun unregister()
}
