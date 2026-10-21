package org.tnozamas.skyatlaz.ui.theme

/**
 * The rhythm every skyatlaz surface shares: gaps, paddings and margins. One scale, so a HUD card, a chat line and a
 * settings row breathe the same way, and changing a value here moves the whole mod at once.
 *
 * Sizes that belong to a single widget (a row height, a button height) stay with that widget; only the rhythm of
 * the layout lives here.
 */
object Spacing {
    /** Inside one line of text (between a label and its own value). */
    const val TIGHT = 1

    /** Between two lines of the same block. */
    const val GAP = 2

    /** Between two blocks of the same surface. */
    const val GAP_WIDE = 4

    /** Inside a small surface: a card, a button. */
    const val PAD = 3

    /** Inside a panel. */
    const val PAD_WIDE = 8

    /** Between a panel and the edge of the screen. */
    const val MARGIN = 16
}
