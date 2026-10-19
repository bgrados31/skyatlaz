package org.tnozamas.skyatlaz.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Numbers the way Hypixel writes them, independent of the player's locale: `1,234,567`, `1.2M`, `3.5k`, `XIV`.
 * Parsing is lenient (formatting codes, spaces, signs); formatting is for display text built on change, not per
 * frame.
 */
object Numbers {
    private val SUFFIXES = charArrayOf('k', 'm', 'b', 't')
    private val SCALES = doubleArrayOf(1e3, 1e6, 1e9, 1e12)
    private const val COMPACT_SUFFIXES = "kMBT"

    /**
     * `"1,234.5"` -> 1234.5, `"1.2M"` -> 1_200_000.0, `"§6-3.5k coins"` -> -3500.0. Null when no number is there.
     * Reads the first number in [text], with an optional k/M/B/T suffix right after it.
     */
    fun parse(text: String): Double? {
        val clean = LegacyText.strip(text)
        var start = -1
        for (index in clean.indices) {
            if (clean[index].isDigit()) {
                start = index
                break
            }
        }
        if (start < 0) return null
        val negative = start > 0 && clean[start - 1] == '-'
        val digits = StringBuilder()
        var index = start
        while (index < clean.length) {
            val char = clean[index]
            when {
                char.isDigit() || char == '.' -> digits.append(char)
                char == ',' -> Unit
                else -> break
            }
            index++
        }
        var value = digits.toString().trimEnd('.').toDoubleOrNull() ?: return null
        val suffix = clean.getOrNull(index)?.lowercaseChar()
        val scale = SUFFIXES.indexOf(suffix ?: ' ')
        if (scale >= 0 && clean.getOrNull(index + 1)?.isLetter() != true) value *= SCALES[scale]
        return if (negative) -value else value
    }

    /** [parse] rounded to a whole number. */
    fun parseLong(text: String): Long? = parse(text)?.roundToLong()

    /** 1234567 -> `1,234,567` (Hypixel style, always `,` grouping). */
    fun grouped(value: Long): String {
        val digits = abs(value).toString()
        val result = StringBuilder(digits.length + digits.length / 3 + 1)
        if (value < 0) result.append('-')
        for ((index, char) in digits.withIndex()) {
            if (index > 0 && (digits.length - index) % 3 == 0) result.append(',')
            result.append(char)
        }
        return result.toString()
    }

    /** 1234567.0 -> `1.23M`, 15300.0 -> `15.3k`, 999.0 -> `999`. At most [decimals] decimals, no trailing zeros. */
    fun compact(
        value: Double,
        decimals: Int = 2,
    ): String {
        val magnitude = abs(value)
        var tier = -1
        while (tier + 1 < SCALES.size && magnitude >= SCALES[tier + 1]) tier++
        val scaled = if (tier < 0) value else value / SCALES[tier]
        val text = trimDecimals(scaled, if (tier < 0) 0 else decimals)
        return if (tier < 0) text else text + COMPACT_SUFFIXES[tier]
    }

    /** 14 -> `XIV`. SkyBlock uses roman numerals for levels and tiers (1..3999). */
    fun roman(value: Int): String {
        require(value in 1..3999) { "Roman numerals cover 1..3999, got $value" }
        val result = StringBuilder()
        var remaining = value
        for (index in ROMAN_VALUES.indices) {
            while (remaining >= ROMAN_VALUES[index]) {
                result.append(ROMAN_SYMBOLS[index])
                remaining -= ROMAN_VALUES[index]
            }
        }
        return result.toString()
    }

    /** `XIV` -> 14; plain digits are accepted too (`14` -> 14), as some players disable roman numerals. */
    fun parseRoman(text: String): Int? {
        val clean = LegacyText.strip(text).trim().uppercase()
        if (clean.isEmpty()) return null
        clean.toIntOrNull()?.let { return it }
        var total = 0
        var previous = 0
        for (index in clean.indices.reversed()) {
            val value = ROMAN_DIGITS[clean[index]] ?: return null
            total += if (value < previous) -value else value
            previous = maxOf(previous, value)
        }
        return total.takeIf { it > 0 && roman(it) == clean }
    }

    private fun trimDecimals(
        value: Double,
        decimals: Int,
    ): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10 }
        val rounded = (value * factor).roundToLong() / factor
        val text = if (decimals == 0) rounded.roundToLong().toString() else rounded.toString()
        return if ('.' in text) text.trimEnd('0').trimEnd('.') else text
    }

    private val ROMAN_VALUES = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    private val ROMAN_SYMBOLS = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    private val ROMAN_DIGITS = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
}
