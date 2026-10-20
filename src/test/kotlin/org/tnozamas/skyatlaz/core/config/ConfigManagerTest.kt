package org.tnozamas.skyatlaz.core.config

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.nio.file.Files
import java.nio.file.Path

class ConfigManagerTest {
    @TempDir
    lateinit var dir: Path

    private enum class Style { COMPACT, FULL }

    private class HudConfig : ConfigSection("dungeons.scorehud") {
        val enabled = boolean("enabled", false)
        val scale = decimal("scale", 1.0, 0.5..3.0)
        val lines = int("lines", 5, 1..10)
        val title = text("title", "Score", maxLength = 8)
        val style = choice("style", Style.COMPACT)
    }

    private val problems = ArrayList<String>()

    private val file get() = dir.resolve("skyatlaz").resolve("config.json")

    private fun manager(
        migrations: List<ConfigMigration> = emptyList(),
        scheduler: Scheduler? = null,
    ) = ConfigManager(file, migrations, scheduler) { message, _ -> problems.add(message) }

    private fun writeFile(json: String) {
        Files.createDirectories(file.parent)
        Files.writeString(file, json)
    }

    private fun storedValues() = JsonParser.parseString(Files.readString(file)).asJsonObject.getAsJsonObject("values")

    @Test
    fun `defaults without a file`() {
        val hud = HudConfig()
        manager().apply { register(hud) }.load()

        assertFalse(hud.enabled.value)
        assertEquals(1.0, hud.scale.value)
        assertEquals(Style.COMPACT, hud.style.value)
    }

    @Test
    fun `registered sections are listed in order`() {
        val hud = HudConfig()
        val other = object : ConfigSection("other") {}
        val config = manager()

        config.register(hud)
        config.register(other)

        assertEquals(listOf("dungeons.scorehud", "other"), config.registeredSections.map { it.id })
        assertEquals("config.section.dungeons.scorehud", hud.titleKey)
    }

    @Test
    fun `choice options can be driven by index`() {
        val hud = HudConfig()

        assertEquals(listOf("COMPACT", "FULL"), hud.style.optionNames())
        hud.style.selectIndex(1)
        assertEquals(Style.FULL, hud.style.value)
        hud.style.selectIndex(99)
        assertEquals(Style.FULL, hud.style.value)
    }

    @Test
    fun `values survive a save and load round trip`() {
        val first = HudConfig()
        manager().apply {
            register(first)
            load()
            first.enabled.set(true)
            first.scale.set(2.5)
            first.lines.set(7)
            first.title.set("Run")
            first.style.set(Style.FULL)
            saveNow()
        }

        val second = HudConfig()
        manager().apply { register(second) }.load()

        assertTrue(second.enabled.value)
        assertEquals(2.5, second.scale.value)
        assertEquals(7, second.lines.value)
        assertEquals("Run", second.title.value)
        assertEquals(Style.FULL, second.style.value)
    }

    @Test
    fun `sections registered after load get their stored values`() {
        writeFile("""{"version":1,"values":{"dungeons.scorehud.lines":3}}""")
        val config = manager().apply { load() }
        val hud = HudConfig()

        config.register(hud)

        assertEquals(3, hud.lines.value)
    }

    @Test
    fun `invalid values are clamped or replaced by defaults`() {
        writeFile(
            """{"version":1,"values":{"dungeons.scorehud.scale":99,"dungeons.scorehud.lines":"many",""" +
                """"dungeons.scorehud.title":"Very long title","dungeons.scorehud.style":"GONE"}}""",
        )
        val hud = HudConfig()
        manager().apply { register(hud) }.load()

        assertEquals(3.0, hud.scale.value)
        assertEquals(5, hud.lines.value)
        assertEquals("Very lon", hud.title.value)
        assertEquals(Style.COMPACT, hud.style.value)
    }

    @Test
    fun `setters validate too`() {
        val hud = HudConfig()

        hud.scale.set(0.1)
        hud.lines.set(42)
        hud.scale.set(Double.NaN)

        assertEquals(1.0, hud.scale.value)
        assertEquals(10, hud.lines.value)
    }

    @Test
    fun `unknown keys are kept when saving`() {
        writeFile("""{"version":1,"values":{"removed.feature.enabled":true}}""")
        val hud = HudConfig()
        manager().apply {
            register(hud)
            load()
            hud.enabled.set(true)
            saveNow()
        }

        val values = storedValues()
        assertTrue(values.get("removed.feature.enabled").asBoolean)
        assertTrue(values.get("dungeons.scorehud.enabled").asBoolean)
    }

    @Test
    fun `older files are migrated step by step`() {
        writeFile("""{"version":0,"values":{"dungeons.score.size":2.0}}""")
        val hud = HudConfig()
        manager(listOf(ConfigMigration.rename(0, "dungeons.score.size", "dungeons.scorehud.scale")))
            .apply { register(hud) }
            .load()

        assertEquals(2.0, hud.scale.value)
    }

    @Test
    fun `a corrupt file is moved aside and defaults are used`() {
        writeFile("{ this is not json")
        val hud = HudConfig()
        manager().apply { register(hud) }.load()

        assertEquals(1.0, hud.scale.value)
        assertEquals(1, problems.size)
        assertFalse(Files.exists(file))
        assertTrue(Files.list(file.parent).use { files -> files.anyMatch { it.fileName.toString().contains("broken") } })
    }

    @Test
    fun `listeners run only on real changes`() {
        val hud = HudConfig()
        val seen = ArrayList<Double>()
        val registration = hud.scale.onChange { seen.add(it) }

        hud.scale.set(2.0)
        hud.scale.set(2.0)
        registration.unregister()
        hud.scale.set(1.5)

        assertEquals(listOf(2.0), seen)
    }

    @Test
    fun `changes are saved once after a burst of edits`() {
        val pending = ArrayList<Runnable>()
        val scheduler = Scheduler({ pending.add(it) }) { throw AssertionError(it) }
        val hud = HudConfig()
        manager(scheduler = scheduler).apply {
            register(hud)
            load()
        }

        for (step in 1..5) hud.scale.set(1.0 + step * 0.1)
        repeat(ConfigManager.SAVE_DELAY_TICKS + 1) { scheduler.tick() }
        assertEquals(1, pending.size)
        pending.forEach(Runnable::run)

        assertEquals(1.5, storedValues().get("dungeons.scorehud.scale").asDouble, 1e-9)
    }

    @Test
    fun `duplicate keys are rejected`() {
        val config = manager()
        config.register(HudConfig())

        assertThrows(IllegalArgumentException::class.java) { config.register(HudConfig()) }
    }
}
