# Development Rules — Minecraft Mod (generic template)

Adapted from the SkyAtlaz ruleset, with all SkyBlock/Hypixel-specific content removed.
Copy it to the root of the new mod project as `AGENTS.md` (and/or `CLAUDE.md`) and fill in the `<...>`
placeholders in section 0. It applies to any Minecraft mod (Fabric, NeoForge, Forge, Quilt) and any
language (Kotlin, Java).

---

## 0. Project profile (fill this in first)

| Placeholder | Meaning | Example |
|---|---|---|
| `<MOD_NAME>` | Human name of the mod | `CoolMod` |
| `<PACKAGE>` | Root package / mod id | `com.example.coolmod` |
| `<MC_VERSION>` | Target Minecraft version | `1.21.4` |
| `<LOADER>` | Mod loader | `Fabric` |
| `<LANG>` | Main language | `Kotlin` |
| `<FORMATTER>` | Formatter command | `./gradlew spotlessApply` |
| `<CHECK>` | Full verification command | `./gradlew check` |

Everything else in this document assumes these commands exist. If the project does not have them yet,
set them up before writing features.

---

## 1. Start of every session

1. Read `docs/PROGRESS.md`: current phase, next steps, open questions. It is the handoff between
   sessions and models.
2. Read `docs/HANDOFF.md` if it exists: state of the task another agent left half-done.
3. Read the doc for the area you are about to touch (map in section 11). Do not guess the architecture;
   follow it.

## 2. Commands

| Command | What |
|---|---|
| `<CHECK>` | Format check, compile, unit + architecture tests. **Must be green before finishing.** |
| `<FORMATTER>` | Auto-format. Do not hand-format code. |
| `./gradlew test` | Unit tests only (fast loop). |
| `./gradlew runClient` | Launch the game in the dev client. |
| `./gradlew build` | Build the mod jar into `build/libs/`. |

## 3. Non-negotiable rules

These are enforced automatically where possible (section 10). Do not work around them.

- **English only** in code, comments, docs and commits. Talk to the user in the user's language.
- **No hardcoded user-facing text.** Every string the player sees goes through the translation helper
  (`Tr.tr("key")`) and the key exists in the lang file. Never inline a literal in a message, tooltip,
  item name or config label.
- **Reuse before you write.** Colors/spacing from the theme, HTTP from the http client, threads from the
  scheduler, text parsing from `util`. If the same kind of code would appear twice, build the shared
  component first.
- **Features never import other features.** Features talk to each other through core events and shared
  state only.
- **A disabled module costs nothing:** no listeners registered, no data loaded, no per-tick work.
- **Performance first:** no allocations in render/tick hot paths, no blocking calls on the client thread,
  compute on change instead of every frame.
- **Multiplayer server rules:** never automate input, never modify packets, never give movement/vision
  or combat advantages. Classify each feature (allowed / gray / forbidden) in the project's rules doc
  before writing it. Forbidden -> stop and tell the user.
- **Java only for mixins**, and mixins only forward to the main language (guard check + call, no logic).
- **Reference mods are read-only study material.** Learn the approach, never copy the code (license).

## 4. Architecture expectations

Keep this shape. It is what makes the rest of the rules work.

```
<entrypoint>            Only wires core services and registers modules.
i18n/                   Translation helper. The only way to create user-facing text.
core/
  module/               Module, ModuleManager, ModuleContext, categories
  event/                EventBus + game hooks (engine callbacks -> events)
  config/               ConfigManager (JSON, migrations, debounced atomic saves), ConfigSection
  scheduler/            Tick tasks (later/every), shared async pool, runOnClient
  command/              Command registry (declarative tree, generated help)
  debug/                Dump / snapshot / debug commands, fixtures
api/
  http/                 One HTTP client: async, disk cache, dedupe, rate limit
  <data sources>        One owner per external data source
<game state>/           Shared world/session state, parsed once
ui/
  theme/                Color, spacing and font tokens + gradients
  component/            Widgets: panel, tabs, search, scrolling list, toggle, slider, text field
  hud/                  HUD overlays and the in-game HUD editor
  render/               World render helpers with reused buffers
util/                   Text parsing, number format, shared small helpers
mixin/ (Java)           Guard + forward to the main language. No logic.
feature/<category>/<feature>/   One module per feature
```

Design goals:

1. **Centralized:** every cross-cutting concern has exactly one owner (one HTTP client, one event bus,
   one theme).
2. **Pay for what you use:** a disabled module registers nothing and loads nothing.
3. **Scalable:** adding a feature never requires touching another feature.

### Module system

```kotlin
abstract class Module(val id: String, val category: FeatureCategory) {
    open fun isActive(): Boolean = true                  // activation condition, cheap
    protected open fun onEnable(ctx: ModuleContext) {}   // ctx.on<Event> { }, ctx.track(registration)
    protected open fun onDisable() {}                    // release anything not tracked by ctx
}
```

- The manager owns the lifecycle. Everything a module registers (events, HUD elements, commands,
  scheduled tasks) goes through the context and is **automatically removed on disable**.
- A module runs only when its config toggle is on **and** its activation condition holds. Conditions are
  re-evaluated when the shared state changes.
- Data loads lazily on first enable, never at startup.
- Any exception inside a module (enable, disable, listener, condition) is caught, reported once, and the
  module is stopped and marked failed until the user toggles it off and on. The game never crashes
  because of a feature. Fatal errors (`VirtualMachineError`) still propagate.

### Event bus

- Typed events matched by **exact class** (no subtype dispatch). Listeners stored per event type in
  arrays (copy-on-write on register, plain loop on post), ordered by priority.
- No reflection and no annotation scanning at dispatch time.
- `hasListeners(type)` is O(1). Hooks check it before building the event object, so unused hooks cost
  one boolean check.
- Events post on the client thread. Async work posts results back through the scheduler.
- Engine callbacks come from loader events where they exist; mixins only where the loader has no hook.

### Config

```kotlin
object ScoreHudConfig : ConfigSection("scorehud") {
    val scale = decimal("scale", 1.0, 0.5..3.0)   // also boolean, int, text, choice<Enum>
}
object ScoreHudModule : Module("scorehud", FeatureCategory.MISC) {
    override val config = ScoreHudConfig                 // registered and loaded with the module
    override fun onEnable(ctx: ModuleContext) {
        ctx.watch(ScoreHudConfig.scale) { rebuildLayout() }  // react to changes, never poll
    }
}
```

- One file, `config/<mod>/config.json`: `{ "version": N, "values": { flat.sorted.keys: value } }`. Unknown
  keys are kept. Older versions are upgraded by migrations; a corrupt file is moved aside.
- Values are always valid (clamped / replaced by defaults).
- Every module gets an automatic `module.<id>.enabled` toggle that persists.
- Saves are debounced, written off the client thread with an atomic replace, plus a synchronous save on
  shutdown.
- Labels come from the lang file: `config.<key>.name`, `config.<key>.description`, and
  `config.<key>.value.<lowercase enum name>` for choices.

### Commands

All commands live under one root (`/<mod>`) in a single registry. Nothing is wired by hand.

- Every node has a description key and, when it has no `executes`, prints generated help. No path ends
  in "unknown or incomplete command".
- Module commands exist only while the module runs.
- Never register commands directly with the loader/command framework.

## 5. Workflow

- **New feature:** use the project's `new-feature` skill. It carries the mandatory checklist and the file
  template so every feature has the same shape.
- **Before writing code:** check `docs/PROGRESS.md` for the core pieces the feature needs. If a shared
  piece is missing, build it in core/ui/util first, with tests, then the feature.
- **Library/API docs:** use Context7 (or the equivalent docs tool) instead of memory. Do not guess APIs.
- **Every logic change ships with a fast unit test.** Parsers get fixture-based tests.
- **Commits:** Conventional Commits, enforced by a git hook:
  `<type>(<scope>): <lowercase description, max 72 chars>`
  Types: `feat`, `fix`, `perf`, `refactor`, `docs`, `test`, `build`, `ci`, `chore`, `style`, `revert`.
  Scope is the area (`core`, `event`, `config`, `hud`, `i18n`, `api`, ...). One logical change per commit.
  Commit only when the user asks or at the end of an agreed task.
- **Before ending:** `<CHECK>` green and `docs/PROGRESS.md` updated. Report the real result of the
  commands you ran; never claim a green build you did not see.
- **Record any non-obvious decision** in `docs/DECISIONS.md` (what was decided, why, alternatives).

## 6. Code style

- Formatting is owned by the formatter (Spotless/ktlint or the project equivalent). Do not hand-format.
- Names: `PascalCase` types, `camelCase` functions/properties, `SCREAMING_SNAKE_CASE` constants, packages
  lowercase without underscores. Feature folders: `feature/<category>/<feature>/`.
- One public top-level type per file; the file is named after it.
- Prefer `val`, immutable data, `private`/`internal` by default. Expose the smallest API possible.
- KDoc on every public type and on any non-obvious function. Explain **why**, not what.
- No commented-out code. No TODO without context (`TODO(phase-2): reason`).
- Read the relevant files before editing. Never invent APIs, paths or functions: verify in the code.
- Make small, focused changes. Do not rewrite what was not asked.
- Ask before destructive actions (deleting files, `git reset`, force push).

## 7. Reuse

Before writing anything, search for an existing helper. If the same kind of code would appear twice,
create the shared component first.

| Need | Owner |
|---|---|
| Colors, spacing, fonts, gradients | `ui.theme` |
| Widgets | `ui.component` |
| HUD overlays | `ui.hud` |
| HTTP / external data | `api.http` |
| Game/server data | `api.<source>` |
| Shared game state | `<gamestate>` |
| Threads, delays, throttling | `core.scheduler` |
| Text/chat/item parsing, number format | `util` |
| User-facing text | `i18n.Tr` |
| Chat messages | `ui.chat` |
| Commands | the command registry, never the loader API directly |
| Settings | `ConfigSection` objects + `ctx.watch(entry)` |

## 8. Performance

The mod runs every module every tick; every rule here matters.

- **Disabled = zero cost.** Register listeners in `onEnable` through the module context only.
- **Hot paths** (render, tick, chat, packet handlers): no allocations, no `String.format`, no regex
  without a cheap prefilter (`startsWith`/`contains` before `Regex`), no collection operator chains
  (`map`/`filter` allocate), no boxing. Plain loops and preallocated structures.
- **Compute on change, not per frame.** Cache derived values; invalidate through events.
- **Compile regexes once** (`private val PATTERN = Regex(...)` at object level).
- **Never block the client thread.** I/O and network are async through the scheduler; never
  `Thread.sleep`, never `runBlocking`.
- Lookups: `HashMap`/arrays keyed by stable ids; avoid linear scans over big lists each tick.
- Throttle expensive checks (e.g. every 20 ticks) via the scheduler instead of counting ticks by hand.
- `inline` small higher-order helpers used in hot paths.
- Measure before optimizing bigger changes (Spark profiler / per-module timing).

## 9. Testing strategy

Test in layers and go to the real server/client as late as possible.

| Layer | What it proves | Speed | Share of testing |
|---|---|---|---|
| 1. Unit tests + fixtures | Parsers, calculators, state machines are correct | ms | Most of it |
| 2. Offline sandbox (singleplayer + injected state) | Rendering, layout, HUD editor, screens, module on/off | seconds, hot reload | UI work |
| 3. Real environment | Real data, timing, edge cases not captured | minutes | Capture fixtures + final check |

- **Fast tests always:** unit tests plus architecture tests, under ~10 s for the whole suite.
- **Architecture tests** enforce the source rules (section 10).
- **i18n tests** verify used keys exist, keys are namespaced and unique.
- **Fixtures** (real chat lines, scoreboard/tab snapshots, item data) live in `src/test/resources/fixtures/`.
  Tests read them with a pure snapshot helper, no game needed.
- Every bug found in game becomes a fixture + a failing test first, then the fix.
- Keep logic out of engine classes so it can be tested with plain inputs.

## 10. Automatic enforcement (recommended)

Do not rely on the agent remembering the rules; gate them in the build.

1. `config/source-rules.json` — single source of truth for pattern-based rules:

```json
{
  "rules": [
    {
      "id": "no-console-print",
      "message": "Use the logger instead of printing.",
      "pattern": "\\bprintln\\(|System\\.(out|err)",
      "extensions": ["kt", "java"],
      "allowedIn": []
    },
    {
      "id": "no-literal-text",
      "message": "User-facing text must use Tr.tr(key) with a key in the lang file.",
      "pattern": "Component\\.literal\\(",
      "extensions": ["kt", "java"],
      "allowedIn": ["i18n"]
    },
    {
      "id": "no-blocking",
      "message": "Never block threads. Use the core scheduler or async executors.",
      "pattern": "Thread\\.sleep\\(|\\brunBlocking\\b",
      "extensions": ["kt", "java"],
      "allowedIn": []
    }
  ]
}
```

2. An architecture test reads that file and fails the build on a violation.
3. A `PreToolUse` hook (edit guard) blocks the AI before it writes the offending line, with the rule
   message as feedback.
4. A `Stop` hook verifies `<CHECK>` is green and `docs/PROGRESS.md` was updated.

Suggested rule ids to encode: no console print, no literal user-facing text, no hardcoded colors, network
only in the http client, no blocking, no raw threads.

## 11. Docs map

| Doc | Content |
|---|---|
| `docs/PROGRESS.md` | Status, roadmap, next steps, open questions |
| `docs/ARCHITECTURE.md` | Packages, module system, events, state, rendering |
| `docs/CONVENTIONS.md` | Code style, naming, performance rules, commits |
| `docs/I18N.md` | Translation keys and text rules |
| `docs/RULES.md` | Server rules: allowed, gray, forbidden |
| `docs/TESTING.md` | Unit tests, fixtures, running against a server |
| `docs/DECISIONS.md` | Architecture decision log |
| `HANDOFF.md` | Current task state between agents |

## 12. Feature checklist

Use the same shape for every feature:

```
feature/<category>/<feature>/
  <Feature>Module.kt      Lifecycle + wiring only
  <Feature>Config.kt      ConfigSection object; the on/off toggle is automatic
  <Feature>Logic.kt       Pure logic (parsing/calculations), no engine calls -> unit-testable
  <Feature>Hud.kt         Only if it draws something
src/test/kotlin/.../<Feature>LogicTest.kt
src/main/resources/assets/<mod>/lang/en_us.json   New keys
```

Done when:

- [ ] Feature classified in `docs/RULES.md` (allowed / gray / forbidden).
- [ ] Logic unit test with fixtures passes.
- [ ] `<CHECK>` green.
- [ ] Keys added to the lang file.
- [ ] Tested in game (or listed under "needs in-game test" in `docs/PROGRESS.md`).
- [ ] `docs/PROGRESS.md` updated. Suggested commit: `feat(<category>): add <feature>`.

## 13. Anti-patterns

| Anti-pattern | Why it is banned |
|---|---|
| Literal text in a message/tooltip | Breaks translation and the i18n test |
| Hex color literals | Palette drifts; theme tokens are the single source |
| A feature importing another feature | Coupling; features must stay independent |
| `Thread.sleep` / `runBlocking` in game code | Freezes the client thread |
| New `Thread` / executor per feature | Unbounded threads, no shared pool |
| Registering listeners outside the module context | Disabled feature still runs |
| Regex in a tick/render path without a prefilter | Allocations and CPU spikes |
| Recomputing derived data every frame | Wasted CPU; compute on change |
| Commented-out code | Dead weight, rots out of date |
| Big commit mixing refactor + feature | Unreviewable, hard to revert |
| Claiming a green build that was not run | Trust breaks immediately |
