package org.tnozamas.skyatlaz.util

/**
 * A chat/scoreboard/lore pattern with a mandatory cheap prefilter. Hundreds of lines per minute reach every
 * listener, so [match] first checks that [requires] occurs in the text (a plain `contains`) and only then runs the
 * regex. Declare patterns once, at object level:
 * ```
 * val RARE_DROP = ChatPattern("RARE DROP!", """RARE DROP! (?<item>.+?)(?: \(\+(?<mf>\d+)% ✯ Magic Find\))?""")
 * RARE_DROP.match(event.text)?.let { drop -> drop["item"]; drop.number("mf") }
 * ```
 * Match against plain text (`ChatReceivedEvent.text`) unless the pattern needs colors (`.legacy`).
 * [regex] must match the whole text.
 */
class ChatPattern(
    val requires: String,
    regex: String,
) {
    private val compiled = Regex(regex)

    init {
        require(requires.isNotEmpty()) { "A pattern needs a non-empty literal prefilter" }
    }

    fun match(text: String): ChatMatch? {
        if (!text.contains(requires)) return null
        return compiled.matchEntire(text)?.let(::ChatMatch)
    }

    fun matches(text: String): Boolean = text.contains(requires) && compiled.matches(text)
}
