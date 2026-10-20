package org.tnozamas.skyatlaz.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.core.event.Registration
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement as FabricHudElement

/**
 * Owns every HUD element: registers the single Fabric hook, keeps each element's [HudPlacement] in the config and
 * draws the registered elements each frame. Modules never touch the Fabric registry; they call `ctx.hud(element)`
 * and are unregistered automatically when they stop.
 *
 * An element costs one list iteration and a few field reads per frame while registered; with no elements the hook
 * returns immediately. Positions and scales live in `hud.<element id>.x`, `.y` and `.scale`, registered on first
 * use so the file does not grow for modules that never run.
 *
 * Client thread only.
 */
class HudManager(
    private val config: ConfigManager,
) : FabricHudElement {
    private val elements = ArrayList<HudElement>()
    private val placements = HashMap<String, HudPlacement>()
    private var installed = false

    /** Registers the Fabric HUD hook. Call once, before any module starts. */
    fun install() {
        check(!installed) { "HudManager.install() was called twice" }
        HudElementRegistry.addLast(ID, this)
        installed = true
    }

    /** Makes [element] visible and returns the registration that removes it again. Ids must be unique. */
    fun register(element: HudElement): Registration {
        check(installed) { "HudManager.install() must run before registering elements" }
        require(elements.none { it.id == element.id }) { "Duplicate HUD element id '${element.id}'" }
        element.placement = placements.getOrPut(element.id) { createPlacement(element) }
        elements.add(element)
        var active = true
        return Registration {
            if (active) {
                active = false
                elements.remove(element)
                element.placement = null
            }
        }
    }

    /** Elements in registration order (first drawn first). */
    val activeElements: List<HudElement>
        get() = elements

    /** Draws one element at its configured position and scale. Used by the hook and by the editor. */
    fun draw(
        context: GuiGraphicsExtractor,
        element: HudElement,
    ) {
        val placement = element.layout
        val pose = context.pose()
        pose.pushMatrix()
        pose.translate(
            placement.resolveX(context.guiWidth(), element.width).toFloat(),
            placement.resolveY(context.guiHeight(), element.height).toFloat(),
        )
        pose.scale(placement.scale.toFloat())
        element.render(context)
        pose.popMatrix()
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker,
    ) {
        if (elements.isEmpty()) return
        if (Minecraft.getInstance().gui.screen() is HudEditorScreen) return
        for (element in elements) draw(context, element)
    }

    private fun createPlacement(element: HudElement): HudPlacement {
        val section = LayoutConfig(element)
        config.register(section)
        lateinit var placement: HudPlacement
        placement =
            HudPlacement(
                id = element.id,
                defaultX = element.defaultX,
                defaultY = element.defaultY,
                defaultScale = element.defaultScale,
                x = section.x.value,
                y = section.y.value,
                scale = section.scale.value,
            ) {
                section.x.set(placement.x)
                section.y.set(placement.y)
                section.scale.set(placement.scale)
            }
        return placement
    }

    /** `hud.<id>`: three entries per element, created the first time the element registers. */
    private class LayoutConfig(
        element: HudElement,
    ) : ConfigSection("hud.${element.id}") {
        val x = decimal("x", element.defaultX, 0.0..1.0)
        val y = decimal("y", element.defaultY, 0.0..1.0)
        val scale = decimal("scale", element.defaultScale, HudPlacement.MIN_SCALE..HudPlacement.MAX_SCALE)
    }

    private companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath("skyatlaz", "hud")
    }
}
