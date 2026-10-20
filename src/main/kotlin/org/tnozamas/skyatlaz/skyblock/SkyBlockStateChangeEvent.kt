package org.tnozamas.skyatlaz.skyblock

import org.tnozamas.skyatlaz.core.event.Event

/**
 * Any parsed [SkyBlockState] changed (area, profile, purse, bits...). Features that only care about one field can
 * still listen here and compare, but preferring [AreaChangeEvent] keeps area-only features cheap. Client thread.
 */
class SkyBlockStateChangeEvent(
    val previous: SkyBlockState,
    val current: SkyBlockState,
) : Event
