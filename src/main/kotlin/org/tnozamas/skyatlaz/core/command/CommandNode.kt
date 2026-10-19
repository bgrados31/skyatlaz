package org.tnozamas.skyatlaz.core.command

import com.mojang.brigadier.arguments.ArgumentType

/**
 * A node of the live command tree. Nodes are never removed, only switched off when no registration references
 * them, so the Brigadier tree built from them stays valid and a module can re-register freely.
 */
class CommandNode internal constructor(
    val name: String,
    val parent: CommandNode?,
    internal val argumentType: ArgumentType<*>?,
) {
    var descriptionKey: String? = null
        internal set
    internal var ownCategory: CommandCategory? = null
    internal var visibility: (() -> Boolean)? = null
    internal var executor: ((CommandCall) -> Unit)? = null
    internal var suggestions: (() -> Iterable<String>)? = null
    internal var references = 0
    internal val childMap = LinkedHashMap<String, CommandNode>()

    val isArgument: Boolean get() = argumentType != null

    val isRoot: Boolean get() = parent == null

    /** Referenced by at least one live registration (the root always is). */
    val isAlive: Boolean get() = isRoot || references > 0

    val isExecutable: Boolean get() = executor != null

    val category: CommandCategory get() = ownCategory ?: parent?.category ?: CommandCategory.GENERAL

    /** Full syntax, e.g. `/skyatlaz pet <name>`. */
    val usage: String
        get() {
            val token = if (isArgument) "<$name>" else name
            return if (parent == null) "/$token" else parent.usage + " " + token
        }

    /** What clicking the command in help types into chat: the path up to its first argument. */
    val suggestion: String
        get() = if (isArgument) parent!!.suggestion else (parent?.suggestion ?: "/") + (if (isRoot) "" else " ") + name

    internal fun clear() {
        descriptionKey = null
        ownCategory = null
        visibility = null
        executor = null
        suggestions = null
    }

    override fun toString(): String = usage
}
