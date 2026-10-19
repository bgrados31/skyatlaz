package org.tnozamas.skyatlaz.core.event

/**
 * Base type of everything posted on the [EventBus]. Listeners are matched by the exact event class (no subtype
 * dispatch), so lookups stay a single map access.
 */
interface Event
