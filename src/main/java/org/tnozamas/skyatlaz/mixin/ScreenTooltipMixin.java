package org.tnozamas.skyatlaz.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tnozamas.skyatlaz.core.tooltip.TooltipLines;

/** Adds the lines skyatlaz features provide (prices) to the vanilla item tooltip. */
@Mixin(Screen.class)
public class ScreenTooltipMixin {
    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void skyatlaz$addItemLines(
            Minecraft client, ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> extra = TooltipLines.forStack(stack);
        if (extra.isEmpty()) {
            return;
        }
        List<Component> merged = new ArrayList<>(cir.getReturnValue());
        merged.addAll(extra);
        cir.setReturnValue(merged);
    }
}
