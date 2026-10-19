package org.tnozamas.skyatlaz.api.repo

/**
 * One bazaar product from `quick_status`. Named from the player's side:
 * - [instantBuy]: coins per unit to buy now (Hypixel `buyPrice`);
 * - [instantSell]: coins per unit received when selling now (Hypixel `sellPrice`).
 * Volumes are units currently offered; weekly values are units moved in the last 7 days.
 */
data class BazaarQuote(
    val instantBuy: Double,
    val instantSell: Double,
    val buyVolume: Long,
    val sellVolume: Long,
    val buyWeekly: Long,
    val sellWeekly: Long,
)
