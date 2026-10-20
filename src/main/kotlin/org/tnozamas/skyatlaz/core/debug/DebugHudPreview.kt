package org.tnozamas.skyatlaz.core.debug

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.skyblock.SkyBlockState
import org.tnozamas.skyatlaz.skyblock.SkyBlockStateChangeEvent
import org.tnozamas.skyatlaz.skyblock.SkyBlockTracker
import org.tnozamas.skyatlaz.ui.hud.HudElement
import org.tnozamas.skyatlaz.ui.hud.HudManager
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme
import org.tnozamas.skyatlaz.util.Numbers

/**
 * A small SkyBlock state card registered only while debug mode is on. It exists so `/skyatlaz hud` has something to
 * drag and scale during development, and doubles as a quick check of the parsers. It is not a player feature: debug
 * mode is off in production unless the owner turns it on. It sits in the top right corner so it does not overlap the
 * player facing HUD elements.
 */
class DebugHudPreview(
    private val hud: HudManager,
    private val events: EventBus,
    private val skyblock: SkyBlockTracker,
) {
    private val element = PreviewElement()
    private var registrations = emptyList<Registration>()

    fun setActive(active: Boolean) {
        if (active == registrations.isNotEmpty()) return
        registrations =
            if (active) {
                element.update(skyblock.state)
                listOf(
                    hud.register(element),
                    events.register(SkyBlockStateChangeEvent::class.java) { event -> element.update(event.current) },
                )
            } else {
                registrations.reversed().forEach(Registration::unregister)
                emptyList()
            }
    }

    private class PreviewElement : HudElement("debug.state", defaultX = 0.99, defaultY = 0.02) {
        private var lines: List<Line> = emptyList()
        private var state = SkyBlockState.EMPTY
        private var dirty = true

        override var width: Int = 0
            private set

        override var height: Int = 0
            private set

        override fun render(context: GuiGraphicsExtractor) {
            if (dirty) rebuild()
            val font = Minecraft.getInstance().font
            var y = 0
            for (line in lines) {
                context.text(font, line.text, 0, y, Theme.argb(line.color), true)
                y += font.lineHeight + LINE_GAP
            }
        }

        /** Stores the new state; measuring happens on the first frame, when the font exists. */
        fun update(state: SkyBlockState) {
            this.state = state
            dirty = true
        }

        private fun rebuild() {
            dirty = false
            val font = Minecraft.getInstance().font
            lines =
                buildList {
                    add(Line(Tr.tr("hud.debug.title").withGradient(Theme.BRAND_GRADIENT), Theme.TEXT))
                    if (!state.onSkyBlock) {
                        add(Line(Tr.tr("hud.debug.not_on_skyblock"), Theme.TEXT_MUTED))
                    } else {
                        add(Line(Tr.tr("hud.debug.location", value(state.area), value(state.subArea)), Theme.TEXT))
                        add(Line(Tr.tr("hud.debug.player", value(state.profile), value(state.level)), Theme.TEXT_MUTED))
                        state.purse?.let { add(Line(Tr.tr("hud.debug.purse", Tr.verbatim(Numbers.grouped(it))), Theme.TEXT_MUTED)) }
                    }
                }
            width = lines.maxOfOrNull { font.width(it.text) } ?: 0
            height = lines.size * font.lineHeight + (lines.size - 1).coerceAtLeast(0) * LINE_GAP
        }

        private fun value(value: String?): Component = Tr.verbatim(value ?: "-")

        private fun value(value: Int?): Component = Tr.verbatim(value?.toString() ?: "-")
    }

    private data class Line(
        val text: Component,
        val color: Int,
    )

    private companion object {
        const val LINE_GAP = Spacing.GAP
    }
}
