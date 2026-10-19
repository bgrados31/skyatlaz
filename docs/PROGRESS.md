# Progress / handoff

Update this file at the end of every session: what changed, what is next, open questions.

## Current phase: 1 - Core (in progress: event, module, scheduler, error, command done; theme/chat pulled forward)

## Roadmap
- [x] **Phase 0 - Environment**: Gradle/Loom/Kotlin on MC 26.2, single client source set, Spotless (ktlint),
  architecture + i18n tests, shared rules file, Claude Code hooks, git hooks, CI, docs, skills, reference repos.
- [ ] **Phase 1 - Core**: `core.module` (Module, ModuleManager, ModuleContext with auto-unregister),
  `core.event` (EventBus with hasListeners), `core.scheduler`, `core.config` (typed model + persistence +
  migrations), `core.error` (module isolation), `core.debug` (commands, fixture dump), first mixins for
  tick/chat/render hooks. Unit tests for all of it.
- [ ] **Phase 2 - SkyBlock state**: `api.hypixel` (Mod API wrapper), scoreboard/tab/chat parsers,
  `skyblock` state + change events, `util` (ChatMatcher, ItemParser, NumberFormat), `api.http` + `api.repo`.
- [ ] **Phase 3 - UI base**: `ui.theme` tokens, `ui.hud` system + editor, `ui.component`, config screen.
- [ ] **Phase 4 - First features** (vertical slice) to validate the base, then grow by category.

## Next steps
1. Phase 1 next: `core.config` (toggles drive `ModuleManager.setEnabled`; decide own UI vs YACL first),
   `core.debug` (dump / load / chat / open for the offline sandbox), first mixins (chat, render hooks).
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
