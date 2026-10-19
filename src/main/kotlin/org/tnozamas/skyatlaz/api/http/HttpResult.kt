package org.tnozamas.skyatlaz.api.http

/** Outcome of [HttpService.get], delivered on the client thread. */
sealed interface HttpResult<out T> {
    /**
     * [value] parsed from the body. [fromCache]: no download happened (fresh cache or HTTP 304). [stale]: the
     * network failed and this is the last good copy; show it, maybe with a hint.
     */
    data class Success<T>(
        val value: T,
        val fromCache: Boolean,
        val stale: Boolean,
    ) : HttpResult<T>

    /** Nothing usable: network/HTTP error or unparsable body, and no cached copy. */
    data class Failure(
        val error: Throwable,
    ) : HttpResult<Nothing>
}
