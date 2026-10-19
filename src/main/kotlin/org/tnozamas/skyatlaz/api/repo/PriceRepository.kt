package org.tnozamas.skyatlaz.api.repo

import org.tnozamas.skyatlaz.api.http.HttpResult
import org.tnozamas.skyatlaz.api.http.HttpService
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.scheduler.Cooldown
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.time.Duration

/**
 * Prices and item facts from official, keyless Hypixel endpoints. Demand driven, so unused it costs nothing:
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
) {
    var bazaar: BazaarSnapshot? = null
        private set

    var catalog: ItemCatalog? = null
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

    /** Best known value of one unit: bazaar instant-sell, else NPC sell price. Null when unknown or not loaded. */
    fun value(id: String): ItemValue? {
        bazaar?.quotes?.get(id)?.let { quote ->
            if (quote.instantSell > 0) return ItemValue(quote.instantSell, ItemValue.Source.BAZAAR)
        }
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
        refreshBazaar()
        refreshTasks.add(scheduler.every(BAZAAR_REFRESH_TICKS) { refreshBazaar() })
        // The catalog changes rarely; re-checking once per hour is plenty (the HTTP cache decides if it downloads).
        refreshTasks.add(scheduler.every(CATALOG_RECHECK_TICKS) { refreshCatalog() })
    }

    private fun stop() {
        refreshTasks.forEach(Registration::unregister)
        refreshTasks.clear()
        bazaar = null
        catalog = null
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

        private const val PROBLEM_COOLDOWN_TICKS = 20 * 60 * 5
    }
}
