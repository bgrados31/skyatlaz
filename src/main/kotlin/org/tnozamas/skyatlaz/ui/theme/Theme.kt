package org.tnozamas.skyatlaz.ui.theme

/**
 * Every color in the mod lives here (enforced by the `no-hardcoded-color` rule). Change a token and the whole
 * UI follows. Colors are 0xRRGGBB.
 *
 * The palette is deliberately soft (pastel, mid lightness) so SkyAtlaz output is recognizable without shouting
 * over Hypixel's own colorful chat. Semantic colors keep the hues players read without thinking: green = success,
 * amber = warning, red = error, blue = info.
 */
object Theme {
    // Brand: aqua -> lavender -> rose.
    const val BRAND_AQUA = 0x7DD3FC
    const val BRAND_LAVENDER = 0xB4A5FD
    const val BRAND_ROSE = 0xF5B4D6

    // Text, from strongest to quietest.
    const val TEXT = 0xE8EAF2
    const val TEXT_MUTED = 0x9AA1B9
    const val TEXT_SUBTLE = 0x5E6478

    /** Near-black backdrop for in-game screens, used with a translucent alpha. */
    const val SCREEN_DIM = 0x0B0D14

    // Surfaces for screens built on `ui.component`: opaque enough to read over the world, still soft.
    const val SURFACE = 0x151A26
    const val SURFACE_RAISED = 0x1D2333
    const val SURFACE_SUNKEN = 0x10141D
    const val BORDER = 0x2C3347
    const val TRACK = 0x323A50

    // Semantic: a deep tone of the hue for gradient starts, a soft tone for gradient ends.
    const val SUCCESS = 0x6EE7A8
    const val SUCCESS_DEEP = 0x22B566
    const val WARNING = 0xFCD34D
    const val WARNING_DEEP = 0xF08C00
    const val ERROR = 0xFB8A8A
    const val ERROR_DEEP = 0xE5383B
    const val INFO = 0x8CC8FF
    const val INFO_DEEP = 0x3D7EF0

    /** The mod's signature gradient: name, titles, highlights. */
    @JvmField val BRAND_GRADIENT = Gradient.of(BRAND_AQUA, BRAND_LAVENDER, BRAND_ROSE)

    /** Cyclic version of the brand gradient for scrolling animations in screens and HUD. */
    @JvmField val BRAND_GRADIENT_CYCLIC = Gradient.cyclic(BRAND_AQUA, BRAND_LAVENDER, BRAND_ROSE)

    // Message bodies go from the deep tone to the soft one within their own hue: the tone stays obvious from start
    // to end and stands out from the white of regular chat, without a rainbow.
    @JvmField val SUCCESS_GRADIENT = Gradient.of(SUCCESS_DEEP, SUCCESS)

    @JvmField val WARNING_GRADIENT = Gradient.of(WARNING_DEEP, WARNING)

    @JvmField val ERROR_GRADIENT = Gradient.of(ERROR_DEEP, ERROR)

    @JvmField val INFO_GRADIENT = Gradient.of(INFO_DEEP, INFO)

    /**
     * Converts a `0xRRGGBB` theme token to the `0xAARRGGBB` value required by GUI drawing calls.
     * Keeps alpha math out of feature code and the hardcoded-color rule intact.
     */
    fun argb(
        rgb: Int,
        alpha: Int = 255,
    ): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 16777215)
}
