package org.tnozamas.skyatlaz.core.command

import com.mojang.brigadier.context.CommandContext

/** One execution of a command: typed access to its arguments by the names declared with `argument(...)`. */
class CommandCall internal constructor(
    private val context: CommandContext<*>,
) {
    fun string(name: String): String = context.getArgument(name, String::class.java)

    fun int(name: String): Int = context.getArgument(name, Int::class.javaObjectType)

    fun double(name: String): Double = context.getArgument(name, Double::class.javaObjectType)

    fun bool(name: String): Boolean = context.getArgument(name, Boolean::class.javaObjectType)

    fun <T : Any> get(
        name: String,
        type: Class<T>,
    ): T = context.getArgument(name, type)
}
