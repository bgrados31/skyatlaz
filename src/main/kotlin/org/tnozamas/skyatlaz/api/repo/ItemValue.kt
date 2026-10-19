package org.tnozamas.skyatlaz.api.repo

/** What one unit of an item is worth and where that number comes from. */
data class ItemValue(
    val coins: Double,
    val source: Source,
) {
    /** Ordered by preference when several apply. Auction prices will be added as another source. */
    enum class Source {
        /** Instant-sell price on the bazaar. */
        BAZAAR,

        /** What an NPC pays. */
        NPC,
    }
}
