package org.tnozamas.skyatlaz.api.repo

import com.mojang.brigadier.arguments.StringArgumentType
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.util.Numbers

/**
 * `/skyatlaz debug price <id>`: shows what [PriceRepository] knows about an item. Keeps the repository in use for a
 * minute afterwards, so repeated lookups do not reload. Joins the existing `debug` group.
 */
class PriceDebugCommands(
    private val prices: PriceRepository,
    private val scheduler: Scheduler,
    private val bus: EventBus,
) {
    private var hold: Registration? = null
    private var holdUntil = 0L

    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("price") {
                    description("command.debug.price")
                    argument("id", StringArgumentType.word()) {
                        description("command.debug.price.id")
                        suggests { prices.catalog?.ids ?: emptySet() }
                        executes { lookup(it.string("id").uppercase()) }
                    }
                }
            }
        }
    }

    private fun lookup(id: String) {
        keepLoaded()
        if (prices.catalog != null && prices.bazaar != null) {
            report(id)
            return
        }
        Chat.info(Tr.tr("debug.price.loading"))
        var waiting: Registration? = null
        waiting =
            bus.register<PricesUpdatedEvent> {
                if (prices.catalog != null && prices.bazaar != null) {
                    waiting?.unregister()
                    report(id)
                }
            }
    }

    private fun report(id: String) {
        val item = prices.item(id)
        val value = prices.value(id)
        if (item == null && value == null) {
            Chat.warning(Tr.tr("debug.price.unknown", Tr.verbatim(id)))
            return
        }
        val name = Tr.verbatim(item?.name ?: id)
        val quote = prices.quote(id)
        when {
            quote != null -> {
                Chat.info(
                    Tr.tr(
                        "debug.price.bazaar",
                        name,
                        Tr.verbatim(Numbers.compact(quote.instantBuy)),
                        Tr.verbatim(Numbers.compact(quote.instantSell)),
                        Tr.verbatim(Numbers.compact(quote.sellWeekly.toDouble())),
                    ),
                )
            }

            value?.source == ItemValue.Source.AUCTION -> {
                Chat.info(Tr.tr("debug.price.auction", name, Tr.verbatim(Numbers.compact(value.coins))))
            }

            value != null -> {
                Chat.info(Tr.tr("debug.price.npc", name, Tr.verbatim(Numbers.compact(value.coins))))
            }

            else -> {
                Chat.info(Tr.tr("debug.price.no_value", name))
            }
        }
    }

    private fun keepLoaded() {
        if (hold == null) hold = prices.use()
        holdUntil = scheduler.currentTick + HOLD_TICKS
        scheduler.later(HOLD_TICKS) {
            if (scheduler.currentTick >= holdUntil) {
                hold?.unregister()
                hold = null
            }
        }
    }

    private companion object {
        const val HOLD_TICKS = 20 * 60
    }
}
