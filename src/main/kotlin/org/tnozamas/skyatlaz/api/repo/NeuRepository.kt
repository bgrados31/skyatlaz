package org.tnozamas.skyatlaz.api.repo

import org.tnozamas.skyatlaz.api.http.HttpResult
import org.tnozamas.skyatlaz.api.http.HttpService
import java.time.Duration

/**
 * Item pages and recipes from the community-maintained NEU-REPO data repository, fetched on demand:
 * `items/<ID>.json` from the repository's `master` branch. Nothing downloads until [load] asks for an item.
 *
 * A parsed item stays in a small LRU cache. The shared HTTP cache makes repeat lookups cheap across restarts, and a
 * failed id is not retried for [FAILURE_RETRY_MS] so a typo does not hammer GitHub. Call from the client thread;
 * [callback] also runs there.
 */
class NeuRepository(
    private val http: HttpService,
    private val onProblem: (String) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cache =
        object : LinkedHashMap<String, NeuItem>(CACHE_HINT, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NeuItem>): Boolean = size > MAX_CACHED
        }

    private val pending = HashMap<String, ArrayList<(NeuItem?) -> Unit>>()
    private val failures = HashMap<String, Long>()
    private var layout: HotmLayout? = null
    private var layoutPending: ArrayList<(HotmLayout?) -> Unit>? = null

    /** The parsed page if it is already in memory; never downloads. */
    fun cached(id: String): NeuItem? = normalize(id)?.let(cache::get)

    /** The tree layout if it is already in memory; never downloads. */
    fun cachedLayout(): HotmLayout? = layout

    /** Loads the layout of the Heart of the Mountain tree. [callback] gets null when the download fails. */
    fun loadLayout(callback: (HotmLayout?) -> Unit) {
        layout?.let {
            callback(it)
            return
        }
        layoutPending?.let {
            it.add(callback)
            return
        }
        layoutPending = arrayListOf(callback)
        http.get(LAYOUT_URL, MAX_AGE, HotmLayout::parse) { result ->
            when (result) {
                is HttpResult.Success -> {
                    layout = result.value
                    finishLayout(result.value)
                }

                is HttpResult.Failure -> {
                    onProblem("Could not load the tree layout: ${result.error.message}")
                    finishLayout(null)
                }
            }
        }
    }

    private fun finishLayout(value: HotmLayout?) {
        layoutPending?.forEach { it(value) }
        layoutPending = null
    }

    /** Loads [id] (any case) unless cached. [callback] gets null for invalid ids and load failures. */
    fun load(
        id: String,
        callback: (NeuItem?) -> Unit,
    ) {
        val key = normalize(id)
        if (key == null) {
            callback(null)
            return
        }
        cache[key]?.let {
            callback(it)
            return
        }
        pending[key]?.let {
            it.add(callback)
            return
        }
        val failedAt = failures[key]
        if (failedAt != null && clock() - failedAt < FAILURE_RETRY_MS) {
            callback(null)
            return
        }
        pending[key] = arrayListOf(callback)
        http.get(urlFor(key), MAX_AGE, NeuItem::parse) { result ->
            when (result) {
                is HttpResult.Success -> {
                    failures.remove(key)
                    cache[key] = result.value
                    finish(key, result.value)
                }

                is HttpResult.Failure -> {
                    failures[key] = clock()
                    onProblem("Could not load NEU item $key: ${result.error.message}")
                    finish(key, null)
                }
            }
        }
    }

    private fun finish(
        key: String,
        item: NeuItem?,
    ) {
        pending.remove(key)?.forEach { it(item) }
    }

    private fun normalize(id: String): String? {
        val clean = id.trim().uppercase()
        return clean.takeIf { ID_PATTERN.matches(it) }
    }

    companion object {
        /** Raw file for one item page, e.g. `.../items/HYPERION.json`. */
        const val ITEM_URL = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/"

        /** The layout of the Heart of the Mountain tree, the same community data. */
        const val LAYOUT_URL = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/hotmlayout.json"

        fun urlFor(id: String): String = ITEM_URL + id + ".json"

        /** Community data changes without releases; a week is plenty and the HTTP cache revalidates with ETag. */
        val MAX_AGE: Duration = Duration.ofDays(7)

        private const val FAILURE_RETRY_MS = 60_000L
        private const val CACHE_HINT = 128
        private const val MAX_CACHED = 128

        /** Item ids are file names in the repository: uppercase letters, digits, `_`, `-` and `;` (pets). */
        private val ID_PATTERN = Regex("[A-Z0-9_;-]{1,64}")
    }
}
