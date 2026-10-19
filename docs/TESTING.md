# Testing

## Strategy
Hypixel cannot run locally, so test in layers and go to Hypixel as late as possible:

| Layer | What it proves | Speed | Share of testing |
|---|---|---|---|
| 1. Unit tests + fixtures | Parsers, calculators, state machines are correct | ms | Most of it |
| 2. Offline sandbox (singleplayer + injected state) | Rendering, layout, HUD editor, screen overlays, module on/off | seconds, hot reload | UI work |
| 3. Hypixel | Real data, timing, edge cases we did not capture | minutes | Capture fixtures + final check |

Loop for a feature (e.g. a HOTM overlay): open the real menu on Hypixel once, dump it (layer 3), turn the dump into
fixtures + parser tests (layer 1), iterate the overlay offline against the same dump (layer 2), confirm on Hypixel.
This only works because features read **shared SkyBlock state and core events**, never Minecraft directly: the
sandbox feeds the same state from fixtures.

## 1. Fast tests (always)
`./gradlew test` (part of `./gradlew check`). Must stay under ~10 s.
- **Architecture tests** (`architecture/ArchitectureTest`): rules from `config/source-rules.json` + structure rules.
- **i18n tests** (`i18n/TranslationKeysTest`): used keys exist, keys are namespaced and unique.
- **Unit tests**: one per parser/calculator/state machine. Keep logic out of Minecraft classes so it can be
  tested with plain inputs.

## 2. Fixtures from the real game [planned: core/debug]
1. In game, run `/skyatlaz debug dump <name>`: writes current scoreboard, tab list, recent chat, held item and
   the **open container** (title, slots, item names, lore, components) to `run/skyatlaz-dumps/<name>.json`.
2. Copy useful dumps to `src/test/resources/fixtures/<area>/` and write a test against them.
3. Every bug found in game becomes a fixture + failing test first, then the fix.

## Run tasks
All inherit the client run (DevAuth included). IntelliJ gets matching run configs after a Gradle sync.
| Task | Starts at |
|---|---|
| `./gradlew runClient` | Title screen |
| `./gradlew runWorld` | Singleplayer world `run/saves/Development` (override: `skyatlaz.dev.world=<folder>` in `~/.gradle/gradle.properties`) |
| `./gradlew runHypixel` | Joined to `play.hypixel.net` |

## Agent access to the dev world (MCP Fabric)
An AI agent can see and drive the **singleplayer dev world** through [MCP Fabric](https://github.com/denfry/mcpfabric)
(MIT): screenshots, chat/commands, open-screen widgets and clicks.
- **Only `runWorld` loads it** (`fabric.addMods=run/agent-mods`, opt-in `skyatlaz.agentMods=true` in
  `~/.gradle/gradle.properties`). `runClient` and `runHypixel` never do: it automates input, which is forbidden on
  Hypixel (`docs/HYPIXEL_RULES.md`).
- Setup per machine:
  1. Put `mcpfabric-<version>+26.2.jar` from Modrinth in `run/agent-mods/` (verify the sha512).
  2. Run `./gradlew runWorld` once; it writes `run/config/mcpfabric.config.json`. Set `enableWorldWrite` to
     `false` and `enableUiControl` to `true`; keep `host` = `127.0.0.1` and `requireAuth` = `true`.
  3. MCP server (outside the repo): clone `denfry/mcpfabric` at the matching tag, `cd mcp-server`,
     `npm ci && npm run build`, then
     `claude mcp add mcpfabric --scope local -e MCPFABRIC_TOKEN=<token> -- node <path>/mcp-server/dist/index.js`.
     The token stays in your local Claude config, never in the repo.
- Without the MCP server, `node tools/agent/bridge.mjs chat "/skyatlaz"` and
  `node tools/agent/bridge.mjs screenshot out.png` talk to the bridge directly.

## 3. Offline sandbox [planned: core/debug]
`./gradlew runWorld`, then load a dump instead of joining Hypixel:
- `/skyatlaz debug load <name>`: pushes the dump's scoreboard/tab/location into SkyBlock state (fires the same
  change events as on Hypixel).
- `/skyatlaz debug chat <text>`: injects a chat line through the normal chat pipeline.
- `/skyatlaz debug open <name>`: opens a local container screen filled with the dumped items, so container
  overlays (HOTM tree, menus, solvers) render exactly as on Hypixel.

Debug commands are only registered in a dev environment or with debug mode on. Later, the Fabric client gametest
API (already on the dev classpath) can automate these scenarios.

## 4. Running on Hypixel
- `./gradlew runClient` (or the "Minecraft Client" run config in IntelliJ).
- The Hypixel Mod API Fabric mod is added to the dev runtime automatically.
- **Login (DevAuth):** opt in per machine in your user-level `~/.gradle/gradle.properties` (never the repo):
  ```
  skyatlaz.devauth=true
  # optional, default "main" (accounts are defined in ~/.devauth/config.toml)
  skyatlaz.devauth.account=alt
  ```
  The build then adds DevAuth and sets `-Ddevauth.enabled` / `-Ddevauth.account` on the client run. The first
  login for an account opens the Microsoft login in the browser; tokens are cached in `~/.devauth/`.
  Verified working with DevAuth 1.2.2 on 26.2.
- Prefer an **alt account** for testing new features.
- Hot reload: use JetBrains Runtime 25 as the project JDK and debug mode; method body changes apply without restart.

## 5. Performance checks
- Spark profiler mod in dev runtime for sampling sessions (when needed).
- [planned] per-module timing overlay in debug mode.
