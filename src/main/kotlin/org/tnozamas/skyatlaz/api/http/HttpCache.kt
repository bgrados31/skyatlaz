package org.tnozamas.skyatlaz.api.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Disk cache of GET bodies with their validators: `<sha1(url)>.body` + `<sha1(url)>.meta` (JSON). Worker threads
 * only. A cache problem is never fatal: unreadable entries count as missing.
 */
class HttpCache(
    private val directory: Path,
) {
    data class Entry(
        val body: String,
        val etag: String?,
        val lastModified: String?,
        val fetchedAt: Long,
    )

    fun read(url: String): Entry? =
        runCatching {
            val meta = meta(url)
            val body = body(url)
            if (!Files.exists(meta) || !Files.exists(body)) return null
            val json = JsonParser.parseString(Files.readString(meta)).asJsonObject
            Entry(
                body = Files.readString(body),
                etag = json.get("etag")?.takeUnless { it.isJsonNull }?.asString,
                lastModified = json.get("lastModified")?.takeUnless { it.isJsonNull }?.asString,
                fetchedAt = json.get("fetchedAt").asLong,
            )
        }.getOrNull()

    fun write(
        url: String,
        entry: Entry,
    ) {
        Files.createDirectories(directory)
        Files.writeString(body(url), entry.body)
        writeMeta(url, entry)
    }

    /** Marks a still-valid entry (HTTP 304) as fresh again without rewriting the body. */
    fun touch(
        url: String,
        entry: Entry,
        now: Long,
    ) = writeMeta(url, entry.copy(fetchedAt = now))

    private fun writeMeta(
        url: String,
        entry: Entry,
    ) {
        val json =
            JsonObject().apply {
                addProperty("url", url)
                addProperty("etag", entry.etag)
                addProperty("lastModified", entry.lastModified)
                addProperty("fetchedAt", entry.fetchedAt)
            }
        Files.writeString(meta(url), json.toString())
    }

    private fun body(url: String) = directory.resolve(key(url) + ".body")

    private fun meta(url: String) = directory.resolve(key(url) + ".meta")

    private fun key(url: String): String =
        MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
}
