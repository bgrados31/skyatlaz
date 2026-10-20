package org.tnozamas.skyatlaz.util

/**
 * How the mod writes big numbers: `638.77M` (short, for a glance) or `638,769,221` (exact, Hypixel style).
 *
 * The choice is stored by name, so renaming a constant would drop the saved value; keep the names stable.
 */
enum class NumberStyle {
    COMPACT,
    GROUPED,
}
