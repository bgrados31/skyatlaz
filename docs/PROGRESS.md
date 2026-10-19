# Progress / handoff

Update this file at the end of every session: what changed, what is next, open questions.

## Current phase: 1 - Core (done), next: 2 - SkyBlock state

## Roadmap
- [x] **Phase 0 - Environment**: Gradle/Loom/Kotlin on MC 26.2, single client source set, Spotless (ktlint),
  architecture + i18n tests, shared rules file, Claude Code hooks, git hooks, CI, docs, skills, reference repos.
- [x] **Phase 1 - Core**: `core.module` (Module, ModuleManager, ModuleContext with auto-unregister),
  `core.event` (EventBus with hasListeners), `core.scheduler`, `core.config` (typed model + persistence +
  migrations), `core.error` (module isolation), `core.debug` (commands, fixture dump), first mixins for
  tick/chat/render hooks. Unit tests for all of it.
- [ ] **Phase 2 - SkyBlock state**: `api.hypixel` (Mod API wrapper), scoreboard/tab/chat parsers,
  `skyblock` state + change events, `util` (ChatMatcher, ItemParser, NumberFormat), `api.http` + `api.repo`.
- [ ] **Phase 3 - UI base**: `ui.theme` tokens, `ui.hud` system + editor, `ui.component`, config screen.
- [ ] **Phase 4 - First features** (vertical slice) to validate the base, then grow by category.

## Next steps
1. Phase 2 (`api.hypixel`, `util`, `api.http`, `api.repo` prices done): parsers on `Snapshot` fixtures,
   `skyblock` state + change events, `debug load`. Capture real dumps on Hypixel first (hub, dungeon, mines, HOTM).
   Render hooks arrive with `ui.hud` (phase 3). Config screen: own UI recommended vs YACL adapter (phase 3).
2. UI sounds (amethyst chimes at musical pitches, cut short via the scheduler, volume + toggle) for command and
   chat feedback. Command palette screen (Ctrl+K style, fuzzy search) in phase 3, fed by `CommandRegistry`.
3. Owner: build and register the MCP Fabric server (docs/TESTING.md "Agent access"); the jar and config are
   already in place. Until then agents use `node tools/agent/bridge.mjs`.
4. Hot reload: install JetBrains Runtime 25 and add `-XX:+AllowEnhancedClassRedefinition` + Mixin agent to runs.
5. Client gametests (fabric-client-gametest-api, screenshots) with `core.debug`.

## Open questions for the owner
- Optional extras not yet confirmed: remote kill-switch, per-module profiling overlay, config migrations,
  update checker, release pipeline (Modrinth/GitHub).
- Config screen: own UI vs YACL behind an adapter.
- GitHub repository for CI.
- Lowest BIN: Tricked lowestbins enabled with README credit (DECISIONS). Optional: ask EliteSkyblock for averages.

## Session log
- 2026-10-19 (Opus 5.5): Phase 0 setup. Fixed template build error (`project.property` nullable types).
  Moved target from 26.3 to 26.2 (see DECISIONS). Added rules, hooks, docs, skills, reference repos.
- 2026-10-19 (Opus 5.5): DevAuth wired into the client run (opt-in via user gradle.properties, default account
  `main`); verified login on 26.2. Documented the layered testing strategy (unit + fixtures, offline sandbox,
  Hypixel last) in TESTING/DECISIONS.
- 2026-10-19 (Opus 5.5): Quick play runs (`runWorld`, `runHypixel`). Phase 1 started: `core.event` (EventBus with
  priorities, hasListeners, per-listener error routing) and `core.module` (Module, ModuleContext auto-unregister,
  ModuleManager with activation conditions and failure isolation), 17 unit tests.
- 2026-10-19 (Opus 5.5): `core.scheduler` (later/every/async, ModuleContext helpers), `ui.theme` (Theme tokens,
  OKLab Gradient), `withGradient`, `ui.chat.Chat`, module failure chat notice, `/skyatlaz debug theme`. 48 tests.
- 2026-10-19 (Opus 5.5): `core.command` (declarative registry, generated clickable help, categories, module
  commands via `ctx.command`, debug-only commands), `ModuleServices`. MCP Fabric bridge in `runWorld` only;
  verified `/skyatlaz`, `/skyatlaz debug`, `debug theme`, `version` in game via screenshots. 62 tests.
- 2026-10-19 (Opus 5.5): Chat redesign: soft palette, `SkyAtlaz › icon text` messages, `ChatLayout` (rules,
  pixel-aligned columns), help with breadcrumb title, relative names, rich hover, click to open/run/type.
  Game now runs on the RTX 4050 (Windows GPU preference for the toolchain java.exe).
- 2026-10-19 (Opus 5.5): Semantic gradients deepen within their hue; icons (ⓘ ✔ ⚠ ✖, category icons, hover
  action icons, ─ rule); runWorld window 1280x720 for legible agent screenshots.
- 2026-10-19 (Opus 5.5): Help as a tree (├ └ connectors, ✧ entries, deeper indent and path header on subpages,
  constant brand title); no category or message icons; semantic gradients deep -> soft; removed debug glyphs.
- 2026-10-19 (Opus 5.5): Help redesigned without decoration: typography hierarchy, blank lines, page header and
  back link on subpages, plain hover (usage, description, subcommands, click hint).
- 2026-10-19 (Opus 5.5): `core.config` (typed entries, sections, flat versioned JSON, migrations, corrupt-file
  recovery, debounced atomic saves, shutdown save), module toggles in config, `ctx.watch`, `GeneralConfig.debugMode`
  gates DEBUG commands. 75 tests.
- 2026-10-19 (Opus 5.5): Phase 1 done. `GameHooks` (chat/tick/join/leave events via Fabric API),
  `util.LegacyText`, tab accessor mixin, `core.debug` (dump, chat injection, offline container open, chat
  recorder only in debug mode). Verified in game: injected chat recorded with codes, item + container round trip
  exact. 82 tests.
- 2026-10-19 (Opus 5.5): `api.hypixel` (Mod API wrapper: location events, throttled party requests, session state
  cleared on disconnect), `core.scheduler.Cooldown`, `/skyatlaz debug hypixel`. 90 tests. Needs a check on Hypixel.
- 2026-10-19 (Opus 5.5): `util` (Numbers, ChatPattern + ctx.onChat, RarityLine, SkyBlockItemData) and `api.http`
  (HttpService with disk cache, ETag/304, stale fallback, dedupe; JdkHttpTransport with gzip). Verified in game:
  bazaar 3.6M chars from network in 677 ms, then from cache in 69 ms. 131 tests.
- 2026-10-19 (Opus 5.5): `api.repo` PriceRepository (official keyless bazaar + item catalog, streaming parsers, loaded
  only while used, refresh 60 s / 6 h, PricesUpdatedEvent, value() bazaar -> NPC), `ctx.usePrices()`,
  `/skyatlaz debug price`. Verified with live data. 137 tests.
