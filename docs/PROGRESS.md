# Progress / handoff

Update this file at the end of every session: what changed, what is next, open questions.

## Current phase: 0 - Environment (done), next: 1 - Core

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
1. Phase 1, starting with `core.event` and `core.module` (see `docs/ARCHITECTURE.md`).
2. Verify `./gradlew runClient` launches 26.2 with the Hypixel Mod API mod, and test DevAuth.

## Open questions for the owner
- Optional extras not yet confirmed: remote kill-switch, per-module profiling overlay, config migrations,
  update checker, release pipeline (Modrinth/GitHub).
- Config screen: own UI vs YACL behind an adapter.
- GitHub repository for CI.

## Session log
- 2026-10-19 (Opus 5.5): Phase 0 setup. Fixed template build error (`project.property` nullable types).
  Moved target from 26.3 to 26.2 (see DECISIONS). Added rules, hooks, docs, skills, reference repos.
