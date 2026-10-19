package org.tnozamas.skyatlaz.ui.theme

import kotlin.math.cbrt
import kotlin.math.floor
import kotlin.math.pow

/**
 * Multi-stop color gradient. Everything expensive happens once in the constructor: stops are mixed in OKLab
 * (perceptual, so blue to yellow does not pass through muddy gray) into a [RESOLUTION]-entry lookup table.
 * After that [colorAt] is an array read and [forEachRun] allocates nothing, so both are safe in render paths.
 *
 * Colors are 0xRRGGBB and must come from [Theme] tokens. A [cyclic] gradient wraps back to its first stop,
 * which makes scrolling animations seamless (feed a moving offset into [colorAt]).
 */
class Gradient private constructor(
    stops: IntArray,
    val cyclic: Boolean,
) {
    private val table = IntArray(RESOLUTION)

    init {
        require(stops.isNotEmpty()) { "A gradient needs at least one color" }
        val points = if (cyclic && stops.size > 1) stops + stops[0] else stops
        val labs = Array(points.size) { toOkLab(points[it]) }
        val segments = (points.size - 1).coerceAtLeast(1)
        for (index in 0 until RESOLUTION) {
            val position = index.toFloat() / (RESOLUTION - 1) * segments
            val segment = position.toInt().coerceAtMost(segments - 1)
            val from = labs[segment.coerceAtMost(labs.size - 1)]
            val to = labs[(segment + 1).coerceAtMost(labs.size - 1)]
            table[index] = fromOkLab(from, to, position - segment)
        }
    }

    /**
     * Color at [t]: 0 = first stop, 1 = last stop. Values outside 0..1 clamp, or wrap when [cyclic].
     */
    fun colorAt(t: Float): Int {
        val position = if (cyclic) t - floor(t) else t.coerceIn(0f, 1f)
        return table[(position * (RESOLUTION - 1) + 0.5f).toInt()]
    }

    /**
     * Splits [text] into runs of one color, spreading the gradient over its visible characters. Whitespace takes
     * the color of the run it sits in, and neighbors with the same color merge, so the caller builds as few
     * styled pieces as possible. [offset] shifts the gradient (animation); [action] gets `[start, end)` and the
     * color.
     */
    inline fun forEachRun(
        text: CharSequence,
        offset: Float = 0f,
        action: (start: Int, end: Int, rgb: Int) -> Unit,
    ) {
        val visible = visibleCount(text)
        if (visible == 0) {
            if (text.isNotEmpty()) action(0, text.length, colorAt(offset))
            return
        }
        val step = if (visible > 1) 1f / (visible - 1) else 0f
        var runStart = 0
        var runColor = colorAt(offset)
        var seen = 0
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (!Character.isWhitespace(codePoint)) {
                val color = colorAt(offset + seen * step)
                if (color != runColor && index > runStart) {
                    action(runStart, index, runColor)
                    runStart = index
                }
                runColor = color
                seen++
            }
            index += Character.charCount(codePoint)
        }
        action(runStart, text.length, runColor)
    }

    /** Non-whitespace code points in [text]. Public only because [forEachRun] is inline. */
    fun visibleCount(text: CharSequence): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (!Character.isWhitespace(codePoint)) count++
            index += Character.charCount(codePoint)
        }
        return count
    }

    companion object {
        const val RESOLUTION = 256

        /** Gradient through [colors] (0xRRGGBB, from [Theme]), evenly spaced. One color gives a solid fill. */
        fun of(vararg colors: Int): Gradient = Gradient(colors.copyOf(), cyclic = false)

        /** Like [of] but wraps back to the first color, for seamless scrolling animations. */
        fun cyclic(vararg colors: Int): Gradient = Gradient(colors.copyOf(), cyclic = true)

        private fun toLinear(channel: Int): Float {
            val c = channel / 255f
            return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
        }

        private fun toSrgb(linear: Float): Int {
            val c = if (linear <= 0.0031308f) linear * 12.92f else 1.055f * linear.pow(1f / 2.4f) - 0.055f
            return (c.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        }

        private fun toOkLab(rgb: Int): FloatArray {
            val r = toLinear(rgb shr 16 and 0xFF)
            val g = toLinear(rgb shr 8 and 0xFF)
            val b = toLinear(rgb and 0xFF)
            val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
            val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
            val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
            return floatArrayOf(
                0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
                1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
                0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
            )
        }

        private fun fromOkLab(
            from: FloatArray,
            to: FloatArray,
            t: Float,
        ): Int {
            val lightness = from[0] + (to[0] - from[0]) * t
            val a = from[1] + (to[1] - from[1]) * t
            val b = from[2] + (to[2] - from[2]) * t
            val l = (lightness + 0.3963377774f * a + 0.2158037573f * b).let { it * it * it }
            val m = (lightness - 0.1055613458f * a - 0.0638541728f * b).let { it * it * it }
            val s = (lightness - 0.0894841775f * a - 1.2914855480f * b).let { it * it * it }
            val red = toSrgb(4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s)
            val green = toSrgb(-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s)
            val blue = toSrgb(-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s)
            return (red shl 16) or (green shl 8) or blue
        }
    }
}
