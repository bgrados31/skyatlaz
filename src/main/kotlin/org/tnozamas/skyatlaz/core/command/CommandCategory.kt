package org.tnozamas.skyatlaz.core.command

/**
 * Sections of the generated help. Set on a top-level command; children inherit it. Declaration order is the
 * order shown in `/skyatlaz`. Titles come from `command.category.<name>` in en_us.json; [icon] is a symbol (not
 * language) checked to render well with `/skyatlaz debug glyphs`.
 */
enum class CommandCategory(
    val icon: String,
) {
    GENERAL("✦"),
    FEATURES("★"),
    TOOLS("⚙"),

    /** Hidden unless debug mode is on (always on in the dev environment). */
    DEBUG("⚗"),
    ;

    val translationKey: String = "command.category." + name.lowercase()
}
