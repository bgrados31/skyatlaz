package org.tnozamas.skyatlaz.api.repo

/** What one unit of an item is worth and where that number comes from. */
data class ItemValue(
    val coins: Double,
    val source: Source,
) {
    /** Ordered by preference when several apply. */
    enum class Source {
        /** Instant-sell price on the bazaar. */
        BAZAAR,

        /** Lowest buy-it-now auction price (only when a provider is enabled). */
        AUCTION,

        /** What an NPC pays. */
        NPC,
    }
}
