package org.tnozamas.skyatlaz.api.http

/** Performs one blocking GET. Called on worker threads only; [JdkHttpTransport] in game, fakes in tests. */
fun interface HttpTransport {
    fun get(
        url: String,
        headers: Map<String, String>,
    ): HttpResponse
}
