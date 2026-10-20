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

## 2. Fixtures from the real game
1. In game (debug mode on), run `/skyatlaz debug dump <name>`: writes sidebar title and lines, tab list with header
   and footer, the last 100 chat lines, the held item and the **open container** (title, rows, slots with id, name,
   lore and `custom_data` SNBT) to `run/skyatlaz/dumps/<name>.json`. Texts keep legacy `§` codes.
   Tests read it with `Snapshot.fromJson(...)` (pure Kotlin, no game needed).
2. Copy useful dumps to `src/test/resources/fixtures/<area>/` and write a test against them.
3. Every bug found in game becomes a fixture + failing test first, then the fix.

**When the state only exists in an open inventory** (the Heart of the Mountain tree), the chat is blocked and the
command cannot be typed. Press **F7** instead (the capture binding, which fires with a screen open): it saves the same
dump, named after the title of the container (`Heart of the Mountain` -> `heart_of_the_mountain`) or, with no container
open, after the area. The binding can be changed in the "Controls" tab.

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
- Without the MCP server, `node tools/agent/bridge.mjs cmd skyatlaz` and
  `node tools/agent/bridge.mjs screenshot out.png` talk to the bridge directly.

## 3. Offline sandbox
`./gradlew runWorld`, then replay Hypixel data instead of joining:
- `/skyatlaz debug chat <text>`: injects a server message through the normal receive pipeline (`ChatReceivedEvent`
  fires, listeners can cancel it). `&` color codes, e.g. `&6&lLEGENDARY &eDrop`.
- [done] `/skyatlaz debug load <name>`: pushes the dump's scoreboard/tab/location into SkyBlock state
  (fires the same change events as on Hypixel and pauses live updates until the world changes).
- [done] `/skyatlaz debug item <id>`: gives the player an item with SkyBlock data (`custom_data.id`) and opens the
  inventory, so the item features (prices in the tooltip) can be tried offline. It also loads the `hub` dump when the
  world is not on SkyBlock: the features that need SkyBlock data (the prices) only run on SkyBlock, so without it the
  tooltip would show nothing. Copy `src/test/resources/fixtures/skyblock/hub.json` to `run/skyatlaz/dumps/` once if
  that dump is missing.
- `/skyatlaz debug state`: prints the parsed state, to compare a loaded dump or the live game with expectations.
- `/skyatlaz debug neu <id>`: fetches one NEU-REPO item page and prints its crafting/forge recipes (network + cache).
- `/skyatlaz debug open <name>`: opens a local container screen filled with the dumped items, so container
  overlays (HOTM tree, menus, solvers) render exactly as on Hypixel.
- [done] `/skyatlaz config`: opens the real settings screen (tabs from registered sections + module toggles,
  search, Reset, Done/Esc, auto-save).
- [done] `/skyatlaz hud`: opens the HUD editor (drag, scroll to scale, `R` reset, `Tab` select, `Esc` close).
  With debug mode on, a SkyBlock state card is registered so the editor has an element to manipulate; its
  layout is saved as `hud.debug.state.x/.y/.scale` and can also be set by hand to check a specific position or
  scale before launching.

Debug commands exist only with debug mode on (`general.debug_mode`, default on in dev). Later, the Fabric client gametest
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
- Laptops with hybrid graphics: Windows may run the game on the integrated GPU. Set the toolchain `java.exe`
  to "High performance" (Settings › Display › Graphics); the log line `using drivers: ... NVIDIA` confirms it.
- Hot reload: use JetBrains Runtime 25 as the project JDK and debug mode; method body changes apply without restart.

## 5. Performance checks
- Spark profiler mod in dev runtime for sampling sessions (when needed).
- [planned] per-module timing overlay in debug mode.
