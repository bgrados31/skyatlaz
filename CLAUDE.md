# skyatlaz

All-in-one client mod for Hypixel SkyBlock. Kotlin, Fabric, Minecraft 26.2 (unobfuscated, Mojang names), Java 25.
Private project (All Rights Reserved), author `tnozamas`, package `org.tnozamas.skyatlaz`.

## Start of every session
1. Read `docs/PROGRESS.md` (current phase, next steps, open questions). It is the handoff between sessions/models.
2. Read the doc for the area you touch (map below). Do not guess architecture; follow it.

## Commands
- `./gradlew check` - format check, compile, unit + architecture tests. Must be green before finishing.
- `./gradlew spotlessApply` - auto-format (ktlint for Kotlin, palantir for Java mixins).
- `./gradlew runClient` / `runWorld` / `runHypixel` - launch the game at the title screen / dev world / Hypixel
  (see `docs/TESTING.md`).
- `node tools/refs/sync.mjs` - clone/update reference mods into `references/` (git-ignored).
- `node tools/agent/bridge.mjs cmd skyatlaz | screenshot out.png` - drive/see the `runWorld` dev world (agents;
  see `docs/TESTING.md`). Never against Hypixel.

## Non-negotiable rules
Enforced automatically (`config/source-rules.json` + `ArchitectureTest` + `.claude/hooks`). Do not work around them.
- **English only** in code, comments, docs and commits. Talk to the user in Spanish.
- **No hardcoded user-facing text.** Use `Tr.tr("key")` and add the key to `en_us.json`. See `docs/I18N.md`.
- **Reuse before you write.** Colors/spacing from `ui.theme`, HTTP from `api.http`, threads from `core.scheduler`,
  text parsing from `util`. If something will be used twice, make it a shared component first.
- **Features never import other features.** They talk through core events and shared SkyBlock state.
- **A disabled module costs nothing:** no listeners registered, no data loaded, no per-tick work.
- **Performance first:** no allocations in render/tick hot paths, no blocking calls on the client thread,
  compute on change instead of every frame. See `docs/CONVENTIONS.md#performance`.
- **Hypixel rules:** classify every feature in `docs/HYPIXEL_RULES.md` before writing it. Never automate input,
  modify packets, or give movement/vision advantages.
- **Java only for mixins**, and mixins only forward to Kotlin (no logic).
- **`references/` is read-only study material.** Learn the approach, never copy code (license).

## Workflow
- New feature: use the `new-feature` skill. Looking at how other mods solve something: use the `skyblock-refs` skill.
- Library/API docs (Fabric API, Loom, Mixin, Kotlin): use Context7 instead of memory.
- Every logic change ships with a fast unit test (parsers get fixture-based tests).
- Commits: Conventional Commits, enforced by `.githooks/commit-msg` (e.g. `feat(dungeons): add score hud`).
  One logical change per commit. Commit only when the user asks or at the end of an agreed task.
- Before ending: `./gradlew check` green and `docs/PROGRESS.md` updated (the Stop hook enforces both).
- Record any non-obvious decision in `docs/DECISIONS.md`.

## Docs map
| Doc | Content |
|---|---|
| `docs/PROGRESS.md` | Status, roadmap, next steps, open questions |
| `docs/ARCHITECTURE.md` | Packages, module system, events, state, rendering |
| `docs/CONVENTIONS.md` | Code style, naming, performance rules, commits |
| `docs/I18N.md` | Translation keys and text rules |
| `docs/HYPIXEL_RULES.md` | What is allowed, gray, forbidden |
| `docs/TESTING.md` | Unit tests, fixtures, running on Hypixel |
| `docs/DECISIONS.md` | Architecture decision log |
