package org.tnozamas.skyatlaz.core.debug

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.TagParser
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.util.LegacyText

/** Converts between live item stacks and [Snapshot.Item], keeping what SkyBlock parsers read. */
object ItemSnapshots {
    fun capture(stack: ItemStack): Snapshot.Item? {
        if (stack.isEmpty) return null
        return Snapshot.Item(
            id = BuiltInRegistries.ITEM.getKey(stack.item).toString(),
            count = stack.count,
            name = LegacyText.encode(stack.hoverName),
            lore = stack.get(DataComponents.LORE)?.lines()?.map(LegacyText::encode) ?: emptyList(),
            customData = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.toString(),
        )
    }

    /**
     * Rebuilds a display copy of a captured item (name, lore, custom data). Legacy `§` codes in the texts render as
     * formatting, exactly as Hypixel sends them.
     */
    fun restore(item: Snapshot.Item): ItemStack {
        val type = BuiltInRegistries.ITEM.getValue(Identifier.parse(item.id))
        val stack = ItemStack(type, item.count.coerceAtLeast(1))
        stack.set(DataComponents.CUSTOM_NAME, Tr.verbatim(item.name).withStyle { it.withItalic(false) })
        stack.set(DataComponents.LORE, ItemLore(item.lore.map { line -> Tr.verbatim(line).withStyle { it.withItalic(false) } }))
        item.customData?.let { snbt ->
            runCatching { TagParser.parseCompoundFully(snbt) }.getOrNull()?.let { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(it)) }
        }
        return stack
    }
}
