package org.tnozamas.skyatlaz

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tnozamas.skyatlaz.api.http.HttpCache
import org.tnozamas.skyatlaz.api.http.HttpDebugCommands
import org.tnozamas.skyatlaz.api.http.HttpService
import org.tnozamas.skyatlaz.api.http.JdkHttpTransport
import org.tnozamas.skyatlaz.api.hypixel.HypixelDebugCommands
import org.tnozamas.skyatlaz.api.hypixel.HypixelModApi
import org.tnozamas.skyatlaz.api.repo.NeuDebugCommands
import org.tnozamas.skyatlaz.api.repo.NeuRepository
import org.tnozamas.skyatlaz.api.repo.PriceDebugCommands
import org.tnozamas.skyatlaz.api.repo.PriceRepository
import org.tnozamas.skyatlaz.core.command.BuiltinCommands
import org.tnozamas.skyatlaz.core.command.CommandHelp
import org.tnozamas.skyatlaz.core.command.CommandInstaller
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.config.GeneralConfig
import org.tnozamas.skyatlaz.core.debug.ChatRecorder
import org.tnozamas.skyatlaz.core.debug.DebugCapture
import org.tnozamas.skyatlaz.core.debug.DebugCommands
import org.tnozamas.skyatlaz.core.debug.DebugHudPreview
import org.tnozamas.skyatlaz.core.debug.SnapshotStore
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.GameHooks
import org.tnozamas.skyatlaz.core.keybind.KeyBind
import org.tnozamas.skyatlaz.core.keybind.KeyBindManager
import org.tnozamas.skyatlaz.core.keybind.KeyCombo
import org.tnozamas.skyatlaz.core.module.ModuleManager
import org.tnozamas.skyatlaz.core.module.ModuleServices
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.feature.economy.heldprice.HeldPriceModule
import org.tnozamas.skyatlaz.feature.economy.itemprice.ItemPriceModule
import org.tnozamas.skyatlaz.feature.skyblock.statushud.StatusHudModule
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.skyblock.SkyBlockHooks
import org.tnozamas.skyatlaz.skyblock.SkyBlockStateChangeEvent
import org.tnozamas.skyatlaz.skyblock.SkyBlockTracker
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.ui.config.ConfigCommand
import org.tnozamas.skyatlaz.ui.config.ConfigScreen
import org.tnozamas.skyatlaz.ui.hud.HudCommand
import org.tnozamas.skyatlaz.ui.hud.HudManager
import org.tnozamas.skyatlaz.ui.openScreenAfterChat
import org.tnozamas.skyatlaz.ui.palette.CommandPaletteScreen

/** Client entrypoint. Only wires core services together; features register through the module system. */
object SkyAtlaz : ClientModInitializer {
    const val MOD_ID = "skyatlaz"

    /** The binding that opens the command palette. */
    const val PALETTE_KEYBIND = "open_command_palette"

    /** The binding that saves the state as a dump. */
    const val CAPTURE_KEYBIND = "capture_state"

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

    /** Installed mod version, e.g. `0.1.0`. */
    val VERSION: String by lazy {
        FabricLoader
            .getInstance()
            .getModContainer(MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
    }

    /** All downloads: shared client, disk cache in `<game dir>/skyatlaz/cache`, results on the client thread. */
    @JvmField
    val HTTP =
        HttpService(
            transport = JdkHttpTransport("SkyAtlaz/$VERSION (Hypixel SkyBlock mod)"),
            cache =
                HttpCache(
                    FabricLoader
                        .getInstance()
                        .gameDir
                        .resolve(MOD_ID)
                        .resolve("cache"),
                ),
            scheduler = SCHEDULER,
        )

    /** The Hypixel Mod API owner; read `HYPIXEL.session` for location and party. */
    @JvmField
    val HYPIXEL = HypixelModApi(EVENTS, SCHEDULER) { message -> LOGGER.warn(message) }

    /** Bazaar prices and the item catalog; loaded only while some module uses them (`ctx.usePrices()`). */
    @JvmField
    val PRICES = PriceRepository(HTTP, SCHEDULER, EVENTS, onProblem = { message -> LOGGER.warn(message) })

    /** NEU-REPO item pages and recipes, fetched per item on demand. */
    @JvmField
    val NEU = NeuRepository(HTTP, onProblem = { message -> LOGGER.warn(message) })

    /** Parsed SkyBlock state (area, profile, purse...); features read `SKYBLOCK.state` or its change events. */
    @JvmField
    val SKYBLOCK = SkyBlockTracker(EVENTS)

    /** Every HUD element and the `/skyatlaz hud` editor. One Fabric hook draws all of them. */
    @JvmField
    val HUD = HudManager(CONFIG)

    /** Every key binding; features register theirs while they run. */
    @JvmField
    val KEYBINDS =
        KeyBindManager(CONFIG) { first, second ->
            Chat.warning(Tr.tr("keybind.conflict", Tr.tr(first.nameKey), Tr.tr(second.nameKey)))
        }

    /** Module lifecycle. A failing module is stopped, logged and announced in chat; the game keeps running. */
    @JvmField
    val MODULES =
        ModuleManager(ModuleServices(EVENTS, SCHEDULER, COMMANDS, CONFIG, PRICES, HUD, KEYBINDS)) { module, error ->
            LOGGER.error("Module {} failed and was disabled", module.id, error)
            Chat.error(Tr.tr("error.module_disabled", module.id))
        }

    override fun onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_ID)
        CONFIG.register(GeneralConfig)
        CONFIG.load()
        HUD.install()
        ClientLifecycleEvents.CLIENT_STOPPING.register { CONFIG.saveNow() }
        ClientTickEvents.END_CLIENT_TICK.register {
            SCHEDULER.tick()
            KEYBINDS.tick(Minecraft.getInstance().window)
        }
        GameHooks.install(EVENTS)
        HYPIXEL.install()
        SkyBlockHooks.install(SKYBLOCK, SCHEDULER, EVENTS)
        // Modules with an activation condition ("only on SkyBlock") start and stop with the shared state.
        EVENTS.register(SkyBlockStateChangeEvent::class.java) { MODULES.refreshActivation() }
        CommandInstaller.install(COMMANDS)
        BuiltinCommands.register(COMMANDS)
        HudCommand(HUD, SCHEDULER).register(COMMANDS)
        ConfigCommand(CONFIG, MODULES, KEYBINDS, SCHEDULER).register(COMMANDS)
        MODULES.register(StatusHudModule)
        MODULES.register(ItemPriceModule)
        MODULES.register(HeldPriceModule)
        // A core binding: the settings screen, from the keyboard.
        KEYBINDS.register(KeyBind("open_settings", KeyCombo.NONE)) {
            openScreenAfterChat(SCHEDULER) { ConfigScreen(CONFIG, MODULES, KEYBINDS) }
        }
        // A core binding: the command palette, the fuzzy search over every command.
        KEYBINDS.register(KeyBind(PALETTE_KEYBIND, KeyCombo(InputConstants.KEY_K, KeyCombo.CTRL))) {
            openScreenAfterChat(SCHEDULER) {
                CommandPaletteScreen(COMMANDS, SCHEDULER, KEYBINDS.comboOf(PALETTE_KEYBIND).key)
            }
        }

        val recorder = ChatRecorder(EVENTS)
        val hudPreview = DebugHudPreview(HUD, EVENTS, SKYBLOCK)
        recorder.setActive(GeneralConfig.debugMode.value)
        hudPreview.setActive(GeneralConfig.debugMode.value)
        GeneralConfig.debugMode.onChange { enabled ->
            recorder.setActive(enabled)
            hudPreview.setActive(enabled)
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
        DebugCommands(SCHEDULER, recorder, dumps, SKYBLOCK, PRICES).register(COMMANDS)
        HypixelDebugCommands(HYPIXEL, EVENTS).register(COMMANDS)
        HttpDebugCommands(HTTP).register(COMMANDS)
        PriceDebugCommands(PRICES, SCHEDULER, EVENTS).register(COMMANDS)
        NeuDebugCommands(NEU).register(COMMANDS)
        // A core binding: save the state as a dump. It fires with a screen open, because some states (the Heart of the
        // Mountain tree) only exist while an inventory is open, and then the chat is blocked.
        KEYBINDS.register(KeyBind(CAPTURE_KEYBIND, KeyCombo(InputConstants.KEY_F7), worksInScreen = true)) {
            DebugCapture.save(SCHEDULER, recorder, dumps, SKYBLOCK.state.area)
        }
    }
}
