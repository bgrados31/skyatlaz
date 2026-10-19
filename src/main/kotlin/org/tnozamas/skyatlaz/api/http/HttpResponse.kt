package org.tnozamas.skyatlaz.api.http

/** Raw answer of an [HttpTransport]. Header names are lowercase. */
data class HttpResponse(
    val status: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
)
