package org.tnozamas.skyatlaz

import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.module.ModuleManager

/** Client entrypoint. Only wires core services together; features register through the module system. */
object SkyAtlaz : ClientModInitializer {
    const val MOD_ID = "skyatlaz"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    /** The single event bus. Mixins check [EventBus.hasListeners] before posting. */
    @JvmField
    val EVENTS =
        EventBus { event, error ->
            LOGGER.error("Unhandled error in a listener for {}", event.javaClass.simpleName, error)
        }

    // TODO(phase-1): core.error adds a translated chat notice when a module is disabled.
    @JvmField
    val MODULES =
        ModuleManager(EVENTS) { module, error ->
            LOGGER.error("Module {} failed and was disabled", module.id, error)
        }

    override fun onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_ID)
    }
}
