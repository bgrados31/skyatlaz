package org.tnozamas.skyatlaz.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class GradientTest {
    private val two = Gradient.of(Theme.ERROR, Theme.INFO)

    private data class Run(
        val text: String,
        val rgb: Int,
    )

    private fun runs(
        gradient: Gradient,
        text: String,
    ): List<Run> {
        val result = ArrayList<Run>()
        gradient.forEachRun(text) { start, end, rgb -> result.add(Run(text.substring(start, end), rgb)) }
        return result
    }

    private fun channelDistance(
        a: Int,
        b: Int,
    ): Int =
        maxOf(
            abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF)),
            abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF)),
            abs((a and 0xFF) - (b and 0xFF)),
        )

    @Test
    fun `ends match the stops`() {
        assertTrue(channelDistance(Theme.ERROR, two.colorAt(0f)) <= 1)
        assertTrue(channelDistance(Theme.INFO, two.colorAt(1f)) <= 1)
    }

    @Test
    fun `middle stops are hit exactly in multi stop gradients`() {
        val brand = Theme.BRAND_GRADIENT
        assertTrue(channelDistance(Theme.BRAND_VIOLET, brand.colorAt(0.5f)) <= 2)
    }

    @Test
    fun `out of range clamps unless cyclic`() {
        assertEquals(two.colorAt(0f), two.colorAt(-3f))
        assertEquals(two.colorAt(1f), two.colorAt(7f))

        val cyclic = Gradient.cyclic(Theme.ERROR, Theme.INFO)
        assertEquals(cyclic.colorAt(0.25f), cyclic.colorAt(1.25f))
        assertTrue(channelDistance(cyclic.colorAt(0f), cyclic.colorAt(0.999f)) <= 2)
    }

    @Test
    fun `single color is a solid fill`() {
        val solid = Gradient.of(Theme.SUCCESS)
        assertEquals(solid.colorAt(0f), solid.colorAt(1f))
        assertEquals(listOf(Run("ok go", solid.colorAt(0f))), runs(solid, "ok go"))
    }

    @Test
    fun `oklab midpoint is brighter than the muddy rgb average`() {
        // Red to green averaged in sRGB gives a dark olive; OKLab keeps perceived lightness.
        fun brightness(rgb: Int) = (rgb shr 16 and 0xFF) + (rgb shr 8 and 0xFF) + (rgb and 0xFF)
        val naive =
            ((Theme.ERROR shr 16 and 0xFF) + (Theme.SUCCESS shr 16 and 0xFF)) / 2 +
                ((Theme.ERROR shr 8 and 0xFF) + (Theme.SUCCESS shr 8 and 0xFF)) / 2 +
                ((Theme.ERROR and 0xFF) + (Theme.SUCCESS and 0xFF)) / 2
        val mid = Gradient.of(Theme.ERROR, Theme.SUCCESS).colorAt(0.5f)

        assertTrue(brightness(mid) > naive + 30) { "midpoint ${Integer.toHexString(mid)} is not brighter" }
    }

    @Test
    fun `runs cover the text, start and end on the stops, and whitespace does not advance`() {
        val text = "Sky Atlaz"
        val result = runs(two, text)

        assertEquals(text, result.joinToString("") { it.text })
        assertEquals(two.colorAt(0f), result.first().rgb)
        assertEquals(two.colorAt(1f), result.last().rgb)
        assertTrue(result.none { it.text.isBlank() }) { "whitespace must join a run: $result" }
    }

    @Test
    fun `neighbors with the same color merge into one run`() {
        val solid = Gradient.of(Theme.TEXT, Theme.TEXT)
        assertEquals(1, runs(solid, "many letters here").size)
    }

    @Test
    fun `surrogate pairs are not split`() {
        val text = "a💎b"
        val result = runs(two, text)

        assertEquals(text, result.joinToString("") { it.text })
        assertEquals(3, two.visibleCount(text))
        assertNotEquals(result.first().rgb, result.last().rgb)
    }

    @Test
    fun `empty and blank text`() {
        assertTrue(runs(two, "").isEmpty())
        assertEquals(listOf(Run("   ", two.colorAt(0f))), runs(two, "   "))
    }

    @Test
    fun `needs at least one color`() {
        assertThrows(IllegalArgumentException::class.java) { Gradient.of() }
    }
}
