package org.tnozamas.skyatlaz.core.scheduler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CooldownTest {
    @Test
    fun `allows once per interval`() {
        val cooldown = Cooldown(20)

        assertTrue(cooldown.tryUse(0))
        assertFalse(cooldown.tryUse(19))
        assertTrue(cooldown.tryUse(20))
    }

    @Test
    fun `reset makes it ready again`() {
        val cooldown = Cooldown(20)
        cooldown.tryUse(5)

        cooldown.reset()

        assertTrue(cooldown.isReady(6))
    }
}
