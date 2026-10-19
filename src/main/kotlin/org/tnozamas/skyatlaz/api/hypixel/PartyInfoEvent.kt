package org.tnozamas.skyatlaz.api.hypixel

import org.tnozamas.skyatlaz.core.event.Event

/** A party info answer arrived (after `HypixelModApi.requestParty`). Client thread. */
class PartyInfoEvent(
    val party: HypixelParty,
) : Event
