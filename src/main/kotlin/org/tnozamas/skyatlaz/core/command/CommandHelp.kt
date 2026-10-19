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
 * Renders [CommandRegistry.helpFor] as a tree under a brand title:
 * ```
 * ───────── ✦ SkyAtlaz ✦ ─────────        ───────── ✦ SkyAtlaz ✦ ─────────
 *   General                                  skyatlaz › debug   Developer tools
 *   ├ ✧ help      List every command             └ ✧ theme   Preview styles
 *   └ ✧ version   Show the version
 * ─────────────────────────────────        ─────────────────────────────────
 * ```
 * The root page groups commands by category; a subpage starts with its own path (each ancestor links back) and
 * indents its subcommands further. Names are page-relative and aligned; the full syntax lives in the hover.
 * Groups and commands without arguments run on click, commands that need arguments are typed into the chat box.
 * Built on demand, never per frame.
 */
object CommandHelp {
    // Indents in font pixels.
    private const val HEADER_INDENT = 6
    private const val ROOT_ENTRY_INDENT = 8
    private const val SUB_ENTRY_INDENT = 22
    private const val COLUMN_GAP = 10

    private const val BRANCH = "├ "

    // Curved ╰ renders 3px wide in the game font (├ is 9px) and breaks the column; └ matches ├.
    private const val LAST_BRANCH = "└ "
    private const val STAR = "✧ "
    private const val GROUP_MARK = " ›"
    private const val PATH_SEPARATOR = " › "
    private const val SPARKLE = "✦ "
    private const val SPARKLE_END = " ✦"
    private const val HOVER_RULE = "─"
    private const val SUBCOMMANDS = "↳ "
    private const val NEWLINE = "\n"

    fun show(
        registry: CommandRegistry,
        node: CommandNode,
    ) {
        val lines = ArrayList<Component>()
        lines.add(ChatLayout.titledRule(title()))

        val sections = registry.helpFor(node)
        val entryIndent = if (node.isRoot) ROOT_ENTRY_INDENT else SUB_ENTRY_INDENT
        if (!node.isRoot) lines.add(pathHeader(node))
        if (sections.isEmpty()) {
            lines.add(ChatLayout.pad(entryIndent).append(Tr.tr("command.help.empty").withStyle(MUTED)))
        } else {
            val nameColumn = sections.maxOf { section -> section.commands.maxOf { ChatLayout.width(name(it)) } }
            for (section in sections) {
                if (node.isRoot) {
                    lines.add(ChatLayout.pad(HEADER_INDENT).append(Tr.tr(section.category.translationKey).withStyle(CATEGORY)))
                }
                for ((index, command) in section.commands.withIndex()) {
                    val last = index == section.commands.lastIndex
                    lines.add(entry(registry, command, entryIndent, nameColumn, last))
                }
            }
        }
        lines.add(ChatLayout.rule(ChatLayout.lineWidth()))
        Chat.block(lines)
    }

    private fun title(): MutableComponent =
        Tr
            .verbatim(SPARKLE)
            .withStyle(ACCENT)
            .append(Tr.tr("chat.prefix").withGradient(Theme.BRAND_GRADIENT))
            .append(Tr.verbatim(SPARKLE_END).withStyle(ACCENT))

    /** `skyatlaz › debug   Developer tools`: ancestors link back to their pages, the current one is highlighted. */
    private fun pathHeader(node: CommandNode): Component {
        val path = generateSequence(node) { it.parent }.toList().asReversed()
        val header = ChatLayout.pad(HEADER_INDENT)
        for ((index, step) in path.withIndex()) {
            if (index > 0) header.append(Tr.verbatim(PATH_SEPARATOR).withStyle(SUBTLE))
            val label = Tr.verbatim(if (step.isArgument) "<${step.name}>" else step.name)
            if (step === node) {
                header.append(label.withStyle(CURRENT))
            } else {
                val back =
                    MUTED
                        .withClickEvent(ClickEvent.RunCommand(step.suggestion))
                        .withHoverEvent(HoverEvent.ShowText(Tr.tr("command.help.back", Tr.verbatim(step.usage)).withStyle(MUTED)))
                header.append(label.withStyle(back))
            }
        }
        node.descriptionKey?.let { header.append(ChatLayout.pad(COLUMN_GAP)).append(Tr.tr(it).withStyle(MUTED)) }
        return header
    }

    /** Page-relative name: `theme`, `<amount>`, plus a quiet mark on groups. */
    private fun name(command: CommandNode): MutableComponent {
        val label = Tr.verbatim(if (command.isArgument) "<${command.name}>" else command.name).withStyle(NAME)
        if (isGroup(command)) label.append(Tr.verbatim(GROUP_MARK).withStyle(SUBTLE))
        return label
    }

    private fun entry(
        registry: CommandRegistry,
        command: CommandNode,
        indent: Int,
        nameColumn: Int,
        last: Boolean,
    ): Component {
        val name = name(command)
        val description = command.descriptionKey?.let(Tr::tr) ?: Tr.verbatim("")
        val action = action(command)
        val style =
            Style.EMPTY
                .withClickEvent(action.click)
                .withHoverEvent(HoverEvent.ShowText(hover(registry, command, description, action)))
        return ChatLayout
            .pad(indent)
            .append(connector(last))
            .append(Tr.verbatim(STAR).withStyle(ACCENT))
            .append(name.copy())
            .append(ChatLayout.pad(nameColumn - ChatLayout.width(name) + COLUMN_GAP))
            .append(description.copy().withStyle(MUTED))
            .withStyle(style)
    }

    /** Branch glyph in a fixed-width cell, so names line up whatever glyph the font draws. */
    private fun connector(last: Boolean): Component {
        val glyph = Tr.verbatim(if (last) LAST_BRANCH else BRANCH)
        val cell = maxOf(ChatLayout.width(Tr.verbatim(BRANCH)), ChatLayout.width(Tr.verbatim(LAST_BRANCH)))
        return glyph.withStyle(SUBTLE).append(ChatLayout.pad(cell - ChatLayout.width(glyph)))
    }

    private fun hover(
        registry: CommandRegistry,
        command: CommandNode,
        description: Component,
        action: Action,
    ): Component {
        val usage = Tr.verbatim(command.usage).withGradient(Theme.BRAND_GRADIENT)
        val ruleLength = ChatLayout.width(usage) / ChatLayout.width(Tr.verbatim(HOVER_RULE)) + 1
        val hover =
            usage
                .append(NEWLINE)
                .append(Tr.verbatim(HOVER_RULE.repeat(ruleLength)).withStyle(SUBTLE))
                .append(NEWLINE)
                .append(description.copy().withStyle(TEXT))
        val children = command.childMap.values.filter(registry::isAvailable)
        if (children.isNotEmpty()) {
            val names = children.joinToString(" · ") { if (it.isArgument) "<${it.name}>" else it.name }
            hover
                .append(NEWLINE)
                .append(Tr.verbatim(SUBCOMMANDS).withStyle(SUBTLE))
                .append(Tr.verbatim(names).withStyle(MUTED))
        }
        return hover
            .append(NEWLINE)
            .append(NEWLINE)
            .append(Tr.verbatim(action.icon + " ").withStyle(ACCENT))
            .append(Tr.tr(action.hintKey).withStyle(HINT))
    }

    private class Action(
        val click: ClickEvent,
        val hintKey: String,
        val icon: String,
    )

    /** Groups open their page, runnable commands run, commands that need input are typed for the player. */
    private fun action(command: CommandNode): Action =
        when {
            isGroup(command) -> Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_open", "➜")
            command.isExecutable && !command.isArgument -> Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_run", "➤")
            else -> Action(ClickEvent.SuggestCommand(command.suggestion + " "), "command.help.click_type", "✎")
        }

    /** A node that only leads to literal subcommands. */
    private fun isGroup(command: CommandNode): Boolean =
        !command.isExecutable && command.childMap.values.any { it.isAlive && !it.isArgument }

    private val NAME: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val CURRENT: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
    private val TEXT: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val MUTED: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED)
    private val SUBTLE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE)
    private val HINT: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED).withItalic(true)
    private val CATEGORY: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
    private val ACCENT: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
}
