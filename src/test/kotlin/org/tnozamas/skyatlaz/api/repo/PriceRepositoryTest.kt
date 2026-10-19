package org.tnozamas.skyatlaz.api.repo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.api.http.HttpResponse
import org.tnozamas.skyatlaz.api.http.HttpService
import org.tnozamas.skyatlaz.api.http.HttpTransport
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.util.Rarity

class PriceRepositoryTest {
    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/api/$name")) { "missing fixture $name" }.readText()

    private val bazaarJson = fixture("bazaar.json")
    private val itemsJson = fixture("items.json")

    private val requested = ArrayList<String>()
    private val transport =
        HttpTransport { url, _ ->
            requested.add(url)
            HttpResponse(200, if (url == PriceRepository.BAZAAR_URL) bazaarJson else itemsJson)
        }
    private val background = ArrayList<Runnable>()
    private val scheduler = Scheduler({ background.add(it) }) { throw AssertionError(it) }
    private val bus = EventBus { _, error -> throw AssertionError(error) }
    private val repository =
        PriceRepository(HttpService(transport, null, scheduler), scheduler, bus, onProblem = { throw AssertionError(it) })

    private fun settle() {
        repeat(10) {
            while (background.isNotEmpty()) background.removeFirst().run()
            scheduler.tick()
        }
    }

    @Test
    fun `bazaar fixture parses quick status only`() {
        val snapshot = BazaarSnapshot.parse(bazaarJson)
        val cookie = snapshot.quotes.getValue("BOOSTER_COOKIE")

        assertEquals(3, snapshot.quotes.size)
        assertTrue(snapshot.lastUpdated > 0)
        assertTrue(cookie.instantBuy > cookie.instantSell) { "buying instantly costs more than selling instantly" }
        assertTrue(cookie.sellWeekly > 0)
    }

    @Test
    fun `catalog fixture parses ids, names, rarities and npc prices`() {
        val catalog = ItemCatalog.parse(itemsJson)
        val aote = catalog["ASPECT_OF_THE_END"]!!

        assertEquals("Aspect of the End", aote.name)
        assertEquals(Rarity.RARE, aote.rarity)
        assertEquals("SWORD", aote.category)
        assertEquals(56_000.0, aote.npcSellPrice)
        assertNull(catalog["NOT_AN_ITEM"])
    }

    @Test
    fun `nothing downloads until used`() {
        settle()

        assertTrue(requested.isEmpty())
        assertNull(repository.value("BOOSTER_COOKIE"))
    }

    @Test
    fun `using it loads data, announces it and values items`() {
        var updates = 0
        bus.register<PricesUpdatedEvent> { updates++ }

        repository.use()
        settle()

        assertEquals(2, updates)
        assertEquals(ItemValue.Source.BAZAAR, repository.value("BOOSTER_COOKIE")?.source)
        assertEquals(ItemValue(56_000.0, ItemValue.Source.NPC), repository.value("ASPECT_OF_THE_END"))
        assertNull(repository.value("NOT_AN_ITEM"))
    }

    @Test
    fun `bazaar refreshes while used and stops when released`() {
        val registration = repository.use()
        settle()
        val afterStart = requested.count { it == PriceRepository.BAZAAR_URL }

        repeat(PriceRepository.BAZAAR_REFRESH_TICKS) { scheduler.tick() }
        settle()
        assertEquals(afterStart + 1, requested.count { it == PriceRepository.BAZAAR_URL })

        registration.unregister()
        assertFalse(repository.isActive)
        assertNull(repository.bazaar)
        repeat(PriceRepository.BAZAAR_REFRESH_TICKS * 2) { scheduler.tick() }
        settle()
        assertEquals(afterStart + 1, requested.count { it == PriceRepository.BAZAAR_URL })
    }

    @Test
    fun `lowest bin flat format skips non numbers`() {
        val bins = LowestBins.parseFlat("""{"HYPERION": 1.2E9, "BOOSTER_COOKIE": 1.4E7, "BROKEN": "n/a"}""")

        assertEquals(2, bins.size)
        assertEquals(1.2e9, bins["HYPERION"])
    }

    @Test
    fun `auction prices sit between bazaar and npc when a provider is set`() {
        val lowestBinUrl = "https://provider.example/lowestbin"
        val withAuctions =
            PriceRepository(
                HttpService(
                    { url, _ ->
                        requested.add(url)
                        val body =
                            when (url) {
                                PriceRepository.BAZAAR_URL -> bazaarJson
                                lowestBinUrl -> """{"ASPECT_OF_THE_END": 250000, "BOOSTER_COOKIE": 1}"""
                                else -> itemsJson
                            }
                        HttpResponse(200, body)
                    },
                    null,
                    scheduler,
                ),
                scheduler,
                bus,
                onProblem = { throw AssertionError(it) },
                lowestBinUrl = lowestBinUrl,
            )

        withAuctions.use()
        settle()

        assertEquals(ItemValue(250_000.0, ItemValue.Source.AUCTION), withAuctions.value("ASPECT_OF_THE_END"))
        assertEquals(ItemValue.Source.BAZAAR, withAuctions.value("BOOSTER_COOKIE")?.source)
    }

    @Test
    fun `no lowest bin request without a provider`() {
        repository.use()
        settle()

        assertEquals(setOf(PriceRepository.BAZAAR_URL, PriceRepository.ITEMS_URL), requested.toSet())
    }

    @Test
    fun `several users share one lifetime`() {
        val first = repository.use()
        val second = repository.use()

        first.unregister()
        first.unregister()
        assertTrue(repository.isActive)
        second.unregister()
        assertFalse(repository.isActive)
    }
}
