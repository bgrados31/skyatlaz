# Architecture

Status markers: **[done]** exists in code, **[planned]** target design, implement as written.

## Goals
1. **Centralized:** every cross-cutting concern has exactly one owner (one HTTP client, one event bus, one theme...).
2. **Pay for what you use:** a disabled module registers nothing and loads nothing.
3. **Scalable:** adding a feature never requires touching another feature.

## Package layout (`org.tnozamas.skyatlaz`)
```
SkyAtlaz.kt          [done]    Client entrypoint. Only wires core services.
i18n/                [done]    Tr: the only way to create user-facing text.
core/
  module/            [planned] Module, ModuleManager, FeatureCategory
  event/             [planned] EventBus + event types
  config/            [planned] Typed config model, persistence, versioned migrations
  scheduler/         [planned] Tick tasks, throttling, the single async executor
  error/             [planned] Error isolation per module
  debug/             [planned] /skyatlaz debug commands, fixture dumps
api/
  hypixel/           [planned] Wrapper over Hypixel Mod API (subscriptions, cached responses)
  http/              [planned] Shared HTTP client: async, disk cache, ETag, rate limits
  repo/              [planned] Item/static data (Hypixel resources endpoints, NEU-REPO)
skyblock/            [planned] Shared game state (island, area, profile, purse...), parsed once
ui/
  theme/             [planned] Color/spacing/font tokens. Only place where colors exist.
  hud/               [planned] HUD element system + editor (move/scale)
  component/         [planned] Reusable widgets
  render/            [planned] World render helpers with reused buffers
util/                [planned] ChatMatcher, ItemParser, NumberFormat, text helpers
mixin/ (Java)        [done]    Mixins: guard + forward to Kotlin, no logic
feature/<category>/<feature>/  [planned] One Module per feature
```

## Module system [planned]
```kotlin
abstract class Module(val id: String, val category: FeatureCategory) {
    protected open fun onEnable(ctx: ModuleContext) {}   // register listeners, HUD elements, load data
    protected open fun onDisable() {}                    // release anything not tracked by ctx
}
```
- `ModuleManager` owns lifecycle. Enabling calls `onEnable` with a `ModuleContext`; every registration made
  through the context (events, HUD elements, commands, scheduled tasks) is tracked and **automatically removed on
  disable**. Modules never register directly with global systems.
- Modules are only enabled when their config toggle is on **and** their activation condition holds
  (e.g. "only on SkyBlock", "only in Dungeons"). Leaving the condition disables listeners again.
- Data is loaded lazily on first enable, never at startup.
- Any exception thrown by a module is caught by `core.error`, logged once, and the module is disabled with a
  translated chat notice. The game never crashes because of a feature.

## Event bus [planned]
- Typed events, listeners stored per event type in arrays (copy-on-write on register, plain loop on post).
  No reflection, no annotations scanning at dispatch time.
- `EventBus.hasListeners(type)` is O(1). Mixins check it before building an event object, so unused hooks
  cost one boolean check.
- Events are posted on the client thread. Async work posts results back through the scheduler.

## SkyBlock state [planned]
- Single source of truth (`skyblock/`) built from Hypixel Mod API location packets, scoreboard, tab list and chat.
- Each source is parsed **once per tick at most, and only if its content hash changed**. Features read state;
  they never parse the scoreboard/tab themselves.
- State changes are published as events (`IslandChangeEvent`, `AreaChangeEvent`...).

## Hypixel APIs
- **Mod API** (`net.hypixel:mod-api`, compileOnly; users install the `hypixel-mod-api` Fabric mod): location,
  party info, player info. Only `api.hypixel` talks to it. Request cooldowns are per packet type, so responses
  are cached and shared.
- **Public API v2**: SkyBlock resources, bazaar and auctions endpoints need no key. Endpoints needing a key
  (profiles) cannot be used from a distributed mod with a dev key; we do not ship keys.
- All network calls go through `api.http` and run off the client thread.

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
