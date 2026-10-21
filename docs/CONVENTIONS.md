# Conventions

## Language
- Code, identifiers, comments, KDoc, docs, commit messages: **English**.
- User-facing text: translation keys only (`docs/I18N.md`).

## Kotlin style
- Formatting is owned by ktlint through Spotless (`./gradlew spotlessApply`). Do not hand-format.
- Names: `PascalCase` types, `camelCase` functions/properties, `SCREAMING_SNAKE_CASE` constants,
  packages lowercase without underscores. Feature folders: `feature/<category>/<feature>/`.
- One public top-level type per file; file named after it.
- Prefer `val`, immutable data, `private`/`internal` by default. Expose the smallest API possible.
- KDoc on every public type and on any non-obvious function. Explain **why**, not what.
- Spacing and rhythm come from `ui.theme.Spacing`; only the size of a widget itself belongs to the widget.
- No commented-out code, no TODO without context (`TODO(phase-2): reason`).
- Java is only for mixins (`mixin/`); keep them to a guard check plus a call into Kotlin.

## Reuse
Before writing anything, search for an existing helper. If the same kind of code would appear twice,
create a shared component first:
| Need | Owner |
|---|---|
| Colors, spacing, fonts | `ui.theme` |
| Widgets | `ui.component` |
| Drawing primitives, cached text | `ui.render` |
| Key bindings | `core.keybind`: `ctx.keyBind(bind) { }` in modules, never a vanilla `KeyMapping` |
| HUD overlays | `ui.hud` |
| HTTP / external data | `api.http`, `api.repo` |
| Hypixel server data | `api.hypixel` |
| Game state (island, profile...) | `skyblock` |
| Threads, delays, throttling | `core.scheduler` |
| Text/chat/item parsing, number format | `util`: `ChatPattern` (prefilter + regex), `Numbers`, `LegacyText`, `RarityLine`, `SkyBlockItemData` |
| Price lines (the price of an item as rows) | `util.PriceLines` (shared: two features cannot import each other) |
| User-facing text | `i18n.Tr` |
| Gradient text | `Component.withGradient(Theme.X_GRADIENT)` |
| Chat messages | `ui.chat.Chat` (info/success/warning/error) |
| Commands | `ctx.command { }` in modules, `Skyatlaz.COMMANDS` in core (never Fabric/Brigadier directly) |
| Settings | `ConfigSection` objects (`override val config` on the module), `ctx.watch(entry)` to react |

## Performance
The mod will run many modules every tick; every rule here matters.
- **Disabled = zero cost.** Register listeners in `onEnable` through the module context only.
- **Hot paths** (render, tick, chat, packet handlers): no allocations, no `String.format`, no regex without a
  cheap prefilter (`startsWith`/`contains` before `Regex`), no collection operator chains
  (`map/filter` allocate), no boxing. Use plain loops and preallocated structures.
- **Compute on change, not per frame.** Cache derived values; invalidate through events.
- **Compile regexes once** (`private val PATTERN = Regex(...)` at object level).
- **Never block the client thread.** I/O and network are async through `core.scheduler`; never `Thread.sleep`
  or `runBlocking`.
- Lookups: `HashMap`/arrays keyed by stable ids; avoid linear scans over item/entity lists each tick.
- Throttle expensive checks (e.g. every 20 ticks) via the scheduler instead of counting ticks by hand.
- `inline` small higher-order helpers used in hot paths; `@JvmField` for constants read from Java mixins.
- Measure before optimizing bigger changes (Spark profiler / per-module timing).

## Tests
- Every parser, calculator and state transition has a unit test. Tests must stay fast (whole suite < 10 s).
- Fixtures (real chat lines, scoreboard/tab snapshots, item data) live in `src/test/resources/fixtures/`.
- See `docs/TESTING.md`.

## Commits
Conventional Commits, enforced by `.githooks/commit-msg`:
```
<type>(<scope>): <lowercase description, max 72 chars>
```
Types: `feat`, `fix`, `perf`, `refactor`, `docs`, `test`, `build`, `ci`, `chore`, `style`, `revert`.
Scope is the area: `core`, `event`, `config`, `hud`, `i18n`, `api`, `dungeons`, `mining`...
One logical change per commit. Body (optional) explains why.

## Branches
`main` is always green. Work on `feat/<name>`, `fix/<name>`; merge when `./gradlew check` passes.
