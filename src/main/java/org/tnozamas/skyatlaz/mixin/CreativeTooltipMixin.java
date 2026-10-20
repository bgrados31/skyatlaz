package org.tnozamas.skyatlaz.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tnozamas.skyatlaz.core.tooltip.TooltipLines;

/**
 * Adds the lines SkyAtlaz features provide (prices) to the creative inventory tooltip. That screen builds its own
 * tooltip instead of using {@code Screen.getTooltipFromItem}, so it needs its own hook.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeTooltipMixin {
    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void skyatlaz$addItemLines(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> extra = TooltipLines.forStack(stack);
        if (extra.isEmpty()) {
            return;
        }
        List<Component> merged = new ArrayList<>(cir.getReturnValue());
        merged.addAll(extra);
        cir.setReturnValue(merged);
    }
}
