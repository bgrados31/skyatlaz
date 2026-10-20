package org.tnozamas.skyatlaz.api.repo

import com.mojang.brigadier.arguments.StringArgumentType
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * `/skyatlaz debug neu <id>`: shows the NEU-REPO facts and recipes of an item. The page downloads on first use and
 * is then served from memory. Joins the existing `debug` group.
 */
class NeuDebugCommands(
    private val neu: NeuRepository,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("neu") {
                    description("command.debug.neu")
                    argument("id", StringArgumentType.word()) {
                        description("command.debug.neu.id")
                        executes { show(it.string("id")) }
                    }
                }
            }
        }
    }

    private fun show(rawId: String) {
        val id = rawId.uppercase()
        neu.cached(id)?.let {
            report(it)
            return
        }
        Chat.info(Tr.tr("debug.neu.loading"))
        neu.load(id) { item ->
            if (item == null) {
                Chat.warning(Tr.tr("debug.neu.unknown", Tr.verbatim(id)))
            } else {
                report(item)
            }
        }
    }

    private fun report(item: NeuItem) {
        Chat.info(
            Tr.tr(
                "debug.neu.item",
                Tr.verbatim(item.displayName),
                Tr.verbatim(item.id),
                Tr.verbatim(item.vanillaId ?: "-"),
            ),
        )
        if (item.recipes.isEmpty()) {
            Chat.info(Tr.tr("debug.neu.no_recipes", Tr.verbatim(item.id)))
            return
        }
        for (recipe in item.recipes) {
            when (recipe.type) {
                NeuRecipeType.CRAFTING -> {
                    Chat.info(
                        Tr.tr(
                            "debug.neu.crafting",
                            Tr.verbatim(recipe.outputId),
                            Tr.verbatim(recipe.outputCount.toString()),
                            Tr.verbatim(ingredients(recipe.grid.values)),
                        ),
                    )
                }

                NeuRecipeType.FORGE -> {
                    Chat.info(
                        Tr.tr(
                            "debug.neu.forge",
                            Tr.verbatim(recipe.outputId),
                            Tr.verbatim(recipe.outputCount.toString()),
                            Tr.verbatim(recipe.durationSeconds?.toString() ?: "?"),
                            Tr.verbatim(ingredients(recipe.inputs)),
                        ),
                    )
                }
            }
        }
    }

    private fun ingredients(values: Collection<NeuIngredient>): String {
        val totals = LinkedHashMap<String, Double>(values.size)
        for (ingredient in values) totals[ingredient.id] = (totals[ingredient.id] ?: 0.0) + ingredient.amount
        val text = StringBuilder()
        for ((id, value) in totals) {
            if (text.isNotEmpty()) text.append(", ")
            text.append(id).append(" x").append(amount(value))
        }
        return text.toString()
    }

    private fun amount(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
