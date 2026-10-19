package org.tnozamas.skyatlaz

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tnozamas.skyatlaz.api.hypixel.HypixelDebugCommands
import org.tnozamas.skyatlaz.api.hypixel.HypixelModApi
import org.tnozamas.skyatlaz.core.command.BuiltinCommands
import org.tnozamas.skyatlaz.core.command.CommandHelp
import org.tnozamas.skyatlaz.core.command.CommandInstaller
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.config.GeneralConfig
import org.tnozamas.skyatlaz.core.debug.ChatRecorder
import org.tnozamas.skyatlaz.core.debug.DebugCommands
import org.tnozamas.skyatlaz.core.debug.SnapshotStore
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.GameHooks
import org.tnozamas.skyatlaz.core.module.ModuleManager
import org.tnozamas.skyatlaz.core.module.ModuleServices
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

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

    /** Every setting, saved to `config/skyatlaz/config.json`. Loaded in [onInitializeClient] before modules. */
    @JvmField
    val CONFIG =
        ConfigManager(
            file =
                FabricLoader
                    .getInstance()
                    .configDir
                    .resolve(MOD_ID)
                    .resolve("config.json"),
            migrations = emptyList(),
            scheduler = SCHEDULER,
        ) { message, error -> LOGGER.warn(message, error) }

    /** Every `/skyatlaz` command. Nodes without an action print generated help. */
    @JvmField
    val COMMANDS: CommandRegistry =
        CommandRegistry(
            rootName = MOD_ID,
            rootDescriptionKey = "command.root",
            debugEnabled = { GeneralConfig.debugMode.value },
            onHelp = { node -> CommandHelp.show(COMMANDS, node) },
            onError = { node, error ->
                LOGGER.error("Command {} failed", node.usage, error)
                Chat.error(Tr.tr("command.failed"))
            },
        )

    /** The Hypixel Mod API owner; read `HYPIXEL.session` for location and party. */
    @JvmField
    val HYPIXEL = HypixelModApi(EVENTS, SCHEDULER) { message -> LOGGER.warn(message) }

    /** Module lifecycle. A failing module is stopped, logged and announced in chat; the game keeps running. */
    @JvmField
    val MODULES =
        ModuleManager(ModuleServices(EVENTS, SCHEDULER, COMMANDS, CONFIG)) { module, error ->
            LOGGER.error("Module {} failed and was disabled", module.id, error)
            Chat.error(Tr.tr("error.module_disabled", module.id))
        }

    override fun onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_ID)
        CONFIG.register(GeneralConfig)
        CONFIG.load()
        ClientLifecycleEvents.CLIENT_STOPPING.register { CONFIG.saveNow() }
        ClientTickEvents.END_CLIENT_TICK.register { SCHEDULER.tick() }
        GameHooks.install(EVENTS)
        HYPIXEL.install()
        CommandInstaller.install(COMMANDS)
        BuiltinCommands.register(COMMANDS)

        val recorder = ChatRecorder(EVENTS)
        recorder.setActive(GeneralConfig.debugMode.value)
        GeneralConfig.debugMode.onChange { enabled ->
            recorder.setActive(enabled)
            COMMANDS.refresh()
        }
        val dumps =
            SnapshotStore(
                FabricLoader
                    .getInstance()
                    .gameDir
                    .resolve(MOD_ID)
                    .resolve("dumps"),
            )
        DebugCommands(SCHEDULER, recorder, dumps).register(COMMANDS)
        HypixelDebugCommands(HYPIXEL, EVENTS).register(COMMANDS)
    }
}
