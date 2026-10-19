package org.tnozamas.skyatlaz.api.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.io.IOException
import java.nio.file.Path
import java.time.Duration

class HttpServiceTest {
    @TempDir
    lateinit var dir: Path

    private val url = "https://api.hypixel.net/v2/skyblock/bazaar"
    private val maxAge = Duration.ofMinutes(1)
    private var now = 1_000_000L

    /** Scripted server: answers in order, records the request headers it received. */
    private val answers = ArrayDeque<() -> HttpResponse>()
    private val requests = ArrayList<Map<String, String>>()
    private val transport =
        HttpTransport { _, headers ->
            requests.add(headers)
            answers.removeFirst()()
        }

    private val background = ArrayList<Runnable>()
    private val scheduler = Scheduler({ background.add(it) }) { throw AssertionError(it) }
    private val service by lazy { HttpService(transport, HttpCache(dir), scheduler) { now } }

    /** Runs background work and client-thread results until nothing is left. */
    private fun settle() {
        repeat(10) {
            while (background.isNotEmpty()) background.removeFirst().run()
            scheduler.tick()
        }
    }

    private fun fetch(): HttpResult<Int> {
        var result: HttpResult<Int>? = null
        service.get(url, maxAge, { it.length }) { result = it }
        settle()
        return result!!
    }

    @Test
    fun `downloads, parses off thread and caches`() {
        answers.add { HttpResponse(200, "hello", mapOf("etag" to "\"v1\"")) }

        assertEquals(HttpResult.Success(5, fromCache = false, stale = false), fetch())
    }

    @Test
    fun `fresh cache answers without a request`() {
        answers.add { HttpResponse(200, "hello") }
        fetch()
        now += maxAge.toMillis() - 1

        assertEquals(HttpResult.Success(5, fromCache = true, stale = false), fetch())
        assertEquals(1, requests.size)
    }

    @Test
    fun `expired cache is revalidated and 304 reuses the body`() {
        answers.add { HttpResponse(200, "hello", mapOf("etag" to "\"v1\"", "last-modified" to "Mon")) }
        answers.add { HttpResponse(304, "") }
        fetch()
        now += maxAge.toMillis()

        assertEquals(HttpResult.Success(5, fromCache = true, stale = false), fetch())
        assertEquals(mapOf("If-None-Match" to "\"v1\"", "If-Modified-Since" to "Mon"), requests[1])

        // The 304 refreshed the entry: no request inside the next window.
        now += maxAge.toMillis() - 1
        fetch()
        assertEquals(2, requests.size)
    }

    @Test
    fun `network failure falls back to the stale copy`() {
        answers.add { HttpResponse(200, "hello") }
        answers.add { throw IOException("offline") }
        fetch()
        now += maxAge.toMillis()

        assertEquals(HttpResult.Success(5, fromCache = true, stale = true), fetch())
    }

    @Test
    fun `server errors without a cache are failures`() {
        answers.add { HttpResponse(503, "busy") }

        assertInstanceOf(HttpResult.Failure::class.java, fetch())
    }

    @Test
    fun `parse errors are failures`() {
        answers.add { HttpResponse(200, "not a number") }
        var result: HttpResult<Int>? = null

        service.get(url, maxAge, { it.toInt() }) { result = it }
        settle()

        assertInstanceOf(HttpResult.Failure::class.java, result)
    }

    @Test
    fun `concurrent requests share one download`() {
        answers.add { HttpResponse(200, "hello") }
        val results = ArrayList<HttpResult<*>>()

        service.get(url, maxAge, { it.length }) { results.add(it) }
        service.getText(url, maxAge) { results.add(it) }
        settle()

        assertEquals(1, requests.size)
        assertEquals(listOf(HttpResult.Success(5, false, false), HttpResult.Success("hello", false, false)), results)
    }

    @Test
    fun `works without a disk cache`() {
        val uncached = HttpService(transport, null, scheduler) { now }
        answers.add { HttpResponse(200, "abc") }
        var result: HttpResult<String>? = null

        uncached.getText(url, maxAge) { result = it }
        settle()

        assertTrue(result is HttpResult.Success)
    }
}
