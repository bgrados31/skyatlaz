package org.tnozamas.skyatlaz.api.http

import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.util.Numbers
import java.time.Duration

/**
 * `/skyatlaz debug http`: downloads the public bazaar endpoint through [HttpService] and reports size and source
 * (network, cache, stale), to check transport, gzip and caching in game. Joins the existing `debug` group.
 */
class HttpDebugCommands(
    private val http: HttpService,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("http") {
                    description("command.debug.http")
                    executes { fetch() }
                }
            }
        }
    }

    private fun fetch() {
        val started = System.nanoTime()
        http.getText(BAZAAR_URL, Duration.ofSeconds(CACHE_SECONDS)) { result ->
            val millis = (System.nanoTime() - started) / NANOS_PER_MILLI
            when (result) {
                is HttpResult.Success -> {
                    val source =
                        when {
                            result.stale -> "debug.http.source.stale"
                            result.fromCache -> "debug.http.source.cache"
                            else -> "debug.http.source.network"
                        }
                    Chat.success(
                        Tr.tr(
                            "debug.http.result",
                            Tr.verbatim(Numbers.compact(result.value.length.toDouble())),
                            Tr.tr(source),
                            Tr.verbatim(millis.toString()),
                        ),
                    )
                }

                is HttpResult.Failure -> {
                    Chat.error(Tr.tr("debug.http.failed", Tr.verbatim(result.error.message ?: result.error.javaClass.simpleName)))
                }
            }
        }
    }

    private companion object {
        const val BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar"
        const val CACHE_SECONDS = 60L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
