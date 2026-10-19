package org.tnozamas.skyatlaz.util

/** A successful [ChatPattern] match: named groups as text or as Hypixel-style numbers. */
class ChatMatch internal constructor(
    private val result: MatchResult,
) {
    /** The named group's text, or null if the group did not participate. */
    operator fun get(group: String): String? = result.groups[group]?.value

    /** The named group parsed with [Numbers.parse] (`1,234`, `1.2M`...). */
    fun number(group: String): Double? = get(group)?.let(Numbers::parse)

    fun long(group: String): Long? = get(group)?.let(Numbers::parseLong)

    /** The named group as a roman or plain number (`XIV`, `14`). */
    fun roman(group: String): Int? = get(group)?.let(Numbers::parseRoman)
}
