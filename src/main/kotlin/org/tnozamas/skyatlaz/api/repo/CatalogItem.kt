package org.tnozamas.skyatlaz.api.repo

import org.tnozamas.skyatlaz.util.Rarity

/** Static facts about one SkyBlock item from Hypixel's item resources. [npcSellPrice] is null when NPCs don't buy it. */
data class CatalogItem(
    val id: String,
    val name: String,
    val rarity: Rarity?,
    val category: String?,
    val npcSellPrice: Double?,
)
