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
 * Renders [CommandRegistry.helpFor] as a quiet chat panel:
 * ```
 * ─────────── SkyAtlaz › debug ───────────
 *   Debug
 *     theme     Preview every chat message style
 * ─────────────────────────────────────────
 * ```
 * Names are relative to the page and aligned in one column; the full syntax lives in the hover. Groups and
 * commands without arguments run on click, commands that need arguments are typed into the chat box. The
 * breadcrumb in the title links back to parent pages. Built on demand, never per frame.
 */
object CommandHelp {
    private const val CATEGORY_INDENT = 6
    private const val ENTRY_INDENT = 14
    private const val COLUMN_GAP = 10
    private const val GROUP_MARK = " ›"
    private const val CRUMB_SEPARATOR = " › "
    private const val BULLET = "▸ "
    private const val SPARKLE = "✦ "
    private const val SPARKLE_END = " ✦"
    private const val HOVER_RULE = "─"
    private const val SUBCOMMANDS = "↳ "

    fun show(
        registry: CommandRegistry,
        node: CommandNode,
    ) {
        val sections = registry.helpFor(node)
        val lines = ArrayList<Component>()
        val title =
            Tr
                .verbatim(SPARKLE)
                .withStyle(ACCENT)
                .append(breadcrumb(node))
                .append(Tr.verbatim(SPARKLE_END).withStyle(ACCENT))
        lines.add(ChatLayout.titledRule(title))

        if (sections.isEmpty()) {
            lines.add(ChatLayout.pad(ENTRY_INDENT).append(Tr.tr("command.help.empty").withStyle(MUTED)))
        } else {
            val nameColumn = sections.maxOf { section -> section.commands.maxOf { ChatLayout.width(name(it)) } }
            for (section in sections) {
                lines.add(
                    ChatLayout
                        .pad(CATEGORY_INDENT)
                        .append(Tr.verbatim(section.category.icon + " ").withStyle(ACCENT))
                        .append(Tr.tr(section.category.translationKey).withStyle(CATEGORY)),
                )
                for (command in section.commands) lines.add(entry(registry, command, nameColumn))
            }
        }
        lines.add(ChatLayout.rule(ChatLayout.lineWidth()))
        Chat.block(lines)
    }

    /** `SkyAtlaz › debug › tools`: every ancestor is a link to its own page. */
    private fun breadcrumb(node: CommandNode): MutableComponent {
        val path = generateSequence(node) { it.parent }.toList().asReversed()
        val result = Tr.verbatim("")
        for ((index, step) in path.withIndex()) {
            if (index > 0) result.append(Tr.verbatim(CRUMB_SEPARATOR).withStyle(SUBTLE))
            val label = if (step.isRoot) Tr.tr("chat.prefix").withGradient(Theme.BRAND_GRADIENT) else Tr.verbatim(step.name)
            if (step === node) {
                result.append(if (step.isRoot) label else label.withStyle(TITLE))
            } else {
                val back =
                    Style.EMPTY
                        .withClickEvent(ClickEvent.RunCommand(step.suggestion))
                        .withHoverEvent(HoverEvent.ShowText(Tr.tr("command.help.back", Tr.verbatim(step.usage)).withStyle(MUTED)))
                result.append(label.withStyle(if (step.isRoot) back else back.withColor(Theme.TEXT_MUTED)))
            }
        }
        return result
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
            .pad(ENTRY_INDENT - ChatLayout.width(Tr.verbatim(BULLET)))
            .append(Tr.verbatim(BULLET).withStyle(SUBTLE))
            .append(name.copy())
            .append(ChatLayout.pad(nameColumn - ChatLayout.width(name) + COLUMN_GAP))
            .append(description.copy().withStyle(MUTED))
            .withStyle(style)
    }

    private fun hover(
        registry: CommandRegistry,
        command: CommandNode,
        description: Component,
        action: Action,
    ): Component {
        val usage = Tr.verbatim(command.usage).withGradient(Theme.BRAND_GRADIENT)
        val hover =
            Tr
                .verbatim(command.category.icon + " ")
                .withStyle(ACCENT)
                .append(usage)
                .append(NEWLINE)
                .append(
                    Tr
                        .verbatim(
                            HOVER_RULE.repeat(ChatLayout.width(usage) / ChatLayout.width(Tr.verbatim(HOVER_RULE)) + 2),
                        ).withStyle(SUBTLE),
                ).append(NEWLINE)
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
            isGroup(command) -> {
                Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_open", "➜")
            }

            command.isExecutable && !command.isArgument -> {
                Action(ClickEvent.RunCommand(command.suggestion), "command.help.click_run", "➤")
            }

            else -> {
                Action(ClickEvent.SuggestCommand(command.suggestion + " "), "command.help.click_type", "✎")
            }
        }

    /** A node that only leads to literal subcommands. */
    private fun isGroup(command: CommandNode): Boolean =
        !command.isExecutable && command.childMap.values.any { it.isAlive && !it.isArgument }

    private const val NEWLINE = "\n"
    private val NAME: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val TITLE: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val TEXT: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val MUTED: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED)
    private val SUBTLE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE)
    private val HINT: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED).withItalic(true)
    private val CATEGORY: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
    private val ACCENT: Style = Style.EMPTY.withColor(Theme.BRAND_LAVENDER)
}
