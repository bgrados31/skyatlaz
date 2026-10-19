package org.tnozamas.skyatlaz

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.module.ModuleManager
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

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

    /** Tick tasks and the shared background pool. Ticked at the end of every client tick. */
    @JvmField
    val SCHEDULER =
        Scheduler(Scheduler.createExecutor()) { error ->
            LOGGER.error("Unhandled error in a scheduled task", error)
        }

    // TODO(phase-1): core.error adds a translated chat notice when a module is disabled.
    @JvmField
    val MODULES =
        ModuleManager(EVENTS, SCHEDULER) { module, error ->
            LOGGER.error("Module {} failed and was disabled", module.id, error)
        }

    override fun onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_ID)
        ClientTickEvents.END_CLIENT_TICK.register { SCHEDULER.tick() }
    }
}
