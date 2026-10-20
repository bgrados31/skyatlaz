package org.tnozamas.skyatlaz.ui.hud

import kotlin.math.roundToInt

/**
 * Where one HUD element sits and how large it is. Position is stored as a fraction of the free space, not as
 * absolute pixels: `x = 0` is the left edge, `x = 1` the right edge, and everything in between slides smoothly when
 * the window or GUI scale changes. The same applies to `y`. This replaces anchor enums and keeps saved layouts valid
 * across resolutions.
 *
 * [scale] multiplies the element's own (unscaled) size. Values are clamped, so a corrupt file can never produce a
 * widget outside the screen. [onChange] runs after a real change, only on the client thread.
 */
class HudPlacement(
    val id: String,
    val defaultX: Double,
    val defaultY: Double,
    val defaultScale: Double,
    x: Double = defaultX,
    y: Double = defaultY,
    scale: Double = defaultScale,
    private val onChange: () -> Unit = {},
) {
    var x: Double = sanitizeFraction(x)
        private set

    var y: Double = sanitizeFraction(y)
        private set

    var scale: Double = sanitizeScale(scale)
        private set

    /** Moves to a fraction of the available space. Values outside `0..1` are clamped. */
    fun moveTo(
        x: Double,
        y: Double,
    ) {
        val newX = sanitizeFraction(x)
        val newY = sanitizeFraction(y)
        if (newX == this.x && newY == this.y) return
        this.x = newX
        this.y = newY
        onChange()
    }

    /** Sets the scale, clamped to [MIN_SCALE]..[MAX_SCALE]. */
    fun setScale(scale: Double) {
        val newScale = sanitizeScale(scale)
        if (newScale == this.scale) return
        this.scale = newScale
        onChange()
    }

    /** Restores the element's declared position and scale. */
    fun reset() {
        if (x == defaultX && y == defaultY && scale == defaultScale) return
        x = sanitizeFraction(defaultX)
        y = sanitizeFraction(defaultY)
        scale = sanitizeScale(defaultScale)
        onChange()
    }

    /** Pixel width after scaling, rounded the same way as [resolveX] and [resolveY]. */
    fun scaledWidth(elementWidth: Int): Int = (elementWidth * scale).roundToInt()

    /** Pixel height after scaling. */
    fun scaledHeight(elementHeight: Int): Int = (elementHeight * scale).roundToInt()

    /** Left edge in screen pixels for an element [elementWidth] pixels wide. */
    fun resolveX(
        screenWidth: Int,
        elementWidth: Int,
    ): Int = resolve(x, screenWidth, scaledWidth(elementWidth))

    /** Top edge in screen pixels for an element [elementHeight] pixels tall. */
    fun resolveY(
        screenHeight: Int,
        elementHeight: Int,
    ): Int = resolve(y, screenHeight, scaledHeight(elementHeight))

    override fun toString(): String = "$id(x=$x, y=$y, scale=$scale)"

    companion object {
        const val MIN_SCALE = 0.5
        const val MAX_SCALE = 3.0

        fun sanitizeFraction(value: Double): Double = if (value.isNaN()) 0.0 else value.coerceIn(0.0, 1.0)

        fun sanitizeScale(value: Double): Double = if (value.isNaN()) 1.0 else value.coerceIn(MIN_SCALE, MAX_SCALE)

        fun resolve(
            fraction: Double,
            screenSize: Int,
            scaledSize: Int,
        ): Int {
            val available = screenSize - scaledSize
            if (available <= 0) return 0
            return (fraction * available).roundToInt().coerceIn(0, available)
        }
    }
}
