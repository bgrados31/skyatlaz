package org.tnozamas.skyatlaz.core.event

/**
 * End of every client tick (20 per second). A singleton, so posting allocates nothing. Prefer `ctx.every(n)` for
 * periodic work; listen to this only for work that truly must run every tick.
 */
object ClientTickEvent : Event
