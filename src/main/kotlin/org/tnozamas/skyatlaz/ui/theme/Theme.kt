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

    // Semantic (soft).
    const val SUCCESS = 0x6EE7A8
    const val WARNING = 0xFCD34D
    const val ERROR = 0xFB8A8A
    const val INFO = 0x8CC8FF

    /** The mod's signature gradient: name, titles, highlights. */
    @JvmField val BRAND_GRADIENT = Gradient.of(BRAND_AQUA, BRAND_LAVENDER, BRAND_ROSE)

    /** Cyclic version of the brand gradient for scrolling animations in screens and HUD. */
    @JvmField val BRAND_GRADIENT_CYCLIC = Gradient.cyclic(BRAND_AQUA, BRAND_LAVENDER, BRAND_ROSE)

    // Message bodies fade from their semantic color into regular text: the tone reads at a glance, the rest
    // stays calm and legible.
    @JvmField val SUCCESS_GRADIENT = Gradient.of(SUCCESS, TEXT)

    @JvmField val WARNING_GRADIENT = Gradient.of(WARNING, TEXT)

    @JvmField val ERROR_GRADIENT = Gradient.of(ERROR, TEXT)

    @JvmField val INFO_GRADIENT = Gradient.of(INFO, TEXT)
}
