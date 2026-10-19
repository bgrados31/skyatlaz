package org.tnozamas.skyatlaz

import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Client entrypoint. Only wires core services together; features register through the module system. */
object SkyAtlaz : ClientModInitializer {
    const val MOD_ID = "skyatlaz"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_ID)
    }
}
