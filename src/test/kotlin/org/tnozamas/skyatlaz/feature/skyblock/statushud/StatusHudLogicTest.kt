package org.tnozamas.skyatlaz.feature.skyblock.statushud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.skyblock.SkyBlockState
import org.tnozamas.skyatlaz.util.NumberStyle

/** The rows of the status card, without the game. */
class StatusHudLogicTest {
    private val state =
        SkyBlockState(
            onSkyBlock = true,
            area = "Hub",
            subArea = "Village",
            profile = "Mango",
            level = 85,
            purse = 638_769_221L,
            bits = 32_222L,
            bank = 1_100_000L,
        )

    private fun build(options: StatusHudOptions = StatusHudOptions()) = StatusHudLogic.build(state, options)

    @Test
    fun `outside skyblock there is nothing to show`() {
        assertTrue(StatusHudLogic.build(SkyBlockState.EMPTY, StatusHudOptions()).isEmpty())
    }

    @Test
    fun `default rows are area, profile, level and purse`() {
        assertEquals(
            listOf(
                StatusHudLine("skyblock.statushud.location", listOf("Hub", "Village")),
                StatusHudLine("skyblock.statushud.profile", listOf("Mango")),
                StatusHudLine("skyblock.statushud.level", listOf("85")),
                StatusHudLine("skyblock.statushud.purse", listOf("638.77M")),
            ),
            build(),
        )
    }

    @Test
    fun `bits and bank are hidden unless they are asked for`() {
        val rows = build(StatusHudOptions(showBits = true, showBank = true))

        assertEquals(
            listOf(
                StatusHudLine("skyblock.statushud.location", listOf("Hub", "Village")),
                StatusHudLine("skyblock.statushud.profile", listOf("Mango")),
                StatusHudLine("skyblock.statushud.level", listOf("85")),
                StatusHudLine("skyblock.statushud.purse", listOf("638.77M")),
                StatusHudLine("skyblock.statushud.bits", listOf("32.22k")),
                StatusHudLine("skyblock.statushud.bank", listOf("1.1M")),
            ),
            rows,
        )
    }

    @Test
    fun `a disabled row is skipped`() {
        val rows = build(StatusHudOptions(showProfile = false, showPurse = false))

        assertEquals(
            listOf(
                StatusHudLine("skyblock.statushud.location", listOf("Hub", "Village")),
                StatusHudLine("skyblock.statushud.level", listOf("85")),
            ),
            rows,
        )
    }

    @Test
    fun `grouped numbers are written the way hypixel writes them`() {
        val rows = build(StatusHudOptions(numberStyle = NumberStyle.GROUPED))

        assertEquals(StatusHudLine("skyblock.statushud.purse", listOf("638,769,221")), rows.last())
    }

    @Test
    fun `a row without a value is skipped instead of showing a placeholder`() {
        val withoutBits = state.copy(bits = null, purse = null)
        val rows = StatusHudLogic.build(withoutBits, StatusHudOptions(showBits = true))

        assertEquals(
            listOf(
                StatusHudLine("skyblock.statushud.location", listOf("Hub", "Village")),
                StatusHudLine("skyblock.statushud.profile", listOf("Mango")),
                StatusHudLine("skyblock.statushud.level", listOf("85")),
            ),
            rows,
        )
    }

    @Test
    fun `the area row falls back to the area alone`() {
        val withoutSubArea = state.copy(subArea = null)
        val rows = StatusHudLogic.build(withoutSubArea, StatusHudOptions())

        assertEquals(StatusHudLine("skyblock.statushud.area", listOf("Hub")), rows.first())
    }
}
