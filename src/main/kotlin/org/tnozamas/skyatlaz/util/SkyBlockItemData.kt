package org.tnozamas.skyatlaz.util

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * The SkyBlock data Hypixel stores in an item's `minecraft:custom_data`. Read once per item when needed (cache by
 * [uuid] or stack identity), never per frame.
 * - [id]: SkyBlock item id (`HYPERION`), the key for prices and repo data. Null for vanilla/menu items.
 * - [stars]: dungeon stars (`upgrade_level`, older items `dungeon_item_level`).
 */
data class SkyBlockItemData(
    val id: String?,
    val uuid: String?,
    val reforge: String?,
    val stars: Int,
    val recombobulated: Boolean,
    val hotPotatoBooks: Int,
    val enchantments: Map<String, Int>,
    val timestamp: Long?,
) {
    companion object {
        val EMPTY = SkyBlockItemData(null, null, null, 0, false, 0, emptyMap(), null)

        fun of(stack: ItemStack): SkyBlockItemData = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.let(::of) ?: EMPTY

        fun of(tag: CompoundTag): SkyBlockItemData {
            val enchantments = tag.getCompoundOrEmpty("enchantments")
            return SkyBlockItemData(
                id = tag.getStringOr("id", "").ifEmpty { null },
                uuid = tag.getStringOr("uuid", "").ifEmpty { null },
                reforge = tag.getStringOr("modifier", "").ifEmpty { null },
                stars = tag.getIntOr("upgrade_level", tag.getIntOr("dungeon_item_level", 0)),
                recombobulated = tag.getIntOr("rarity_upgrades", 0) > 0,
                hotPotatoBooks = tag.getIntOr("hot_potato_count", 0),
                enchantments = enchantments.keySet().associateWith { enchantments.getIntOr(it, 0) },
                timestamp = tag.getLongOr("timestamp", 0L).takeIf { it > 0 },
            )
        }
    }
}
