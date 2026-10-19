package org.tnozamas.skyatlaz.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to the tab list header and footer, where Hypixel shows profile and event info. */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor("header")
    @Nullable
    Component skyatlaz$getHeader();

    @Accessor("footer")
    @Nullable
    Component skyatlaz$getFooter();
}
