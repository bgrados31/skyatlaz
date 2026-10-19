package org.tnozamas.skyatlaz.api.repo

import org.tnozamas.skyatlaz.api.http.HttpResult
import org.tnozamas.skyatlaz.api.http.HttpService
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.scheduler.Cooldown
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.time.Duration

/**
 * Prices and item facts: official keyless Hypixel endpoints (bazaar, item catalog) plus, when [lowestBinUrl] is
 * set, lowest BIN auction prices from a third-party provider used with its author's permission. Demand driven, so
 * unused it costs nothing:
 * - nothing downloads until someone calls [use] (modules: `ctx.usePrices()`);
 * - while used, bazaar data refreshes every [BAZAAR_REFRESH_TICKS] and the catalog every [CATALOG_MAX_AGE];
 * - when the last user releases it, refreshing stops and the data is dropped (the disk cache makes the next
 *   start instant).
 * Every applied update posts [PricesUpdatedEvent]. Client thread only.
 */
class PriceRepository(
    private val http: HttpService,
    private val scheduler: Scheduler,
    private val bus: EventBus,
    private val onProblem: (String) -> Unit,
    private val lowestBinUrl: String? = LOWEST_BIN_URL,
) {
    var bazaar: BazaarSnapshot? = null
        private set

    var catalog: ItemCatalog? = null
        private set

    var lowestBins: LowestBins? = null
        private set

    val isActive: Boolean get() = users > 0

    private var users = 0
    private val refreshTasks = ArrayList<Registration>(2)
    private val problemCooldown = Cooldown(PROBLEM_COOLDOWN_TICKS)

    /** Keeps prices loaded and fresh until the returned registration is released. */
    fun use(): Registration {
        users++
        if (users == 1) start()
        var released = false
        return Registration {
            if (!released) {
                released = true
                users--
                if (users == 0) stop()
            }
        }
    }

    /** Best known value of one unit: bazaar instant-sell, else lowest BIN, else NPC price. Null when unknown. */
    fun value(id: String): ItemValue? {
        bazaar?.quotes?.get(id)?.let { quote ->
            if (quote.instantSell > 0) return ItemValue(quote.instantSell, ItemValue.Source.BAZAAR)
        }
        lowestBins?.get(id)?.takeIf { it > 0 }?.let { return ItemValue(it, ItemValue.Source.AUCTION) }
        return catalog
            ?.get(id)
            ?.npcSellPrice
            ?.takeIf { it > 0 }
            ?.let { ItemValue(it, ItemValue.Source.NPC) }
    }

    fun quote(id: String): BazaarQuote? = bazaar?.quotes?.get(id)

    fun item(id: String): CatalogItem? = catalog?.get(id)

    private fun start() {
        refreshCatalog()
        refreshMarket()
        refreshTasks.add(scheduler.every(BAZAAR_REFRESH_TICKS) { refreshMarket() })
        // The catalog changes rarely; re-checking once per hour is plenty (the HTTP cache decides if it downloads).
        refreshTasks.add(scheduler.every(CATALOG_RECHECK_TICKS) { refreshCatalog() })
    }

    private fun stop() {
        refreshTasks.forEach(Registration::unregister)
        refreshTasks.clear()
        bazaar = null
        catalog = null
        lowestBins = null
    }

    private fun refreshMarket() {
        refreshBazaar()
        lowestBinUrl?.let(::refreshLowestBins)
    }

    private fun refreshLowestBins(url: String) {
        http.get(url, BAZAAR_MAX_AGE, LowestBins::parseFlat) { result ->
            if (!isActive) return@get
            when (result) {
                is HttpResult.Success -> apply { lowestBins = result.value }
                is HttpResult.Failure -> report("lowest BIN prices", result.error)
            }
        }
    }

    private fun refreshBazaar() {
        http.get(BAZAAR_URL, BAZAAR_MAX_AGE, BazaarSnapshot::parse) { result ->
            if (!isActive) return@get
            when (result) {
                is HttpResult.Success -> apply { bazaar = result.value }
                is HttpResult.Failure -> report("bazaar", result.error)
            }
        }
    }

    private fun refreshCatalog() {
        http.get(ITEMS_URL, CATALOG_MAX_AGE, ItemCatalog::parse) { result ->
            if (!isActive) return@get
            when (result) {
                is HttpResult.Success -> apply { catalog = result.value }
                is HttpResult.Failure -> report("item catalog", result.error)
            }
        }
    }

    private inline fun apply(update: () -> Unit) {
        update()
        bus.post(PricesUpdatedEvent)
    }

    private fun report(
        what: String,
        error: Throwable,
    ) {
        if (problemCooldown.tryUse(scheduler.currentTick)) onProblem("Could not load $what: ${error.message}")
    }

    companion object {
        const val BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar"
        const val ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items"

        /** One minute: Hypixel refreshes bazaar data about every 20 s; a minute is fresh enough for displays. */
        const val BAZAAR_REFRESH_TICKS = 1200
        val BAZAAR_MAX_AGE: Duration = Duration.ofSeconds(55)

        val CATALOG_MAX_AGE: Duration = Duration.ofHours(6)
        const val CATALOG_RECHECK_TICKS = 20 * 60 * 60

        /**
         * Third-party lowest BIN endpoint (flat `{ "ID": price }` with Hypixel ids): Tricked's hosted lowestbins
         * (https://github.com/Tricked-dev/lowestbins), used with the credit its author asks for (README). Set to null
         * to disable; see the "Lowest BIN" decision in docs/DECISIONS.md.
         */
        val LOWEST_BIN_URL: String? = "https://lb.tricked.dev/lowestbins"

        private const val PROBLEM_COOLDOWN_TICKS = 20 * 60 * 5
    }
}
