package org.tnozamas.skyatlaz.core.keybind

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.core.config.ConfigEntry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.core.event.Registration

/**
 * Every key binding in the mod. Features register a binding through their module context, so a binding exists only
 * while its feature runs and disappears with it.
 *
 * The keys are polled once per client tick (never per frame) and fire on the press, not while held. A binding does
 * not fire while a screen is open, so typing in the chat or in a settings field is never stolen.
 *
 * The chosen key is stored in the config file (`keybind.<id>.key`), so it survives restarts. Client thread only.
 */
class KeyBindManager(
    private val config: ConfigManager,
    private val onConflict: (first: KeyBind, second: KeyBind) -> Unit,
) {
    private val stores = HashMap<String, Store>()
    private val binds = LinkedHashMap<String, Binding>()
    private val reported = HashSet<String>()

    /** One binding and the listener that runs when its key is pressed. */
    private class Binding(
        val bind: KeyBind,
        val entry: ConfigEntry.Integer,
        val onPress: () -> Unit,
    ) {
        var down = false

        val combo: KeyCombo get() = KeyCombo.decode(entry.value)
    }

    /** The config section of a binding, created once so the saved key survives restarts. */
    private class BindSection(
        bind: KeyBind,
    ) : ConfigSection("keybind.${bind.id}") {
        val key: ConfigEntry.Integer = int("key", bind.defaultCombo.encode(), 0..Int.MAX_VALUE)
    }

    private class Store(
        val entry: ConfigEntry.Integer,
    )

    /**
     * Registers [bind] and returns the registration that removes it. The listener runs on the client thread while
     * the feature that registered it runs.
     */
    fun register(
        bind: KeyBind,
        onPress: () -> Unit,
    ): Registration {
        require(bind.id !in binds) { "Duplicate key bind '${bind.id}'" }
        val store =
            stores.getOrPut(bind.id) {
                val section = BindSection(bind)
                config.register(section)
                Store(section.key)
            }
        val binding = Binding(bind, store.entry, onPress)
        binds[bind.id] = binding
        reportConflicts()
        return Registration {
            binds.remove(bind.id)
            binding.down = false
        }
    }

    /** The bindings that are active right now, in registration order. */
    fun active(): List<KeyBind> = binds.values.map { it.bind }

    /** The key a binding uses right now. */
    fun comboOf(id: String): KeyCombo = binds[id]?.combo ?: KeyCombo.NONE

    /** Changes the key of an active binding and saves it. */
    fun setCombo(
        id: String,
        combo: KeyCombo,
    ) {
        val binding = binds[id] ?: return
        binding.entry.set(combo.encode())
        reportConflicts()
    }

    /**
     * Polls the keys once per tick and fires the bindings that were just pressed. Does nothing when no feature has a
     * binding, so a disabled feature costs nothing.
     */
    fun tick(window: Window) {
        if (binds.isEmpty()) return
        // A screen owns the keyboard while it is open.
        if (Minecraft.getInstance().gui.screen() != null) return
        val pressed = KeyModifiers.pressed(window)
        for (binding in binds.values) {
            val combo = binding.combo
            val down =
                combo.isBound &&
                    InputConstants.isKeyDown(window, combo.key) &&
                    combo.modifiersPressed(pressed)
            if (down && !binding.down) binding.onPress()
            binding.down = down
        }
    }

    /** Tells the owner when two bindings want the same key, so the player can fix it. */
    private fun reportConflicts() {
        val list = binds.values.toList()
        for (first in 0 until list.size) {
            for (second in first + 1 until list.size) {
                val one = list[first]
                val other = list[second]
                if (!one.combo.isBound || one.combo != other.combo) continue
                val pair = "${one.bind.id}|${other.bind.id}"
                if (reported.add(pair)) onConflict(one.bind, other.bind)
            }
        }
    }
}
