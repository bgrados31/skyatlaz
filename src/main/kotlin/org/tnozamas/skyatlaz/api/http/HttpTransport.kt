package org.tnozamas.skyatlaz.api.http

/** Performs one blocking GET. Called on worker threads only; [JdkHttpTransport] in game, fakes in tests. */
fun interface HttpTransport {
    fun get(
        url: String,
        headers: Map<String, String>,
    ): HttpResponse

    /**
     * Performs one blocking POST with a JSON body. Only the transports that need it implement it; the default
     * refuses, so a fake in a test never has to care about it.
     */
    fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): HttpResponse = throw UnsupportedOperationException("This transport does not support POST")
}
