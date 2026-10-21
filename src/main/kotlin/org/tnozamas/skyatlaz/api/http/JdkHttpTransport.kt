package org.tnozamas.skyatlaz.api.http

import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.zip.GZIPInputStream

/**
 * The one network client of the mod (`network-only-in-api-http` rule). Asks for gzip (bazaar and repo JSON compress
 * about 10x), follows redirects, times out instead of hanging a worker.
 */
class JdkHttpTransport(
    private val userAgent: String,
) : HttpTransport {
    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    override fun get(
        url: String,
        headers: Map<String, String>,
    ): HttpResponse = send(request(url, headers) { it.GET() })

    override fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): HttpResponse =
        send(
            request(url, headers) {
                it.header("Content-Type", "application/json")
                it.POST(HttpRequest.BodyPublishers.ofString(body))
            },
        )

    private fun request(
        url: String,
        headers: Map<String, String>,
        body: (HttpRequest.Builder) -> Unit,
    ): HttpRequest {
        val builder =
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip")
                .apply { headers.forEach { (name, value) -> header(name, value) } }
        body(builder)
        return builder.build()
    }

    private fun send(request: HttpRequest): HttpResponse {
        val response = client.send(request, BodyHandlers.ofByteArray())
        val responseHeaders =
            response
                .headers()
                .map()
                .entries
                .associate { (name, values) -> name.lowercase() to values.first() }
        val bytes = response.body()
        val body =
            if (responseHeaders["content-encoding"].equals("gzip", ignoreCase = true)) {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readAllBytes() }
            } else {
                bytes
            }
        return HttpResponse(response.statusCode(), body.toString(Charsets.UTF_8), responseHeaders)
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(15)
    }
}
