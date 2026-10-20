package org.tnozamas.skyatlaz.api.repo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.api.http.HttpResponse
import org.tnozamas.skyatlaz.api.http.HttpService
import org.tnozamas.skyatlaz.api.http.HttpTransport
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

class NeuRepositoryTest {
    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/neu/$name")) { "missing fixture $name" }.readText()

    private val itemJson = fixture("hyperion.json")
    private val requested = ArrayList<String>()
    private val problems = ArrayList<String>()
    private val background = ArrayList<Runnable>()
    private val scheduler = Scheduler({ background.add(it) }) { throw AssertionError(it) }
    private var now = 0L
    private val transport =
        HttpTransport { url, _ ->
            requested.add(url)
            if (url == NeuRepository.urlFor("HYPERION")) HttpResponse(200, itemJson) else HttpResponse(404, "")
        }
    private val repository =
        NeuRepository(
            HttpService(transport, null, scheduler),
            onProblem = { problems.add(it) },
            clock = { now },
        )

    private fun settle() {
        repeat(10) {
            while (background.isNotEmpty()) background.removeFirst().run()
            scheduler.tick()
        }
    }

    @Test
    fun `nothing downloads until an item is requested`() {
        settle()

        assertTrue(requested.isEmpty())
        assertNull(repository.cached("HYPERION"))
    }

    @Test
    fun `an item is downloaded once and then served from memory`() {
        var first: NeuItem? = null
        repository.load("hyperion") { first = it }
        settle()

        assertNotNull(first)
        assertEquals("HYPERION", first?.id)
        assertEquals(NeuRepository.urlFor("HYPERION"), requested.single())

        var second: NeuItem? = null
        repository.load("HYPERION") { second = it }

        assertEquals(first, second)
        assertEquals(1, requested.size)
        assertNotNull(repository.cached("HYPERION"))
    }

    @Test
    fun `concurrent requests share one download`() {
        val results = ArrayList<NeuItem?>()
        repository.load("HYPERION") { results.add(it) }
        repository.load("HYPERION") { results.add(it) }
        settle()

        assertEquals(2, results.size)
        assertEquals(1, requested.size)
        assertTrue(results.all { it?.id == "HYPERION" })
    }

    @Test
    fun `a failed id is not retried immediately`() {
        var result: NeuItem? = null
        repository.load("NOT_AN_ITEM") { result = it }
        settle()
        assertNull(result)
        assertEquals(1, requested.size)
        assertEquals(1, problems.size)

        repository.load("NOT_AN_ITEM") { result = it }
        assertEquals(1, requested.size)

        now += 61_000
        repository.load("NOT_AN_ITEM") { result = it }
        settle()
        assertEquals(2, requested.size)
    }

    @Test
    fun `invalid ids never touch the network`() {
        var result: NeuItem? = null
        repository.load("../HYPERION") { result = it }

        assertNull(result)
        assertTrue(requested.isEmpty())
    }
}
