package org.tnozamas.skyatlaz.api.service

import org.tnozamas.skyatlaz.core.event.Event

/** A new profile of the player arrived at [ServiceRepository]. Recompute the displays that use it here. */
object ServiceProfileEvent : Event
