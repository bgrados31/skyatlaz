package org.tnozamas.skyatlaz.ui.hud

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.i18n.Tr

/**
 * A movable piece of the HUD. Modules register it with `ctx.hud(element)`; [HudManager] owns placement, scaling and
 * the editor, so an element only draws its own content.
 *
 * Drawing is local: render at `(0, 0)` with [width] x [height] pixels and no scale. The manager translates and
 * scales the pose. Keep [width] and [height] updated when the content changes (and cache the components you draw),
 * never build text or measure fonts per frame.
 *
 * [nameKey] names the element in the editor; the default is `hud.<id>.name`.
 */
abstract class HudElement(
    val id: String,
    val nameKey: String = "hud.$id.name",
    val defaultX: Double = 0.01,
    val defaultY: Double = 0.01,
    val defaultScale: Double = 1.0,
) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid HUD element id '$id': lowercase segments separated by dots" }
    }

    /** Built once: language changes during a session are not supported. */
    val name: Component = Tr.tr(nameKey)

    internal var placement: HudPlacement? = null

    /** Position and scale, valid only while the element is registered. */
    val layout: HudPlacement
        get() = checkNotNull(placement) { "HUD element '$id' is not registered" }

    /** Unscaled width in pixels. */
    abstract val width: Int

    /** Unscaled height in pixels. */
    abstract val height: Int

    /** Draws the element at `(0, 0)`; the manager has already applied its position and scale. */
    abstract fun render(context: GuiGraphicsExtractor)

    override fun toString(): String = id

    private companion object {
        val ID_PATTERN = Regex("[a-z0-9_]+(\\.[a-z0-9_]+)*")
    }
}
