package org.tnozamas.skyatlaz.core.keybind

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tnozamas.skyatlaz.core.config.ConfigManager
import java.nio.file.Files
import java.nio.file.Path

/** The registry of key bindings: what is active, what is saved and what clashes. */
class KeyBindManagerTest {
    @TempDir
    lateinit var dir: Path

    private val conflicts = ArrayList<Pair<String, String>>()

    private val file get() = dir.resolve("skyatlaz").resolve("config.json")

    private fun manager(): Pair<KeyBindManager, ConfigManager> {
        val config = ConfigManager(file, emptyList(), null) { _, _ -> }
        config.load()
        val manager =
            KeyBindManager(config) { first, second ->
                conflicts += first.id to second.id
            }
        return manager to config
    }

    private fun storedValues(): Map<String, Int> {
        val json = JsonParser.parseString(Files.readString(file)).asJsonObject.getAsJsonObject("values")
        return json.keySet().associateWith { json.get(it).asInt }
    }

    @Test
    fun `a binding is active while it is registered`() {
        val (manager, _) = manager()
        manager.register(KeyBind("test.one", KeyCombo(290))) {}

        assertEquals(listOf("test.one"), manager.active().map { it.id })
        assertEquals(KeyCombo(290), manager.comboOf("test.one"))
    }

    @Test
    fun `removing the registration removes the binding`() {
        val (manager, _) = manager()
        val registration = manager.register(KeyBind("test.one", KeyCombo(290))) {}

        registration.unregister()

        assertTrue(manager.active().isEmpty())
        assertEquals(KeyCombo.NONE, manager.comboOf("test.one"))
    }

    @Test
    fun `the chosen key is saved in the config`() {
        val (manager, config) = manager()
        manager.register(KeyBind("test.one", KeyCombo(290))) {}

        manager.setCombo("test.one", KeyCombo(291, KeyCombo.SHIFT))
        config.saveNow()

        val stored = storedValues()["keybind.test.one.key"]!!
        assertEquals(291, stored and 0xFFFF)
        assertEquals(KeyCombo.SHIFT, stored shr 16)
    }

    @Test
    fun `a stored key is loaded when the binding starts`() {
        val (manager, config) = manager()
        manager.register(KeyBind("test.one", KeyCombo(290))) {}
        manager.setCombo("test.one", KeyCombo(291))
        config.saveNow()

        val (second, _) = manager()
        second.register(KeyBind("test.one", KeyCombo(290))) {}

        assertEquals(KeyCombo(291), second.comboOf("test.one"))
    }

    @Test
    fun `two bindings on the same key are reported once`() {
        val (manager, _) = manager()
        manager.register(KeyBind("test.one", KeyCombo(290))) {}
        manager.register(KeyBind("test.two", KeyCombo(290))) {}

        assertEquals(1, conflicts.size)
        assertEquals("test.one", conflicts.first().first)
    }

    @Test
    fun `bindings on different keys do not clash`() {
        val (manager, _) = manager()
        manager.register(KeyBind("test.one", KeyCombo(290))) {}
        manager.register(KeyBind("test.two", KeyCombo(291))) {}

        assertTrue(conflicts.isEmpty())
    }
}
