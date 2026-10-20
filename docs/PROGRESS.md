# Progress / handoff

Update this file at the end of every session: what changed, what is next, open questions.

## Current phase: 4 - First features (first vertical slice done)

## Where we left off (2026-10-20)
- Command palette: `ui/palette` opens with Ctrl+K (a key binding, rebindable in the "Controls" tab) and shows every
  command of the mod in a fuzzy search: type a few letters and the commands that match appear, best first. Enter runs
  the selected one; a command that needs an argument is typed into the chat instead. The search is pure
  (`CommandPaletteLogic`, 8 tests) and the screen only reads the command tree, so it also shows the commands a module
  adds while it runs.
- Third vertical slice: `feature/economy/heldprice` shows the item in the player's hand and what it is worth, as a
  card on the HUD (bazaar instant sell, lowest BIN and NPC price, each one can be turned off). It reads the shared
  price repository and the shared price lines (`util.PriceLines`, moved out of the tooltip feature because two
  features cannot import each other), and the hand is watched once per tick by reference, so the card follows the
  player switching items without doing work per frame. 200 tests, all green.
- Second vertical slice: `feature/economy/itemprice` shows the price of an item **in its tooltip** (bazaar instant
  sell, lowest BIN and NPC price, each one can be turned off). It reads the shared price repository, so no new
  download logic; the lines are added by a mixin on `Screen.getTooltipFromItem` and the registry is
  `core.tooltip.TooltipLines`. 195 tests, all green.
- New dev tool: `/skyatlaz debug item <id>` gives the player an item with SkyBlock data and opens the inventory, so
  the item features can be tried offline. It also loads the `hub` dump when the world is not on SkyBlock (the price
  module only runs on SkyBlock), and the chat says when the prices arrive.
- The base is closed: `ui.theme.Spacing` owns the rhythm of every layout (the widget kit, the HUD cards and the
  settings screen take their gaps from it), `ui.render` has the shared drawing primitives (`Shapes`, `CachedText`)
  and `core.keybind` is a full key binding system (registry, persistence in `keybind.<id>.key`, conflicts, one poll
  per tick, rebinding in the settings screen). 189 tests, all green.
- Key bindings are ours, not the vanilla `KeyMapping`: a binding only exists while its feature runs, fires on the
  press and never while a screen is open. The settings screen grew a "Controls" tab (the search field moved to the
  title row, because the tab bar could not hold four tabs next to it).
- Phase 4 started with its first vertical slice: `feature/skyblock/statushud`, a HUD card with area, profile,
  level and money. It is a real module (config toggle, settings, activation condition "on SkyBlock"), it reads the
  shared `skyblock` state and its rows are built by a pure `StatusHudLogic` (7 unit tests). 175 tests, all green.
- The UI sandbox is gone: `ui/prototype` and `/skyatlaz debug ui` were deleted with the owner's confirmation, and
  the `skyatlaz.prototype.*` keys with them. The real config screen is the only consumer of `ui.component`.
- `ModuleManager.refreshActivation()` is now called for real (the entrypoint wires `SkyBlockStateChangeEvent` to it),
  so a module with an activation condition actually starts and stops with the shared state. It was never called
  outside the tests before.
- Phase 2 is complete for the captured sources: `api.hypixel` (Mod API session), `util`, `api.http`, `api.repo`
  (bazaar, item catalog, lowest BIN via Tricked, NEU-REPO item pages and recipes on demand), `skyblock`
  (`SkyBlockTracker`: area, sub-area, profile, server, purse, bits, bank, level; `AreaChangeEvent` /
  `SkyBlockStateChangeEvent`), `debug load` / `debug state` / `debug neu`.
- Phase 3 core is done: `ui.hud.HudManager` (one Fabric hook for every element), `HudElement`, fraction-based
  `HudPlacement` persisted in `hud.<id>.x/.y/.scale`, `/skyatlaz hud` editor (drag, scroll, `R`, `Tab`, `Esc`),
  and `core.debug.DebugHudPreview` (a SkyBlock state card shown only in debug mode).
- The real config screen is done: `/skyatlaz config` opens `ui.config.ConfigScreen`, fed by
  `ConfigManager.registeredSections` and `ModuleManager.toggles`. Tabs group sections by top-level id, module
  toggles get a Modules tab, `hud.*` stays in the HUD editor, search/no-results and Reset (active tab) work, and
  edits use the existing debounced save. 168 tests, all green.
- Verified in `runWorld`: HUD card at default position and at `x = 0.50`, `y = 0.30`, `scale = 1.50`; editor
  dims the world, outlines the element, shows its label, position/scale readout and hints above the hotbar.
- Verified the real config screen in `runWorld` at the 427x240 logical size: `/skyatlaz config`, search and
  no-results, toggle by click and by Tab/Space, Reset restoring the active tab, Done, Esc, and the saved JSON.
  Screenshots in `run/verify/` (git-ignored).
- Real dumps captured and added as fixtures: `hub`, `island`, `mines` (`src/test/resources/fixtures/skyblock/`).
- Still wanted from the owner: `/skyatlaz debug hypixel` output (Mod API location + party) and dumps for
  `hotm` (menu open) and `dungeon` (mid-run), ideally `hollows`, `garden` and a party. The `hotm` one is the tree
  of the Heart of the Mountain, an inventory: open it and press F7 (the capture binding, which works with a screen
  open) to save it as `heart_of_the_mountain`.
- Owner preferences learned: talk in Spanish; UI must be calm and typographic (soft palette, no decorative icons,
  blank-line spacing; see DECISIONS "Gradient identity"); verify UI in game with screenshots before committing.

## Roadmap
- [x] **Phase 0 - Environment**: Gradle/Loom/Kotlin on MC 26.2, single client source set, Spotless (ktlint),
  architecture + i18n tests, shared rules file, Claude Code hooks, git hooks, CI, docs, skills, reference repos.
- [x] **Phase 1 - Core**: `core.module` (Module, ModuleManager, ModuleContext with auto-unregister),
  `core.event` (EventBus with hasListeners), `core.scheduler`, `core.config` (typed model + persistence +
  migrations), `core.error` (module isolation), `core.debug` (commands, fixture dump), first mixins for
  tick/chat/render hooks. Unit tests for all of it.
- [ ] **Phase 2 - SkyBlock state**: [done] `api.hypixel`, `util`, `api.http`, `api.repo` prices + NEU-REPO item
  pages/recipes, scoreboard/tab parsers + `skyblock` state + `debug load`. [done] the perks of the HOTM read from the
  lore of its menu (`feature/mining/hotm`). [todo] dungeon-specific state, chat-driven state.
- [x] **Phase 3 - UI base**: `ui.theme` tokens, `ui.hud` system + editor, `ui.component` engine (chosen for the
  config screen), real `ui.config.ConfigScreen` backed by `core.config` + module toggles.
- [ ] **Phase 4 - First features**: [done] `skyblock/statushud` (the vertical slice that validates the stack),
  `economy/itemprice` (prices in the item tooltip) and `economy/heldprice` (the price of the item in hand).
  [done] `mining/hotm` (the mining tree of the player, read from the profile API).
  [todo] the next features, one category at a time.

## Next steps
1. Phase 4: the next vertical slices, now that the shape is proven (`feature/<category>/<feature>` with a config
   section, pure logic, HUD element and tests). Candidates: the dungeon score, purse/bank tracker.
   The mining card is built ([done]) but it needs the developer API key of the player in the settings
   (`general.api_key`); the dungeon state needs a dump taken after the run (the score arrives at the end).
2. UI sounds (amethyst chimes at musical pitches, cut short via the scheduler, volume + toggle) for command and
   chat feedback. The command palette is done (`ui/palette`, Ctrl+K).
3. Owner: build and register the MCP Fabric server (docs/TESTING.md "Agent access"); the jar and config are
   already in place. Until then agents use `node tools/agent/bridge.mjs`.
4. Hot reload: install JetBrains Runtime 25 and add `-XX:+AllowEnhancedClassRedefinition` + Mixin agent to runs.
5. Client gametests (fabric-client-gametest-api, screenshots) with `core.debug`.

## Open questions for the owner
- Optional extras not yet confirmed: remote kill-switch, per-module profiling overlay, config migrations,
  update checker, release pipeline (Modrinth/GitHub).
- Config screen first pass is implemented as all non-HUD registered sections plus module toggles; confirm the owner
  is happy with that scope before adding feature sections whose options arrive later.
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
- 2026-10-19 (Opus 5.5): Lowest BIN from Tricked lowestbins (only provider with stated terms: credit, given in
  README); value order bazaar > auction > NPC. Checked providers: Moulberry down, EliteSkyblock and hysky.de public
  without terms. 140 tests. Ignored stray `logs/`.
- 2026-10-19: Real dumps captured (hub, private island, Dwarven Mines) and turned into fixtures. `skyblock`:
  `SkyBlockTracker` + pure parser (area/sub-area/profile/server/purse/bits/bank/level), change events, 0.5 s
  content-compared capture, shared `SkyBlockReader` with dumps; `/skyatlaz debug load` and `debug state`. 149 tests.
  Verified in game (`runWorld`): loading `hub` reported `Hub / Village / Mango / mega24B / 638,769,221 coins`.
- 2026-10-19: `api.repo` NEU-REPO item pages on demand: `NeuRepository` fetches `items/<ID>.json` from the
  community data repo through the shared HTTP cache (128-entry LRU, 1 min negative cache), `NeuItem` parser
  (display name/lore stripped, vanilla id, crafting grids, forge inputs + duration), `/skyatlaz debug neu <id>`.
  Fixtures + tests for single grid, `recipes[]` crafting and forge. 159 tests. Not bundled; credited in README.
  Verified in game (`runWorld`): `debug neu HYPERION` showed the 8 lasers + handle; `AMBER_MATERIAL` showed forge
  inputs and 21600 s; an unknown id answered with the warning. Also smoke-parsed all ~8,900 local NEU pages.
- 2026-10-19: Phase 3 HUD base: `ui.hud` (`HudElement`, `HudManager`, `HudPlacement`, `HudEditorScreen`,
  `HudCommand`), `ctx.hud(element)`, per-element layout in `hud.<id>.x/.y/.scale`, debug-only SkyBlock state card.
  `Theme.argb`/`SCREEN_DIM` for GUI alpha. 165 tests. Verified in `runWorld` with screenshots at default and
  `x = 0.50`, `y = 0.30`, `scale = 1.50`; editor hides the normal HUD, dims the world and keeps hints above the
  hotbar.

- 2026-10-19: Phase 3 UI comparison: four prototype screens with identical mock content (YACL, Cloth Config,
  SpruceUI, owo-ui) plus a picker, verified in `runWorld` with screenshots in `run/ui-comparison/` (git-ignored).
  Yumi cannot be compared: `yumi-mc-foundation` 1.1.3+26.2 ships no UI/config classes. Loom injects owo's mixin
  interfaces into the shared Minecraft jar, so the prototypes add a temporary `testRuntimeOnly(owo-lib)` for
  plain JVM tests. 165 tests, `check` green. Owner decision on the library pending; see DECISIONS 2026-10-19.
- 2026-10-20: Phase 3 UI comparison completed with two more options: MoulConfig 4.7.2 (the NEU/SkyHanni/Firmament
  panel, `modern-26.2` from `maven.notenoughupdates.org`) and the mod's own engine. `ui.component` now exists as
  a real widget kit (panel, tabs, search, scrolling with scissor, focus/Tab, tooltips, toggle/slider/text/button,
  `UiScreen` base); `OwnPrototypeScreen` exercises it and `MoulConfigPrototypeScreen` builds MoulConfig categories
  and options by hand (no annotation processor). Both verified in `runWorld`; screenshots `5-own.png` and
  `6-moul.png`. `check` green with 165 tests. Owner decision still pending.
- 2026-10-20: Owner chose the own engine. Deleted the YACL/Cloth/SpruceUI/owo/MoulConfig prototypes and the
  picker, plus their Gradle repos, dependencies and `*_version` properties; `/skyatlaz debug ui` now opens the own
  sandbox directly. `check` green with 165 tests. Remaining work: real config screen backed by `core.config`.
- 2026-10-20: Fresh `runWorld` after the cleanup: 73 mods, none of the discarded UI libraries. Exercised the
  sandbox through the MCP bridge and Win32 messages (tabs, search/no results, wheel scroll, toggle, slider, text,
  reset, done, Esc). Found and fixed two issues: `UiList` now reveals a keyboard-focused row instead of leaving it
  off-screen, and an empty focused `UiTextField` hides its placeholder so the caret does not overlap it. Both
  re-verified in game; `check` green with 165 tests. Screenshots in `run/verify/` (git-ignored).
- 2026-10-20: Real config screen built on `ui.component`: `ConfigManager.registeredSections`, `ModuleManager.toggles`,
  `ui.config.ConfigScreen` + `ConfigCommand` (`/skyatlaz config`), `UiChoice` and a decimal-slider mapping, section
  title keys, and an empty-tab guard. Verified in `runWorld` with screenshots; `check` green with 168 tests.
  `ui/prototype` is still present (now unused by the real flow) and `debug ui` still opens it; deletion waits for
  owner confirmation.
- 2026-10-20: The owner confirmed the deletion of the UI sandbox: `ui/prototype` and `/skyatlaz debug ui` are gone
  (with the `skyatlaz.prototype.*` keys), so the real config screen is the only consumer of `ui.component`.
- 2026-10-20: Phase 4 starts with its first vertical slice, `feature/skyblock/statushud`: a real module (toggle,
  settings, activation condition "on SkyBlock") that shows area, profile, level, purse, bits and bank on the HUD.
  `StatusHudLogic` builds the rows (pure, 7 tests) and `StatusHud` translates and measures them only when they
  change. `ModuleManager.refreshActivation()` was never called outside the tests: the entrypoint now wires it to
  `SkyBlockStateChangeEvent`, so a module with a condition really starts and stops with the shared state.
  `FeatureCategory.SKYBLOCK` added. 175 tests, `check` green. Verified in `runWorld`: the card (with and without
  bits/bank), the "Status HUD" and "Modules" tabs of the config screen, the `UiChoice` number format (compact vs
  grouped, applied live), Reset, the module toggle removing the card, and the HUD editor listing both elements.
  The debug card moved to the top right corner so it no longer overlaps the player facing one.
- 2026-10-21: A key binding that works with a screen open (F7 by default) saves the state as a dump, so the
  states that only live in an open inventory (the Heart of the Mountain tree) can be captured without the chat, which
  is blocked while a container is open. The dump is named after the title of the container or, without one, after the
  area. Also fixed a crash: a message written before the game GUI exists (a key conflict found while starting) killed
  the client. 215 tests, `check` green.
- 2026-10-21: Third vertical slice, `economy/heldprice`: a card on the HUD with the item in the player's hand and
  what it is worth (bazaar instant sell, lowest BIN, NPC price, each one can be turned off). The hand is read once
  per client tick and compared by reference, so the card follows the player switching items and the prices are only
  looked up when the item changes. The price lines moved to `util.PriceLines` (two features cannot import each
  other) and their translation keys with them (`economy.price.*`), so the tooltip and the card say the same thing.
  New config tab "Economy" with the two price sections. 200 tests, `check` green. Verified in `runWorld`: the card
  (title + price rows, bottom right), the card following the item in hand, the config screen with the Economy tab
  and both sections, and the HUD editor listing the new element.
- 2026-10-21: Command palette (`ui/palette`): Ctrl+K opens a fuzzy search over every command of the mod. Type a
  few letters and the matches appear, best first (the letters only have to appear in order, in the name or in the
  description). Enter runs the selected command; a command that needs an argument is typed into the chat instead. The
  search is pure (`CommandPaletteLogic`, 8 tests) and the palette reads the command tree, so it also shows the
  commands a module adds while it runs. The key that opens it is dropped from the search field (it arrives as a
  character too). 211 tests, `check` green. Verified in `runWorld`: opening with Ctrl+K, the search, running a
  command (the settings screen opened) and typing a command with an argument into the chat.
- 2026-10-21: Dev tooling: `run/options.txt` got `pauseOnLostFocus:false` (the agent's screenshots arrived on a
  paused world) and `run/agent-input.ps1` sends Win32 messages (click, wheel, keys, typing) to the dev window;
  client pixels = logical x 2.4 in the current 1024x576 window.
- 2026-10-21: Base closed. `ui.theme.Spacing` (one rhythm for every surface), `ui.render` (`Shapes` with theme
  colors, `CachedText` that builds a component on change), and `core.keybind` (`KeyCombo`, `KeyBind`,
  `KeyBindManager`: registry, persistence, conflicts, one poll per tick). Features bind through
  `ctx.keyBind(bind) { }`; the player rebinds in the new "Controls" tab of `/skyatlaz config` (click a row, press
  a key, Escape cancels). The search field moved to the title row of the settings screen, because the tab bar could
  not hold four tabs next to it. 189 tests, `check` green.
- 2026-10-21: Second vertical slice, `economy/itemprice`: the price of an item in its tooltip (bazaar instant
  sell, lowest BIN, NPC price). The lines are added by a mixin on `Screen.getTooltipFromItem` and the registry is
  `core.tooltip.TooltipLines`; the provider reads the item data once and keeps the lines until the prices change.
  New dev tool: `/skyatlaz debug item <id>` gives an item with SkyBlock data and opens the inventory, so the
  item features can be tried offline. Fixed a crash: a mixin that calls `setReturnValue` needs
  `cancellable = true`. 195 tests, `check` green.
