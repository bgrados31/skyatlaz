package org.tnozamas.skyatlaz.core.command

import com.mojang.brigadier.arguments.ArgumentType

/**
 * Declarative description of a command subtree, merged into the [CommandRegistry]:
 * ```
 * literal("debug") {
 *     category(CommandCategory.DEBUG)
 *     description("command.debug")
 *     literal("theme") {
 *         description("command.debug.theme")
 *         executes { Chat.info(...) }
 *     }
 * }
 * ```
 * Every node needs a description key (the help is generated from them). A node without [executes] prints its
 * own help, so no path ever ends in "unknown or incomplete command".
 */
@CommandDsl
class CommandBuilder internal constructor(
    internal val name: String,
    internal val argumentType: ArgumentType<*>?,
) {
    internal var descriptionKey: String? = null
    internal var category: CommandCategory? = null
    internal var visibility: (() -> Boolean)? = null
    internal var executor: ((CommandCall) -> Unit)? = null
    internal var suggestions: (() -> Iterable<String>)? = null
    internal val children = ArrayList<CommandBuilder>()

    /** Translation key (relative, like `Tr.tr`) of the one-line description shown in help and on hover. */
    fun description(key: String) {
        descriptionKey = key
    }

    /** Help section; only meaningful on top-level commands, children inherit it. */
    fun category(category: CommandCategory) {
        this.category = category
    }

    /** Extra condition to show and run this node (e.g. "only on SkyBlock"). Cheap, no side effects. */
    fun visibleWhen(condition: () -> Boolean) {
        visibility = condition
    }

    fun executes(action: (CommandCall) -> Unit) {
        executor = action
    }

    /** Tab-completion values for an argument node. */
    fun suggests(provider: () -> Iterable<String>) {
        check(argumentType != null) { "suggests() is only valid on argument nodes" }
        suggestions = provider
    }

    fun literal(
        name: String,
        block: CommandBuilder.() -> Unit,
    ) {
        children.add(CommandBuilder(name, null).apply(block))
    }

    /** Typed argument, e.g. `argument("amount", IntegerArgumentType.integer(1))`; read with [CommandCall.int]. */
    fun argument(
        name: String,
        type: ArgumentType<*>,
        block: CommandBuilder.() -> Unit,
    ) {
        children.add(CommandBuilder(name, type).apply(block))
    }
}

@DslMarker
annotation class CommandDsl
