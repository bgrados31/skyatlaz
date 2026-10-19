package org.tnozamas.skyatlaz.core.debug

import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.core.command.CommandCategory
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.ui.chat.ChatLayout
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * `/skyatlaz debug ...` tools for developing without Hypixel (docs/TESTING.md). In the DEBUG category, so they are
 * hidden unless debug mode is on (always on in the dev environment).
 */
object DebugCommands {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                category(CommandCategory.DEBUG)
                description("command.debug")
                literal("theme") {
                    description("command.debug.theme")
                    executes { previewTheme() }
                }
                literal("glyphs") {
                    description("command.debug.glyphs")
                    executes { previewGlyphs() }
                }
            }
        }
    }

    /** Candidate decoration glyphs, each followed by its width in font pixels, to pick ones that render well. */
    private fun previewGlyphs() {
        val lines = ArrayList<Component>()
        for (group in GLYPHS) {
            val line = Tr.verbatim("")
            for (glyph in group.split(' ')) {
                line
                    .append(Tr.verbatim(glyph).withColor(Theme.TEXT))
                    .append(Tr.verbatim(ChatLayout.width(Tr.verbatim(glyph)).toString() + "  ").withColor(Theme.TEXT_SUBTLE))
            }
            lines.add(line)
        }
        Chat.block(lines)
    }

    private val GLYPHS =
        listOf(
            "─ ━ ═ ┄ ┈ ╌ ▬ ⎯ ‒ ― ﹘ -",
            "╭ ╮ ╰ ╯ ┌ ┐ └ ┘ ├ ┤ │ ┃ ║ ▏ ▎",
            "▸ ▹ › » • ◦ ∙ · ◆ ◇ ❖ ⬥ ⬦ ▪ ▫",
            "✦ ✧ ★ ☆ ✪ ✯ ❋ ✿ ❀ ✹ ✷ ⁂",
            "✔ ✓ ✖ ✗ ✘ ⚠ ⚡ ℹ ❗ ❕ ❓ ⓘ",
            "⌛ ⏱ ⚙ ✎ ☰ ➜ ➤ ⮞ ↗ ↳ ⟳ ⇄ ⬆ ⬇",
            "⛏ ⚔ ☠ ❤ ♦ ♥ ♠ ♣ ⚗ ☄ ✈ ⚓ ⛃",
        )

    /** Prints every message kind so theme changes can be checked in game. */
    private fun previewTheme() {
        Chat.info(Tr.tr("debug.theme.info"))
        Chat.success(Tr.tr("debug.theme.success"))
        Chat.warning(Tr.tr("debug.theme.warning"))
        Chat.error(Tr.tr("debug.theme.error"))
    }
}
