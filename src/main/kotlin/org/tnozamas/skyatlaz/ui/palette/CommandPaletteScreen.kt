package org.tnozamas.skyatlaz.ui.palette

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.tnozamas.skyatlaz.core.command.CommandNode
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.ui.component.UiList
import org.tnozamas.skyatlaz.ui.component.UiMetrics
import org.tnozamas.skyatlaz.ui.component.UiNote
import org.tnozamas.skyatlaz.ui.component.UiScreen
import org.tnozamas.skyatlaz.ui.component.UiTextField
import org.tnozamas.skyatlaz.ui.component.UiWidget
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * Command palette: a fuzzy search over the commands of the mod, opened with a key (Ctrl+K by default).
 *
 * The player types a few letters and the commands that match appear, best first. Enter runs the selected one; a
 * command that needs an argument is typed into the chat instead, with the cursor ready for the argument.
 *
 * The palette only reads the command tree ([CommandRegistry]) and never changes it, so it works the same with the
 * commands of the core and with the ones a module adds while it runs.
 */
class CommandPaletteScreen(
    private val registry: CommandRegistry,
    private val scheduler: Scheduler,
    triggerKey: Int,
) : UiScreen("palette.title") {
    private val heading = Tr.tr("palette.title").withGradient(Theme.BRAND_GRADIENT)
    private val hint = Tr.tr("palette.hint")

    private val list = UiList()
    private var query = ""
    private var entries: List<PaletteEntry> = emptyList()
    private var matches: List<PaletteEntry> = emptyList()
    private var selected = 0

    private val search =
        UiTextField(
            labelKey = null,
            placeholderKey = "palette.search",
            get = { query },
            onChange = {
                query = it
                refresh()
            },
            onSubmit = { runSelected() },
        )

    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0

    /** The moment the palette opened, to ignore the key that opened it (it arrives as a character too). */
    private var openedAt = 0L

    /** The character the key that opens the palette produces, so it does not land in the search field. */
    private val triggerChar: Char? = KeyCharacters.of(triggerKey)

    override fun init() {
        entries = buildEntries()
        openedAt = System.currentTimeMillis()
        panelWidth = minOf(430, width - 40)
        panelHeight = minOf(250, height - 40)
        panelX = (width - panelWidth) / 2
        panelY = (height - panelHeight) / 2

        search.layout(panelX + MARGIN, panelY + 28, panelWidth - MARGIN * 2, font)
        list.layout(panelX + MARGIN, panelY + 60, panelWidth - MARGIN * 2, panelHeight - 60 - 30, font)
        root.setWidgets(listOf(search, list))
        root.focus(search)
        refresh()
    }

    override fun drawChrome(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        context.fill(panelX + 2, panelY + 3, panelX + panelWidth + 2, panelY + panelHeight + 2, Theme.argb(Theme.SCREEN_DIM, 110))
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, Theme.argb(Theme.SURFACE, 248))
        context.outline(panelX, panelY, panelWidth, panelHeight, Theme.argb(Theme.BORDER, 220))
        context.text(font, heading, panelX + MARGIN, panelY + 11, Theme.argb(Theme.TEXT))
        context.fill(
            panelX + MARGIN,
            panelY + panelHeight - 24,
            panelX + panelWidth - MARGIN,
            panelY + panelHeight - 23,
            Theme.argb(Theme.BORDER, 170),
        )
        context.text(font, hint, panelX + MARGIN, panelY + panelHeight - 16, Theme.argb(Theme.TEXT_SUBTLE))
    }

    /** The arrows move the selection; the field keeps the focus so the player can keep typing. */
    override fun keyPressed(event: KeyEvent): Boolean =
        when (event.key()) {
            InputConstants.KEY_UP -> {
                move(-1)
                true
            }

            InputConstants.KEY_DOWN -> {
                move(1)
                true
            }

            else -> {
                super.keyPressed(event)
            }
        }

    /**
     * The key that opened the palette is also delivered as a character, so it would land in the search field. The
     * character of that key is dropped when it arrives right after opening; a character typed later is kept.
     */
    override fun charTyped(event: CharacterEvent): Boolean {
        val char = event.codepointAsString().singleOrNull()
        val trigger = triggerChar
        val isTrigger =
            char != null &&
                trigger != null &&
                char.equals(trigger, ignoreCase = true) &&
                System.currentTimeMillis() - openedAt < TRIGGER_WINDOW_MS
        if (isTrigger) return true
        return super.charTyped(event)
    }

    private fun move(delta: Int) {
        if (matches.isEmpty()) return
        selected = Math.floorMod(selected + delta, matches.size)
        revealSelection()
    }

    private fun refresh() {
        matches = CommandPaletteLogic.search(entries, query)
        selected = selected.coerceIn(0, maxOf(0, matches.size - 1))
        list.setWidgets(
            if (matches.isEmpty()) {
                listOf(UiNote("palette.no_results"))
            } else {
                matches.mapIndexed { index, entry -> PaletteRow(entry, { index == selected }, { pick(entry) }) }
            },
        )
        list.scrollToTop()
    }

    /** Scrolls the list just enough to show the selected row. */
    private fun revealSelection() {
        val row = list.widgets.getOrNull(selected) ?: return
        list.reveal(row, font)
    }

    private fun runSelected() {
        val entry = matches.getOrNull(selected) ?: return
        pick(entry)
    }

    /** Runs the command, or types it in the chat when it still needs an argument. */
    private fun pick(entry: PaletteEntry) {
        onClose()
        if (entry.needsInput) {
            scheduler.later(1) { Minecraft.getInstance().setScreenAndShow(ChatScreen(entry.command + " ", false)) }
            return
        }
        val connection = Minecraft.getInstance().player?.connection
        if (connection == null) {
            Chat.error(Tr.tr("palette.failed"))
            return
        }
        // The same path the chat uses: the client dispatcher picks the command up before the server sees it.
        connection.sendCommand(entry.command.removePrefix("/"))
    }

    /**
     * The commands the palette offers: everything that does something, plus everything that asks for an argument.
     * The groups are skipped: their children are the commands the player wants.
     */
    private fun buildEntries(): List<PaletteEntry> {
        val result = ArrayList<PaletteEntry>()
        collect(registry.root, result, emptyList())
        return result
    }

    private fun collect(
        node: CommandNode,
        into: MutableList<PaletteEntry>,
        path: List<String>,
    ) {
        for (child in node.childMap.values) {
            if (!registry.isAvailable(child) || child.isArgument) continue
            val children = path + child.name
            val arguments = child.childMap.values.filter { it.isArgument }
            if (child.isExecutable || arguments.isNotEmpty()) {
                val label =
                    children.joinToString(" ") +
                        arguments.joinToString("") { " <${it.name}>" }
                into.add(
                    PaletteEntry(
                        label = label,
                        description = child.descriptionKey?.let { Tr.tr(it).string } ?: "",
                        command = child.suggestion,
                        needsInput = arguments.isNotEmpty(),
                    ),
                )
            }
            collect(child, into, children)
        }
    }

    /** One row: the command on the left, what it does on the right. */
    private class PaletteRow(
        private val entry: PaletteEntry,
        private val isSelected: () -> Boolean,
        private val onPick: () -> Unit,
    ) : UiWidget() {
        override val focusable: Boolean get() = false

        override fun measure(font: Font): Int = UiMetrics.ROW_HEIGHT

        override fun render(
            ctx: GuiGraphicsExtractor,
            font: Font,
            mouseX: Int,
            mouseY: Int,
        ) {
            val selected = isSelected()
            val hovered = contains(mouseX, mouseY)
            if (selected || hovered) {
                ctx.fill(
                    x - 6,
                    y,
                    x + width,
                    y + height,
                    Theme.argb(if (selected) Theme.SURFACE_RAISED else Theme.SURFACE, if (selected) 210 else 120),
                )
            }
            if (selected) ctx.fill(x - 6, y, x - 5, y + height, Theme.argb(Theme.BRAND_AQUA, 200))
            val textY = labelY(font)
            // The name takes the left half and the description the rest, so a long description never runs into it.
            val available = width - 12
            val labelText = font.plainSubstrByWidth(entry.label, (available * LABEL_SHARE).toInt())
            val labelWidth = font.width(labelText)
            ctx.text(font, Tr.verbatim(labelText), x + 4, textY, Theme.argb(Theme.TEXT), false)
            if (entry.description.isNotEmpty()) {
                val descriptionText = font.plainSubstrByWidth(entry.description, (available - labelWidth - 8).coerceAtLeast(0))
                val descriptionWidth = font.width(descriptionText)
                ctx.text(
                    font,
                    Tr.verbatim(descriptionText),
                    x + width - 6 - descriptionWidth,
                    textY,
                    Theme.argb(Theme.TEXT_MUTED),
                    false,
                )
            }
        }

        override fun mouseClicked(event: MouseButtonEvent): Boolean {
            if (!contains(event.x().toInt(), event.y().toInt())) return false
            onPick()
            return true
        }

        override val searchText: String get() = entry.label
    }

    private companion object {
        const val MARGIN = Spacing.MARGIN

        /** The key that opens the palette arrives as a character a frame later: that window is dropped. */
        const val TRIGGER_WINDOW_MS = 300L

        /** The share of the row the command name takes; the rest is for the description. */
        const val LABEL_SHARE = 0.4
    }
}
