# Architecture

Status markers: **[done]** exists in code, **[planned]** target design, implement as written.

## Goals
1. **Centralized:** every cross-cutting concern has exactly one owner (one HTTP client, one event bus, one theme...).
2. **Pay for what you use:** a disabled module registers nothing and loads nothing.
3. **Scalable:** adding a feature never requires touching another feature.

## Package layout (`org.tnozamas.skyatlaz`)
```
Skyatlaz.kt          [done]    Client entrypoint. Only wires core services.
i18n/                [done]    Tr: the only way to create user-facing text. withGradient(Gradient)
core/
  module/            [done]    Module, ModuleManager, ModuleContext, FeatureCategory
  event/             [done]    EventBus, GameHooks (Fabric callbacks -> bus); ChatReceivedEvent, ClientTickEvent,
                               WorldJoinEvent, WorldLeaveEvent
  config/            [done]    ConfigManager (JSON, migrations, debounced atomic saves), ConfigSection, ConfigEntry
  scheduler/         [done]    Tick tasks (later/every), shared async pool, runOnClient
  error/             [done]    rethrowIfFatal; isolation lives in EventBus/Scheduler/ModuleManager, notice via ui.chat
  command/           [done]    CommandRegistry (declarative tree, generated help), CommandInstaller (Fabric)
  keybind/           [done]    KeyCombo (key + modifiers), KeyBind, KeyBindManager (registry, persistence,
                                conflicts, one poll per tick)
  debug/             [done]    dump / chat / open / load / state / item / theme, Snapshot fixtures, ChatRecorder
  tooltip/           [done]    TooltipLines: where a feature adds its own lines to the vanilla item tooltip
api/
  hypixel/           [done]    HypixelModApi (only Mod API user) -> HypixelSession (location, party), events
  http/              [done]    HttpService (Skyatlaz.HTTP): async, disk cache + ETag/304, stale fallback, dedupe
  repo/              [done]    PriceRepository (bazaar + item catalog, demand driven via ctx.usePrices);
                               NeuRepository (NEU-REPO item pages + recipes, fetched per item on demand)
skyblock/            [done]    SkyBlockTracker: area, sub-area, profile, purse, bits, bank, level from scoreboard/tab
ui/
  theme/             [done]    Theme color tokens + Gradient (OKLab LUT) + Spacing (the shared rhythm of every layout)
  chat/              [done]    Chat (info/success/warning/error/block): only chat output; ChatLayout (rules, columns)
  hud/               [done]    HudManager (one Fabric hook), fractions-based placement, in-game editor
  component/         [done]    UiScreen/UiRoot + widgets: panel chrome, tabs, search, scrolling list, toggle,
                               slider, choice, text field, key bind, button; focus and keyboard navigation
  config/            [done]    ConfigScreen backed by ConfigEntry metadata; ConfigCommand (`/skyatlaz config`)
  palette/           [done]    CommandPaletteScreen: fuzzy search over the command tree (Ctrl+K) + its pure search
  render/            [done]    Shapes (drawing primitives with theme colors) + CachedText (text built on change).
                                World rendering waits: 26.2 has no Fabric world render event yet
util/                [done]    LegacyText, Numbers (parse/compact/grouped/roman), ChatPattern (+ ctx.onChat),
                               RarityLine + Rarity, SkyBlockItemData (custom_data), PriceLines (the price of an
                               item as translatable rows, shared by the price features)
mixin/ (Java)        [done]    Mixins: accessors, or guard + forward to Kotlin; no logic. PlayerTabOverlayAccessor,
                                ScreenTooltipMixin, CreativeTooltipMixin (item tooltip lines)
feature/<category>/<feature>/  [done]    One Module per feature. skyblock/statushud (status card), economy/itemprice
                                (prices in the item tooltip), economy/heldprice (price of the item in hand)
```

## Module system [done]
```kotlin
abstract class Module(val id: String, val category: FeatureCategory) {   // id: "dungeons.scorehud"
    open fun isActive(): Boolean = true                  // activation condition, cheap, re-checked on state change
    protected open fun onEnable(ctx: ModuleContext) {}   // ctx.on<Event> { }, ctx.track(registration)
    protected open fun onDisable() {}                    // release anything not tracked by ctx
}
```
- Global instances live in the entrypoint: `Skyatlaz.EVENTS` (bus) and `Skyatlaz.MODULES` (manager).
  Both are client-thread only.
- `ModuleManager` owns lifecycle. Enabling calls `onEnable` with a `ModuleContext`; every registration made
  through the context (events, HUD elements, commands, scheduled tasks) is tracked and **automatically removed on
  disable**. Modules never register directly with global systems.
- Modules are only enabled when their config toggle is on **and** their activation condition holds
  (e.g. "only on SkyBlock", "only in Dungeons"). Leaving the condition disables listeners again.
  Conditions are re-evaluated when the shared state changes: the entrypoint wires
  `SkyBlockStateChangeEvent` to `ModuleManager.refreshActivation()`.
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
- Texts for the config screen: `config.<key>.name` and `config.<key>.description`; choice values add
  `config.<key>.value.<lowercase enum name>`.
- `/skyatlaz config` builds tabs from `ConfigManager.registeredSections` (grouped by first id segment) plus
  `ModuleManager.toggles`. `hud.*` sections are excluded because `/skyatlaz hud` owns placement. A tab with one
  section uses `config.section.<id>`; several sections use `config.tab.<group>` plus their section headings.

## Key bindings [done]
- core.keybind owns every binding: KeyCombo (a key code plus modifiers), KeyBind (id, default key, texts) and
  KeyBindManager (registry, persistence, conflict detection, one poll per client tick).
- Features bind through ctx.keyBind(bind) { }, so a binding exists only while the feature runs and disappears
  with it. The key the player chose is saved in keybind.<id>.key.
- The keys are polled once per tick (never per frame) and fire on the press, not while held. A binding never fires
  while a screen is open, so typing in the chat or in the settings is never stolen.
- We do **not** use the vanilla KeyMapping: it adds itself to a static list in its constructor and cannot be
  removed, so a disabled feature would leave a dead key in the controls screen forever.
- The player changes a key in the settings screen (the "Controls" tab): click a row, press a key, done. Escape
  cancels. Two bindings on the same key are reported once in chat.
## Commands [done]
All commands live under `/skyatlaz` in one `CommandRegistry` (`Skyatlaz.COMMANDS`). Nothing is wired by hand:
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

## SkyBlock state [done]
- Single source of truth (`skyblock.SkyBlockTracker`, `Skyatlaz.SKYBLOCK`) built from the scoreboard and tab list;
  the Hypixel Mod API location complements it (island id, map) and chat/profile lines can be added as sources.
- The scoreboard and tab list are captured at most every 10 ticks (0.5 s) and only parsed when their content
  differs from the previous capture. Parsing (`SkyBlockParser`) is pure and tested from `Snapshot` dumps.
- State: area (`Area:` tab line), sub-area (sidebar glyph line), server id, profile, SkyBlock level and progress,
  purse (`Purse:`/`Piggy:`), bits and bank.
- Changes are published as `AreaChangeEvent` and `SkyBlockStateChangeEvent` on the client thread; features read
  `SKYBLOCK.state` and never parse the scoreboard/tab themselves.
- `/skyatlaz debug load <dump>` feeds a saved dump through the same parser and pauses live updates until the world
  changes, so UI can be iterated offline with real Hypixel data.

## Hypixel APIs
- **Mod API** (`net.hypixel:mod-api`, compileOnly; users install the `hypixel-mod-api` Fabric mod): location,
  party info, player info. Only `HypixelModApi` talks to it; it translates packets into `HypixelLocation` /
  `HypixelParty`, posts `LocationChangeEvent` / `PartyInfoEvent` on the client thread and keeps the current state in
  `Skyatlaz.HYPIXEL.session` (cleared on disconnect). Party requests are throttled (5 s cooldown, one pending), so responses
  are cached and shared.
- **Public API v2**: SkyBlock resources, bazaar and auctions endpoints need no key. Endpoints needing a key
  (profiles) cannot be used from a distributed mod with a dev key; we do not ship keys.
- All network calls go through `Skyatlaz.HTTP` (`api.http.HttpService`): `get(url, maxAge, parse) { result }`. Fresh
  cache = no request; expired = conditional request (304 reuses the body); network failure = last copy marked
  `stale`; one download per URL at a time; parsing off the client thread. Cache in `<game dir>/skyatlaz/cache`.

## NEU-REPO item data [done]
- NEU-REPO is a community-maintained data repository (MIT) with one page per SkyBlock item under `items/`. We do not
  bundle it: `NeuRepository` fetches `items/<ID>.json` from `master` only when a feature (or `debug neu`) asks for
  that item. The shared HTTP cache handles ETag/304 and a week of freshness, so startup and unused features cost
  nothing.
- `NeuItem` keeps the display name, vanilla item id, lore and the crafting/forge recipes NEU records. Text is
  stripped of legacy codes; recipes are typed (`CRAFTING` grid keyed `A1`..`C3`, `FORGE` input list + duration).
- Parsed pages live in a 128-entry LRU; unknown ids are not retried for a minute. `/skyatlaz debug neu <id>` prints
  the page and its recipes for verification.

## HUD [done]
```kotlin
class ScoreHud : HudElement("dungeons.scorehud", defaultX = 0.5, defaultY = 0.02) {
    override var width = 0
        private set
    override var height = 0
        private set

    override fun render(context: GuiGraphicsExtractor) { /* draw at (0, 0) */ }
}

override fun onEnable(ctx: ModuleContext) {
    ctx.hud(ScoreHud)   // removed automatically when the module stops
}
```
- One Fabric HUD hook (`skyatlaz:hud`) draws every registered element; with no elements it returns immediately.
  Modules register through `ctx.hud(element)`, never with Fabric directly.
- Position is `x`, `y` in `0..1` of the space left after scaling (`x = 0` left edge, `x = 1` right edge), plus
  `scale` in `0.5..3.0`. This survives window resizes and GUI scale changes without anchor enums.
- Each element's layout lives in `hud.<element id>.x/.y/.scale`, registered on first use (config sections are
  created lazily, so a module that never runs adds no keys). Values are clamped on load and on every edit.
- `/skyatlaz hud` opens the editor: drag to move, scroll to scale, `R` resets, `Tab` cycles, `Esc` closes. The
  normal HUD is hidden while the editor is open so elements are not drawn twice.
- Elements draw at `(0, 0)` in their own coordinates; the manager applies position and scale. Keep `width`,
  `height` and components cached, and rebuild them only when the data changes (never per frame).
- While debug mode is on, `core.debug.DebugHudPreview` adds a small SkyBlock state card. It is a development
  aid, not a player feature, and exists so the editor always has something to manipulate.
- The first player-facing element is `feature/skyblock/statushud`: a card with area, profile, level and money.
  It reads the shared state, turns it into rows (`StatusHudLogic`, pure and tested) and lets the element translate
  and measure them, so the content is rebuilt only when the state or the settings change.

## Rendering [partial]
- HUD elements re-compute their content when their data changes, not every frame. Rendering only draws.
- No allocations per frame: reuse buffers, precompute strings/components, cache text widths.
- `ui.render` holds the shared drawing primitives (`Shapes`, with theme colors instead of raw ARGB) and
  `CachedText`, which builds a component when its value changes and reuses it every frame.
- World render helpers are still missing: 26.2 replaced the old render events and Fabric does not expose the new
  submit-node API yet, so nothing in the mod needs them for now.

## Dependencies
| Dependency | Scope | Why |
|---|---|---|
| Fabric API, Fabric Language Kotlin | runtime | Base |
| Hypixel Mod API | compileOnly + localRuntime | Official server data |
| DevAuth | localRuntime, opt-in | Real account login in dev |
| JUnit 6 | test | Unit + architecture tests |

Adding a dependency requires an entry in `docs/DECISIONS.md` (size, license, maintenance, alternatives).
