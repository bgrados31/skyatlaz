package org.tnozamas.skyatlaz.api.http

import org.tnozamas.skyatlaz.core.error.rethrowIfFatal
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.io.IOException
import java.time.Duration

/**
 * The shared HTTP service. Every external download goes through [get]:
 * - work happens on the scheduler's pool, results come back on the client thread;
 * - a cached body younger than `maxAge` is used without any request; older ones are revalidated with
 *   `If-None-Match` / `If-Modified-Since`, and HTTP 304 reuses the cached body;
 * - if the network fails, the last good copy is returned as `stale` instead of nothing;
 * - concurrent requests for one URL share a single download;
 * - parsing runs off the client thread too (bazaar JSON is megabytes).
 *
 * Call [get] from the client thread.
 */
class HttpService(
    private val transport: HttpTransport,
    private val cache: HttpCache?,
    private val scheduler: Scheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Waiter(
        val parse: (String) -> Any,
        val callback: (HttpResult<Any>) -> Unit,
    )

    /** Raw outcome of one download, before per-caller parsing. */
    private class Raw(
        val body: String,
        val fromCache: Boolean,
        val stale: Boolean,
    )

    private val inFlight = HashMap<String, ArrayList<Waiter>>()

    fun <T : Any> get(
        url: String,
        maxAge: Duration,
        parse: (String) -> T,
        callback: (HttpResult<T>) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        val waiter = Waiter(parse, callback as (HttpResult<Any>) -> Unit)
        inFlight[url]?.let {
            it.add(waiter)
            return
        }
        inFlight[url] = arrayListOf(waiter)
        scheduler.async(work = { fetch(url, maxAge.toMillis()) }, onError = { finish(url, Result.failure(it)) }) { raw ->
            finish(url, Result.success(raw))
        }
    }

    /** Convenience for plain text. */
    fun getText(
        url: String,
        maxAge: Duration,
        callback: (HttpResult<String>) -> Unit,
    ) = get(url, maxAge, { it }, callback)

    private fun finish(
        url: String,
        raw: Result<Raw>,
    ) {
        val waiters = inFlight.remove(url) ?: return
        val outcome = raw.getOrNull()
        if (outcome == null) {
            val error = raw.exceptionOrNull() ?: IOException("Download failed for $url")
            for (waiter in waiters) waiter.callback(HttpResult.Failure(error))
            return
        }
        for (waiter in waiters) {
            scheduler.async(
                work = { waiter.parse(outcome.body) },
                onError = { waiter.callback(HttpResult.Failure(it)) },
            ) { value -> waiter.callback(HttpResult.Success(value, outcome.fromCache, outcome.stale)) }
        }
    }

    /** Worker thread: cache check, conditional request, fallback to the stale copy. */
    private fun fetch(
        url: String,
        maxAgeMillis: Long,
    ): Raw {
        val now = clock()
        val cached = cache?.read(url)
        if (cached != null && now - cached.fetchedAt < maxAgeMillis) return Raw(cached.body, fromCache = true, stale = false)

        val headers = HashMap<String, String>()
        cached?.etag?.let { headers["If-None-Match"] = it }
        cached?.lastModified?.let { headers["If-Modified-Since"] = it }
        val response =
            try {
                transport.get(url, headers)
            } catch (error: Throwable) {
                error.rethrowIfFatal()
                return cached?.let { Raw(it.body, fromCache = true, stale = true) } ?: throw error
            }

        return when {
            response.status == HTTP_NOT_MODIFIED && cached != null -> {
                cache.touch(url, cached, now)
                Raw(cached.body, fromCache = true, stale = false)
            }

            response.status in HTTP_OK_RANGE -> {
                cache?.write(url, HttpCache.Entry(response.body, response.headers["etag"], response.headers["last-modified"], now))
                Raw(response.body, fromCache = false, stale = false)
            }

            cached != null -> {
                Raw(cached.body, fromCache = true, stale = true)
            }

            else -> {
                throw IOException("HTTP ${response.status} for $url")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        val HTTP_OK_RANGE = 200..299
    }
}
