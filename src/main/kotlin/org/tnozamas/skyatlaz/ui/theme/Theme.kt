package org.tnozamas.skyatlaz.ui.theme

/**
 * Every color in the mod lives here (enforced by the `no-hardcoded-color` rule). Change a token and the whole
 * UI follows. Colors are 0xRRGGBB.
 *
 * Semantic colors use the hues players already read without thinking: green = success, amber = warning,
 * red = error, blue = info. Each pairs a saturated base with a lighter companion for its gradient.
 */
object Theme {
    // Brand. Provisional palette, adjust freely: cyan -> violet -> pink.
    const val BRAND_CYAN = 0x38E1FF
    const val BRAND_VIOLET = 0x8B5CF6
    const val BRAND_PINK = 0xF472B6

    // Text
    const val TEXT = 0xE6E8EF
    const val TEXT_MUTED = 0x9AA0B4

    // Semantic
    const val SUCCESS = 0x22C55E
    const val SUCCESS_LIGHT = 0xA3E635
    const val WARNING = 0xF59E0B
    const val WARNING_LIGHT = 0xFDE047
    const val ERROR = 0xEF4444
    const val ERROR_LIGHT = 0xFB7185
    const val INFO = 0x3B82F6
    const val INFO_LIGHT = 0x22D3EE

    /** The mod's signature gradient: chat prefix, titles, highlights. */
    @JvmField val BRAND_GRADIENT = Gradient.of(BRAND_CYAN, BRAND_VIOLET, BRAND_PINK)

    /** Cyclic version of the brand gradient for scrolling animations in screens and HUD. */
    @JvmField val BRAND_GRADIENT_CYCLIC = Gradient.cyclic(BRAND_CYAN, BRAND_VIOLET, BRAND_PINK)

    @JvmField val SUCCESS_GRADIENT = Gradient.of(SUCCESS, SUCCESS_LIGHT)

    @JvmField val WARNING_GRADIENT = Gradient.of(WARNING, WARNING_LIGHT)

    @JvmField val ERROR_GRADIENT = Gradient.of(ERROR, ERROR_LIGHT)

    @JvmField val INFO_GRADIENT = Gradient.of(INFO, INFO_LIGHT)
}
