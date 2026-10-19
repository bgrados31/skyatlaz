package org.tnozamas.skyatlaz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NumbersTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "Purse: §61,234,567|1234567",
            "1,234.5|1234.5",
            "1.2M|1200000",
            "3.5k coins|3500",
            "§c-250 coins|-250",
            "Bits: 15.3K|15300",
            "2B|2000000000",
            "Kills: 7 more|7",
            "12.|12",
        ],
    )
    fun `parse reads hypixel numbers`(
        text: String,
        expected: Double,
    ) {
        assertEquals(expected, Numbers.parse(text)!!, 1e-6)
    }

    @Test
    fun `parse ignores words that only start with a suffix letter`() {
        assertEquals(5.0, Numbers.parse("5 kills"))
        assertEquals(5.0, Numbers.parse("5 billion"))
    }

    @Test
    fun `parse returns null without digits`() {
        assertNull(Numbers.parse("Purse: none"))
    }

    @Test
    fun `grouped uses commas`() {
        assertEquals("1,234,567", Numbers.grouped(1_234_567))
        assertEquals("999", Numbers.grouped(999))
        assertEquals("-1,000", Numbers.grouped(-1000))
        assertEquals("0", Numbers.grouped(0))
    }

    @Test
    fun `compact picks the right suffix and trims zeros`() {
        assertEquals("1.23M", Numbers.compact(1_234_567.0))
        assertEquals("15.3k", Numbers.compact(15_300.0))
        assertEquals("2B", Numbers.compact(2_000_000_000.0))
        assertEquals("999", Numbers.compact(999.4))
        assertEquals("-3.5k", Numbers.compact(-3500.0))
        assertEquals("1.2M", Numbers.compact(1_234_567.0, decimals = 1))
    }

    @Test
    fun `roman numerals both ways`() {
        assertEquals("XIV", Numbers.roman(14))
        assertEquals("MCMXCIX", Numbers.roman(1999))
        assertEquals(14, Numbers.parseRoman("XIV"))
        assertEquals(50, Numbers.parseRoman("§eL"))
        assertEquals(7, Numbers.parseRoman("7"))
        assertNull(Numbers.parseRoman("IIII"))
        assertNull(Numbers.parseRoman("Hello"))
    }
}
