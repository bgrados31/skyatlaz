package org.tnozamas.skyatlaz.api.repo

import com.google.gson.JsonParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeuItemTest {
    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/neu/$name")) { "missing fixture $name" }.readText()

    @Test
    fun `single grid recipe is crafting and text loses legacy codes`() {
        val item = NeuItem.parse(fixture("hyperion.json"))

        assertEquals("HYPERION", item.id)
        assertEquals("Necron's Blade (Unrefined)", item.displayName)
        assertEquals("minecraft:iron_sword", item.vanillaId)
        assertEquals("Gear Score: 425", item.lore.first())
        assertEquals(1, item.recipes.size)

        val recipe = item.recipes.single()
        assertEquals(NeuRecipeType.CRAFTING, recipe.type)
        assertEquals("HYPERION", recipe.outputId)
        assertEquals(1, recipe.outputCount)
        assertEquals(9, recipe.grid.size)
        assertEquals(NeuIngredient("NECRON_BLADE", 1.0), recipe.grid["B2"])
        assertTrue(recipe.inputs.isEmpty())
        assertNull(recipe.durationSeconds)
    }

    @Test
    fun `recipes array keeps crafting grids with their output count`() {
        val item = NeuItem.parse(fixture("abyssal_chestplate.json"))

        val recipe = item.recipes.single()
        assertEquals(NeuRecipeType.CRAFTING, recipe.type)
        assertEquals("ABYSSAL_CHESTPLATE", recipe.outputId)
        assertEquals(2, recipe.outputCount)
        assertEquals(NeuIngredient("GILL_MEMBRANE", 64.0), recipe.grid["A1"])
        assertEquals(NeuIngredient("ENCHANTED_SEA_LUMIES", 16.0), recipe.grid["C2"])
    }

    @Test
    fun `forge recipes expose inputs and duration`() {
        val item = NeuItem.parse(fixture("amber_material.json"))

        val recipe = item.recipes.single()
        assertEquals(NeuRecipeType.FORGE, recipe.type)
        assertEquals(21600, recipe.durationSeconds)
        assertEquals(listOf(NeuIngredient("GOLDEN_PLATE", 1.0), NeuIngredient("FINE_AMBER_GEM", 12.0)), recipe.inputs)
        assertTrue(recipe.grid.isEmpty())
    }

    @Test
    fun `ingredients parse optional and decimal amounts`() {
        assertEquals(NeuIngredient("HYPERION", 1.0), NeuIngredient.parse("HYPERION"))
        assertEquals(NeuIngredient("SKYBLOCK_COIN", 8.0), NeuIngredient.parse("SKYBLOCK_COIN:8.0"))
        assertEquals(NeuIngredient("ARMADILLO;1", 2.0), NeuIngredient.parse("armadillo;1:2"))
        assertNull(NeuIngredient.parse(""))
        assertNull(NeuIngredient.parse("HYPERION:0"))
    }

    @Test
    fun `a json page without an internal name is rejected`() {
        assertThrows(JsonParseException::class.java) { NeuItem.parse("""{"displayname": "No id"}""") }
    }
}
