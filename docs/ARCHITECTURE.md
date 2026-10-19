# Architecture

Status markers: **[done]** exists in code, **[planned]** target design, implement as written.

## Goals
1. **Centralized:** every cross-cutting concern has exactly one owner (one HTTP client, one event bus, one theme...).
2. **Pay for what you use:** a disabled module registers nothing and loads nothing.
3. **Scalable:** adding a feature never requires touching another feature.

## Package layout (`org.tnozamas.skyatlaz`)
```
SkyAtlaz.kt          [done]    Client entrypoint. Only wires core services.
i18n/                [done]    Tr: the only way to create user-facing text. withGradient(Gradient)
core/
  module/            [done]    Module, ModuleManager, ModuleContext, FeatureCategory
  event/             [done]    EventBus, GameHooks (Fabric callbacks -> bus); ChatReceivedEvent, ClientTickEvent,
                               WorldJoinEvent, WorldLeaveEvent
  config/            [done]    ConfigManager (JSON, migrations, debounced atomic saves), ConfigSection, ConfigEntry
  scheduler/         [done]    Tick tasks (later/every), shared async pool, runOnClient
  error/             [done]    rethrowIfFatal; isolation lives in EventBus/Scheduler/ModuleManager, notice via ui.chat
  command/           [done]    CommandRegistry (declarative tree, generated help), CommandInstaller (Fabric)
  debug/             [done]    dump / chat / open / theme, Snapshot fixtures, ChatRecorder (load: phase 2)
api/
  hypixel/           [done]    HypixelModApi (only Mod API user) -> HypixelSession (location, party), events
  http/              [done]    HttpService (SkyAtlaz.HTTP): async, disk cache + ETag/304, stale fallback, dedupe
  repo/              [done]    PriceRepository (bazaar + item catalog, demand driven via ctx.usePrices); NEU-REPO planned
skyblock/            [planned] Shared game state (island, area, profile, purse...), parsed once
ui/
  theme/             [partial] Theme color tokens + Gradient (OKLab LUT) done; spacing/font tokens planned
  chat/              [done]    Chat (info/success/warning/error/block): only chat output; ChatLayout (rules, columns)
  hud/               [planned] HUD element system + editor (move/scale)
  component/         [planned] Reusable widgets
  render/            [planned] World render helpers with reused buffers
util/                [done]    LegacyText, Numbers (parse/compact/grouped/roman), ChatPattern (+ ctx.onChat),
                               RarityLine + Rarity, SkyBlockItemData (custom_data)
mixin/ (Java)        [done]    Mixins: accessors, or guard + forward to Kotlin; no logic. PlayerTabOverlayAccessor
feature/<category>/<feature>/  [planned] One Module per feature
```

## Module system [done]
```kotlin
abstract class Module(val id: String, val category: FeatureCategory) {   // id: "dungeons.scorehud"
    open fun isActive(): Boolean = true                  // activation condition, cheap, re-checked on state change
    protected open fun onEnable(ctx: ModuleContext) {}   // ctx.on<Event> { }, ctx.track(registration)
    protected open fun onDisable() {}                    // release anything not tracked by ctx
}
```
- Global instances live in the entrypoint: `SkyAtlaz.EVENTS` (bus) and `SkyAtlaz.MODULES` (manager).
  Both are client-thread only.
- `ModuleManager` owns lifecycle. Enabling calls `onEnable` with a `ModuleContext`; every registration made
  through the context (events, HUD elements, commands, scheduled tasks) is tracked and **automatically removed on
  disable**. Modules never register directly with global systems.
- Modules are only enabled when their config toggle is on **and** their activation condition holds
  (e.g. "only on SkyBlock", "only in Dungeons"). Leaving the condition disables listeners again.
- Data is loaded lazily on first enable, never at startup.
- Any exception thrown by a module (enable, disable, a listener, its activation condition) is caught, reported
  once through the manager's failure handler, and the module is stopped and marked failed until the user toggles
  it off and on. `VirtualMachineError` still propagates (`core.error.rethrowIfFatal`). Planned: a
  translated chat notice. The game never crashes because of a feature.

## Event bus [done]
- Typed events matched by **exact class** (no subtype dispatch). Listeners stored per event type in arrays
  (copy-on-write on register, plain loop on post), ordered by priority (lower first). A listener removed during a
  post is skipped for the rest of that post.
  No reflection, no annotations scanning at dispatch time.
- `EventBus.hasListeners(type)` is O(1). Mixins check it before building an event object, so unused hooks
  cost one boolean check.
- Events are posted on the client thread. Async work posts results back through the scheduler.
- Game callbacks come from Fabric API events where they exist (`GameHooks`), mixins only where Fabric has no
  hook. Each bridge checks `hasListeners` before building the event; tick/world events are singletons.

## Config [done]
```kotlin
object ScoreHudConfig : ConfigSection("dungeons.scorehud") {
    val scale = decimal("scale", 1.0, 0.5..3.0)   // also boolean, int, text, choice<Enum>
}
object ScoreHudModule : Module("dungeons.scorehud", FeatureCategory.DUNGEONS) {
    override val config = ScoreHudConfig            // registered and loaded with the module
    override fun onEnable(ctx: ModuleContext) {
        ctx.watch(ScoreHudConfig.scale) { rebuildLayout() }   // react to changes, never poll
    }
}
```
- One file, `config/skyatlaz/config.json`: `{ "version": N, "values": { flat.sorted.keys: value } }`. Unknown keys
  are kept. Older versions are upgraded by `ConfigMigration`s; a corrupt file is moved aside (`.broken-*`).
- Values are always valid (clamped / replaced by defaults). `ConfigEntry.value` is a plain field read.
- Every module gets a `module.<id>.enabled` toggle (default `enabledByDefault`, off unless said otherwise);
  `ModuleManager.setEnabled` writes it, so module state persists.
- Saves are debounced (1 s after the last change), written off the client thread with an atomic replace, plus
  a synchronous save on client shutdown. `GeneralConfig.debugMode` gates DEBUG commands.
- Texts for the config screen: `config.<key>.name` and `config.<key>.description`.

## Commands [done]
All commands live under `/skyatlaz` in one `CommandRegistry` (`SkyAtlaz.COMMANDS`). Nothing is wired by hand:
```kotlin
ctx.command {                                   // module commands exist only while the module runs
    literal("pets") {
        category(CommandCategory.FEATURES)      // help section (GENERAL, FEATURES, TOOLS, DEBUG)
        description("command.pets")             // required on every node, translation key
        visibleWhen { SkyBlockState.onSkyBlock } // optional extra condition
        argument("name", StringArgumentType.word()) {
            description("command.pets.name")
            suggests { petNames }
            executes { call -> show(call.string("name")) }
        }
    }
}
```
- Any node without `executes` prints generated help (`CommandHelp`: brand title rule, blank-line spacing,
  categories on the root, page name + description + back link on subpages; entries clickable, hover
  shows syntax), so no path ends in "unknown or incomplete command". `/skyatlaz` is the root help.
- Declarations are validated before applying (names, descriptions, duplicate actions, literal/argument clash).
- Nodes are reference counted and never removed from Brigadier; unavailable nodes fail `requires`, and the
  installer refreshes Fabric's suggestion tree on every change. Call `COMMANDS.refresh()` when a
  `visibleWhen` condition changes.
- `DEBUG` commands are hidden unless debug mode is on (dev environment for now).
- Errors inside a command are logged and reported in chat (`command.failed`), never crash the client.

## SkyBlock state [planned]
- Single source of truth (`skyblock/`) built from Hypixel Mod API location packets, scoreboard, tab list and chat.
- Each source is parsed **once per tick at most, and only if its content hash changed**. Features read state;
  they never parse the scoreboard/tab themselves.
- State changes are published as events (`IslandChangeEvent`, `AreaChangeEvent`...).

## Hypixel APIs
- **Mod API** (`net.hypixel:mod-api`, compileOnly; users install the `hypixel-mod-api` Fabric mod): location,
  party info, player info. Only `HypixelModApi` talks to it; it translates packets into `HypixelLocation` /
  `HypixelParty`, posts `LocationChangeEvent` / `PartyInfoEvent` on the client thread and keeps the current state in
  `SkyAtlaz.HYPIXEL.session` (cleared on disconnect). Party requests are throttled (5 s cooldown, one pending), so responses
  are cached and shared.
- **Public API v2**: SkyBlock resources, bazaar and auctions endpoints need no key. Endpoints needing a key
  (profiles) cannot be used from a distributed mod with a dev key; we do not ship keys.
- All network calls go through `SkyAtlaz.HTTP` (`api.http.HttpService`): `get(url, maxAge, parse) { result }`. Fresh
  cache = no request; expired = conditional request (304 reuses the body); network failure = last copy marked
  `stale`; one download per URL at a time; parsing off the client thread. Cache in `<game dir>/skyatlaz/cache`.

## Rendering [planned]
- HUD elements re-compute their content when their data changes, not every frame. Rendering only draws.
- No allocations per frame: reuse buffers, precompute strings/components, cache text widths.

## Dependencies
| Dependency | Scope | Why |
|---|---|---|
| Fabric API, Fabric Language Kotlin | runtime | Base |
| Hypixel Mod API | compileOnly + localRuntime | Official server data |
| DevAuth | localRuntime, opt-in | Real account login in dev |
| JUnit 6 | test | Unit + architecture tests |

Adding a dependency requires an entry in `docs/DECISIONS.md` (size, license, maintenance, alternatives).
