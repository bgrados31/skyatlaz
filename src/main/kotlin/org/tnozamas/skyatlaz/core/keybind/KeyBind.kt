package org.tnozamas.skyatlaz.core.keybind

/**
 * A key binding a feature can listen to.
 *
 * [id] is the stable name of the binding: it names the config key (`keybind.<id>.key`) and the texts, so it must
 * not change once released. [defaultCombo] is the key the binding starts with; the player can change it in the
 * settings screen and the choice is saved.
 */
data class KeyBind(
    val id: String,
    val defaultCombo: KeyCombo,
) {
    /** The name shown in the settings screen. */
    val nameKey: String get() = "config.keybind.$id.name"

    /** The line under the name. */
    val descriptionKey: String get() = "config.keybind.$id.description"
}
