package org.tnozamas.skyatlaz.core.tooltip

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * Where a feature adds its own lines to the vanilla item tooltip. The mixin that builds the tooltip asks here; with
 * no provider registered the answer is empty, so a feature that is off costs one check per frame.
 *
 * Providers come and go with their feature, so the tooltip only shows the lines of the features that run.
 * Client thread only.
 */
object TooltipLines {
    private val providers = ArrayList<Provider>()

    /** A feature that wants to add lines to the tooltip of an item. */
    fun interface Provider {
        /** The lines to add for [stack]; empty when there is nothing to say about it. */
        fun lines(stack: ItemStack): List<Component>
    }

    /** Adds a provider. It stays until it is removed. */
    @JvmStatic
    fun install(provider: Provider) {
        if (provider !in providers) providers.add(provider)
    }

    /** Removes a provider; the lines it added disappear. */
    @JvmStatic
    fun remove(provider: Provider) {
        providers.remove(provider)
    }

    /** The extra lines for an item, in the order the providers were installed. */
    @JvmStatic
    fun forStack(stack: ItemStack): List<Component> {
        if (providers.isEmpty()) return emptyList()
        var result: List<Component> = emptyList()
        for (provider in providers) {
            val lines = provider.lines(stack)
            if (lines.isEmpty()) continue
            result = if (result.isEmpty()) lines else result + lines
        }
        return result
    }
}
