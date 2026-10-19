package org.tnozamas.skyatlaz.core.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import org.tnozamas.skyatlaz.core.error.rethrowIfFatal
import org.tnozamas.skyatlaz.core.event.Registration

/**
 * Every command of the mod lives under one root (`/skyatlaz`) in this tree. Commands are declared, never wired by
 * hand: [register] merges a [CommandBuilder] subtree and returns a [Registration] (modules use
 * `ctx.command { }`, so their commands exist only while they run). From the tree the registry generates:
 * - the Brigadier nodes ([install]), with availability checks so hidden or removed commands neither run nor
 *   show up in tab completion;
 * - the help ([helpFor]): any node without its own action prints its children grouped by [CommandCategory].
 *
 * Pure logic, no Minecraft: [onHelp] and [onError] are wired to chat by the entrypoint. Client thread only.
 */
class CommandRegistry(
    rootName: String,
    rootDescriptionKey: String,
    private val debugEnabled: () -> Boolean,
    private val onHelp: (CommandNode) -> Unit,
    private val onError: (CommandNode, Throwable) -> Unit,
) {
    val root = CommandNode(rootName, null, null).apply { descriptionKey = rootDescriptionKey }

    /**
     * Called whenever commands appear, disappear or change visibility. The installer merges new nodes into the live
     * dispatcher and refreshes tab completion (Fabric filters suggestions by availability only on refresh).
     */
    var onTreeChanged: (() -> Unit)? = null

    /** One help section: a category and its available commands, in registration order. */
    data class HelpSection(
        val category: CommandCategory,
        val commands: List<CommandNode>,
    )

    /**
     * Merges the commands declared in [block] (top-level `literal(...)` calls go under the root). Validation runs
     * before anything changes, so a bad declaration throws and leaves the tree untouched.
     */
    fun register(block: CommandBuilder.() -> Unit): Registration {
        val spec = CommandBuilder(root.name, null).apply(block)
        require(spec.executor == null && spec.descriptionKey == null) { "Declare commands inside literal(...)" }
        for (child in spec.children) validate(child, root)
        val touched = ArrayList<Pair<CommandNode, Boolean>>()
        for (child in spec.children) apply(child, root, touched)
        refresh()
        var active = true
        return Registration {
            if (active) {
                active = false
                release(touched)
                refresh()
            }
        }
    }

    /** Re-publishes availability. Call when a `visibleWhen` condition may have changed (island change...). */
    fun refresh() {
        onTreeChanged?.invoke()
    }

    /** Whether [node] may run and be suggested right now. */
    fun isAvailable(node: CommandNode): Boolean {
        var current: CommandNode? = node
        while (current != null) {
            if (!current.isAlive) return false
            if (current.visibility?.invoke() == false) return false
            current = current.parent
        }
        return node.category != CommandCategory.DEBUG || debugEnabled()
    }

    /** Available children of [node], grouped by category in declaration order. Empty when there is nothing. */
    fun helpFor(node: CommandNode): List<HelpSection> {
        val sections = ArrayList<HelpSection>()
        for (category in CommandCategory.entries) {
            val commands = node.childMap.values.filter { it.category == category && isAvailable(it) }
            if (commands.isNotEmpty()) sections.add(HelpSection(category, commands))
        }
        return sections
    }

    /** Adds the whole tree to [dispatcher]. Safe to call again: Brigadier merges nodes with the same name. */
    fun <S> install(dispatcher: CommandDispatcher<S>) {
        @Suppress("UNCHECKED_CAST")
        dispatcher.register(build<S>(root) as LiteralArgumentBuilder<S>)
    }

    private fun <S> build(node: CommandNode): ArgumentBuilder<S, *> {
        val builder: ArgumentBuilder<S, *> =
            if (node.argumentType == null) {
                LiteralArgumentBuilder.literal(node.name)
            } else {
                @Suppress("UNCHECKED_CAST")
                RequiredArgumentBuilder.argument<S, Any>(node.name, node.argumentType as ArgumentType<Any>).suggests { _, builder ->
                    val remaining = builder.remainingLowerCase
                    node.suggestions?.invoke()?.forEach { if (it.lowercase().startsWith(remaining)) builder.suggest(it) }
                    builder.buildFuture()
                }
            }
        builder.requires { isAvailable(node) }
        builder.executes { context -> execute(node, context) }
        for (child in node.childMap.values) builder.then(build(child))
        return builder
    }

    private fun execute(
        node: CommandNode,
        context: CommandContext<*>,
    ): Int {
        val executor = node.executor
        try {
            if (executor == null) onHelp(node) else executor(CommandCall(context))
        } catch (error: CommandSyntaxException) {
            throw error
        } catch (error: Throwable) {
            error.rethrowIfFatal()
            onError(node, error)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun validate(
        spec: CommandBuilder,
        parent: CommandNode,
    ) {
        require(NAME_PATTERN.matches(spec.name)) { "Invalid command name '${spec.name}' under ${parent.usage}" }
        // Dead nodes still exist in Brigadier, so their kind is fixed too.
        val any = parent.childMap[spec.name]
        if (any != null) {
            require(any.isArgument == (spec.argumentType != null)) {
                "${any.usage} is already declared as ${if (any.isArgument) "an argument" else "a literal"}"
            }
        }
        val existing = any?.takeIf { it.isAlive }
        if (existing != null) {
            require(spec.executor == null || existing.executor == null) { "${existing.usage} already has an action" }
        } else {
            requireNotNull(spec.descriptionKey) { "${parent.usage} ${spec.name} needs a description(...)" }
        }
        val node = existing ?: CommandNode(spec.name, parent, spec.argumentType)
        for (child in spec.children) validate(child, node)
    }

    /** Applies a validated spec, recording every node it references so the registration can release them. */
    private fun apply(
        spec: CommandBuilder,
        parent: CommandNode,
        touched: MutableList<Pair<CommandNode, Boolean>>,
    ) {
        val node = parent.childMap.getOrPut(spec.name) { CommandNode(spec.name, parent, spec.argumentType) }
        if (!node.isAlive) node.clear()
        node.references++
        if (node.descriptionKey == null) node.descriptionKey = spec.descriptionKey
        if (node.ownCategory == null) node.ownCategory = spec.category
        if (node.visibility == null) node.visibility = spec.visibility
        if (node.suggestions == null) node.suggestions = spec.suggestions
        val ownsExecutor = spec.executor != null
        if (ownsExecutor) node.executor = spec.executor
        touched.add(node to ownsExecutor)
        for (child in spec.children) apply(child, node, touched)
    }

    private fun release(touched: List<Pair<CommandNode, Boolean>>) {
        for (index in touched.indices.reversed()) {
            val (node, ownsExecutor) = touched[index]
            if (ownsExecutor) node.executor = null
            node.references--
            if (node.references == 0) node.clear()
        }
    }

    private companion object {
        val NAME_PATTERN = Regex("[a-z0-9_-]+")
    }
}
