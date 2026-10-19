package org.tnozamas.skyatlaz.core.command

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * Renders [CommandRegistry.helpFor] as a chat panel. Every entry is clickable (types the command into the chat
 * box) and shows its description and syntax on hover. Built on demand; nothing is cached across languages.
 */
object CommandHelp {
    private const val INDENT = "  "
    private const val GROUP_MARK = " ›"

    fun show(
        registry: CommandRegistry,
        node: CommandNode,
    ) {
        val title =
            if (node.isRoot) Tr.tr("command.help.title") else Tr.tr("command.help.title_group", Tr.verbatim(node.usage))
        val sections = registry.helpFor(node)
        val lines = ArrayList<Component>()
        if (sections.isEmpty()) {
            lines.add(Tr.tr("command.help.empty").withStyle(MUTED))
        }
        for (section in sections) {
            if (node.isRoot) lines.add(Tr.tr(section.category.translationKey).withStyle(CATEGORY))
            for (command in section.commands) lines.add(entry(command))
        }
        lines.add(Tr.tr("command.help.hint").withStyle(HINT))
        Chat.panel(title, lines)
    }

    private fun entry(command: CommandNode): Component {
        val description = command.descriptionKey?.let(Tr::tr) ?: Tr.verbatim(command.name)
        val usage = Tr.verbatim(command.usage).withStyle(USAGE)
        if (command.childMap.isNotEmpty() && !command.isExecutable) usage.append(GROUP_MARK)
        val hover =
            Tr
                .verbatim("")
                .append(Tr.verbatim(command.usage).withStyle(USAGE))
                .append("\n")
                .append(description.copy().withStyle(TEXT))
                .append("\n")
                .append(Tr.tr("command.help.click").withStyle(HINT))
        val style =
            Style.EMPTY
                .withClickEvent(ClickEvent.SuggestCommand(command.suggestion + " "))
                .withHoverEvent(HoverEvent.ShowText(hover))
        return Tr
            .verbatim(INDENT)
            .append(usage)
            .append(Tr.verbatim("  "))
            .append(description.copy().withStyle(MUTED))
            .withStyle(style)
    }

    private val USAGE: Style = Style.EMPTY.withColor(Theme.BRAND_CYAN)
    private val TEXT: Style = Style.EMPTY.withColor(Theme.TEXT)
    private val MUTED: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED)
    private val HINT: Style = Style.EMPTY.withColor(Theme.TEXT_MUTED).withItalic(true)
    private val CATEGORY: Style = Style.EMPTY.withColor(Theme.BRAND_VIOLET).withBold(true)
}
