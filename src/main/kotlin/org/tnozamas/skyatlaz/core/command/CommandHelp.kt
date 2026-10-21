package org.tnozamas.skyatlaz.core.command

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.ui.chat.ChatLayout
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * Renders [CommandRegistry.helpFor] as a chat panel. Hierarchy comes from typography, not decoration:
 * ```
 * ────────────────── skyatlaz ──────────────────
 *
 *   General
 *       help        List every available command
 *       version     Show the installed skyatlaz version
 *
 *   Debug
 *       debug ›     Developer and offline testing tools
 *
 * ───────────────────────────────────────────────
 * ```
 * - Blank lines frame the content and separate sections; nothing touches the rules.
 * - Fixed color roles: brand gradient only on the name, lavender for headings, white for what can be clicked,
 *   gray for descriptions, dark gray for rules and marks.
 * - A subpage opens with its own name and description and ends with a quiet link back to the parent page.
 * - Names are page-relative and aligned in one column; the full syntax lives in the hover. Groups and commands
 *   without arguments run on click, commands that need arguments are typed into the chat box.
 *
 * Built on demand, never per frame.
 */
object CommandHelp {
    // Indents in font pixels.
    private const val HEADING_INDENT = 6
    private const val ENTRY_INDENT = 20
    private const val COLUMN_GAP = 14

    private const val GROUP_MARK = " ›"
    private const val NEWLINE = "\n"

    fun show(
        registry: CommandRegistry,
        node: CommandNode,
    ) {
        val lines = ArrayList<Component>()
        lines.add(ChatLayout.titledRule(Tr.tr("chat.prefix").withGradient(Theme.BRAND_GRADIENT)))
        lines.add(BLANK)

        if (!node.isRoot) {
            lines.add(ChatLayout.pad(HEADING_INDENT).append(Tr.verbatim(label(node)).withStyle(HEADING)))
            node.descriptionKey?.let { lines.add(ChatLayout.pad(HEADING_INDENT).append(Tr.tr(it).withStyle(MUTED))) }
            lines.add(BLANK)
        }

        val sections = registry.helpFor(node)
        if (sections.isEmpty()) {
            lines.add(ChatLayout.pad(ENTRY_INDENT).append(Tr.tr("command.help.empty").withStyle(MUTED)))
            lines.add(BLANK)
        } else {
            val nameColumn = sections.maxOf { section -> section.commands.maxOf { ChatLayout.width(name(it)) } }
            for (section in sections) {
                // Subpages have a single inherited category, so only the root shows section headings.
                if (node.isRoot) {
                    lines.add(ChatLayout.pad(HEADING_INDENT).append(Tr.tr(section.category.translationKey).withStyle(HEADING)))
                }
                for (command in section.commands) lines.add(entry(registry, command, nameColumn))
                lines.add(BLANK)
            }
        }

        node.parent?.let { lines.add(backLink(it)) }
        lines.add(ChatLayout.rule(ChatLayout.lineWidth()))
        Chat.block(lines)
    }

    /** `« All commands` (or `« debug`), clickable, in the quietest color. */
    private fun backLink(parent: CommandNode): Component {
        val text = if (parent.isRoot) Tr.tr("command.help.back_root") else Tr.tr("command.help.back", Tr.verbatim(label(parent)))
        val style =
            SUBTLE
                .withClickEvent(ClickEvent.RunCommand(parent.suggestion))
                .withHoverEvent(HoverEvent.ShowText(Tr.verbatim(parent.usage).withGradient(Theme.BRAND_GRADIENT)))
        return ChatLayout.pad(HEADING_INDENT).append(text.withStyle(style))
    }

    private fun label(node: CommandNode): String = if (node.isArgument) "<${node.name}>" else node.name

    /** Page-relative name, plus a quiet mark on groups so it is clear they open a page. */
    private fun name(command: CommandNode): MutableComponent {
        val name = Tr.verbatim(label(command)).withStyle(NAME)
        if (isGroup(command)) name.append(Tr.verbatim(GROUP_MARK).withStyle(SUBTLE))
        return name
    }

    private fun entry(
        registry: CommandRegistry,
        command: CommandNode,
        nameColumn: Int,
    ): Component {
        val name = name(command)
        val description = command.descriptionKey?.let(Tr::tr) ?: Tr.verbatim("")
        val action = action(command)
        val style =
            Style.EMPTY
                .withClickEvent(action.click)
                .withHoverEvent(HoverEvent.ShowText(hover(registry, command, description, action)))
        return ChatLayout
            .pad(ENTRY_INDENT)
            .append(name.copy())
            .append(ChatLayout.pad(nameColumn - ChatLayout.width(name) + COLUMN_GAP))
            .append(description.copy().withStyle(MUTED))
            .withStyle(style)
    }

    /**
     * ```
     * /skyatlaz debug theme
     *
     * Preview every chat message style
     * Subcommands: a, b
     *
     * Click to run
     * ```
     */
    private fun hover(
        registry: CommandRegistry,
        command: CommandNode,
        description: Component,
        action: Action,
    ): Component {
        val hover =
            Tr
                .verbatim(command.usage)
                .withGradient(Theme.BRAND_GRADIENT)
                .append(NEWLINE)
                .append(NEWLINE)
                .append(description.copy().withStyle(TEXT))
        val children = command.childMap.values.filter(registry::isAvailable)
        if (children.isNotEmpty()) {
            val names = children.joinToString(", ") { label(it) }
            hover.append(NEWLINE).append(Tr.tr("command.help.subcommands", Tr.verbatim(names)).withStyle(MUTED))
        }
        return hover
            .append(NEWLINE)
            .append(NEWLINE)
            .append(Tr.tr(action.hintKey).withStyle(HINT))
    }

    private class Action(
        val click: ClickEvent,
        val hintKey: String,
    )

    /** Groups open their page, runnable commands run, commands that need input are typed for the player. */
    private fun action(command: CommandNode): Action =
        when {
            isGroup(command) -> Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_open")
            command.isExecutable && !command.isArgument -> Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_run")
            else -> Action(ClickEvent.SuggestCommand(command.suggestion + " "), "command.help.click_type")
        }

    /** A node that only leads to literal subcommands. */
    private fun isGroup(command: CommandNode): Boolean =
        !command.isExecutable && command.childMap.values.any { it.isAlive && !it.isArgument }

    private val BLANK: Component = Tr.verbatim("")
    private val NAME: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val TEXT: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val MUTED: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED)
    private val SUBTLE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE)
    private val HINT: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED).withItalic(true)
    private val HEADING: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
}
