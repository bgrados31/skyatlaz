package org.tnozamas.skyatlaz.api.repo

import org.tnozamas.skyatlaz.core.event.Event

/** New bazaar data or a new item catalog was applied to [PriceRepository]. Recompute price displays here. */
object PricesUpdatedEvent : Event
