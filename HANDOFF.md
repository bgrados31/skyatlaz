# Handoff - SkyAtlaz base closed, phase 4 running

Updated: 2026-10-21 (later). Read `CLAUDE.md` first; this file is the short state of the current task.

## Objective
The base (phases 0-3) is closed and phase 4 is running: three vertical slices work end to end
(`feature/skyblock/statushud`, `feature/economy/itemprice`, `feature/economy/heldprice`) and the three gaps that were
left in the base (rhythm, render helpers, key bindings) are filled. Next: more vertical slices, one category at a
time, reusing the shape proven by the first one.

## Done
- **One rhythm** (`ui.theme.Spacing`): `TIGHT`, `GAP`, `GAP_WIDE`, `PAD`, `PAD_WIDE`, `MARGIN`. The widget kit
  (`UiMetrics`), the HUD cards (`StatusHud`, `DebugHudPreview`) and the settings screen take their values from it.
- **`ui.render`**: `Shapes` (fill/outline/panel/text with theme colors, no raw ARGB in features) and `CachedText`
  (builds a component when the value changes, reuses it every frame; 3 tests). `KeyNames` writes a combination
  ("Shift + F9") with the vanilla key names.
- **Key bindings** (`core.keybind`): `KeyCombo` (key code + modifiers, packed into one config number), `KeyBind`
  (id + default + texts) and `KeyBindManager` (registry, persistence in `keybind.<id>.key`, conflict detection, one
  poll per client tick). Features bind through `ctx.keyBind(bind) { }`; a binding only exists while the feature runs,
  fires on the press and never while a screen is open. 11 tests.
- **Rebinding in game**: the settings screen grew a "Controls" tab; click a row, press a key, Escape cancels. The
  search field moved to the title row, because the tab bar could not hold four tabs next to it.
- **First vertical slice** `feature/skyblock/statushud` (from the previous session): a HUD card with area, profile,
  level, purse, bits and bank; pure `StatusHudLogic` (7 tests).
- **Third vertical slice** `feature/economy/heldprice`: a card on the HUD with the item in the player's hand and what
  it is worth (bazaar instant sell, lowest BIN, NPC price). The hand is read once per client tick and compared by
  reference, so the card follows the player switching items and the prices are only looked up when the item
  changes. Pure `HeldPriceLogic` (5 tests). The card hides itself when the item has no known price.
- **Command palette** (`ui/palette`): Ctrl+K opens a fuzzy search over every command of the mod. Enter runs the
  selected one; a command that needs an argument is typed into the chat. The key that opens it is dropped from the
  search field (it arrives as a character too), and the palette reads the command tree, so it also shows the commands
  a module adds while it runs. Pure `CommandPaletteLogic` (8 tests) + `KeyCharacters` (3 tests).
- **A capture binding** (F7 by default, `worksInScreen`): saves the state as a dump while a screen is open, named
  after the container title or the area. It is how the HOTM tree (an inventory) can be captured. 4 tests.
- **The price lines are shared** in `util.PriceLines` (`ItemPrices`, `PriceOptions`, `PriceLine`), because two
  features cannot import each other. The translation keys moved with them (`economy.price.*`), so the tooltip and
  the card say the same thing. The tooltip feature's old `ItemPriceLogic` is gone (its test moved to
  `util/PriceLinesTest`).
- **The perks of the HOTM are read** (`feature/mining/hotm/HotmLogic.kt`): the tree is a container, so the perks come
  from the items and their lore. The lore pattern (`Level 12/20`, `Level 50` at the maximum) is the one Skyblocker
  uses, and the state of a perk is read from the marks the lore uses (`ENABLED` = active, `SELECTED` = the equipped
  ability, `Cost` = still to be bought, and the level of a locked perk is the one it would reach). Pure logic, 8
  tests against the real capture of the menu.
- **A capture is never overwritten** anymore: the name gets a number when it is taken (`heart_of_the_mountain`,
  `heart_of_the_mountain_2`, ...), so the scrolled views of the tree can all be captured.
- The **whole tree is known** (49 perks) from the NEU-REPO layout (`references/neu-repo/constants/hotmlayout.json`:
  name, maximum level, position in the tree and powder of every perk). The owner's capture only had 19 of them: it
  was one scrolled view.
- **The mining tree card** (`feature/mining/hotm`): the perks that matter and the ability that is equipped, as a
  card on the HUD. The tree is an inventory, so the perks are read out of the menu that the player has open (the lore
  of the items gives the name, the level, the maximum and the state of each perk) and the names and the maximum
  levels of all the perks come from the community layout of the tree (`constants/hotmlayout.json` of NEU-REPO, fetched
  on demand like the item pages). The module reads the open menu once a second. No key, no network for the perks.
- **The public API of Hypixel is not used**: it needs a developer key and the policy
  (https://developer.hypixel.net/policies) forbids sharing a key with 3rd parties or putting it in a mod. The spec
  and an index of the endpoints are in `docs/api`, with the policy that stops us.
- `.\gradlew spotlessApply check` green: **234 tests, 0 failures**.
- `ModuleManager.refreshActivation()` is now really called (the entrypoint wires `SkyBlockStateChangeEvent` to it),
  so a module with an activation condition starts and stops with the shared state.
- The UI sandbox (`ui/prototype`, `/skyatlaz debug ui`) is deleted with the owner's confirmation.
- `.\gradlew spotlessApply check` green: **215 tests, 0 failures** (before the HOTM parser).
- Verified in `runWorld` (screenshots `60`-`87` in `run/verify/`, git-ignored): the HUD card, the config screen with
  the new tab bar, the "Controls" tab, capturing F6 with the mouse and the keyboard, the key saved in the config
  (`keybind.open_settings.key: 295`) and F6 opening the settings screen from the world.

## Pending
1. Phase 4 continues: the next vertical slices (HOTM tree, dungeon score, purse/bank tracker...). The purse/bank
   tracker does not need anything from the owner.
2. **HOTM**: the card is done. To see the perks of a tier that is not in the view, the owner scrolls the tree (the
   card follows the open menu) or presses **F7** to capture the view (the names no longer overwrite each other) ✓.
   The public API is not an option: its key cannot travel inside the mod.
3. **Dungeon**: the owner's `dungeon.json` was taken 20 s into the run (`Time Elapsed: 20s`, `Cleared: 20% (32)`),
   so it has no score. The score arrives at the end of the run, in the chat (`You scored X points!`) and in the tab.
   A dump after the run is needed.
4. The owner still owes the `/skyatlaz debug hypixel` output (Mod API location + party).
5. World rendering is still missing: 26.2 replaced the old render events and Fabric does not expose the new
   submit-node API yet, so `ui.render` covers the GUI only.
6. Commit only when the owner asks. HEAD is `f5ff499`.

- **Second vertical slice** `feature/economy/itemprice`: the price of an item in its tooltip (bazaar instant sell,
  lowest BIN, NPC price; each one can be turned off). The lines are added by a mixin on
  `Screen.getTooltipFromItem` (`mixin/ScreenTooltipMixin.java`) and the registry is
  `core.tooltip.TooltipLines`; the feature installs a provider while it runs. The provider reads the SkyBlock data
  of the item once (the tooltip is rebuilt every frame) and keeps the lines until the prices or the settings change.
- **New dev tool**: `/skyatlaz debug item <id>` gives the player an item with SkyBlock data and opens the
  inventory, so the item features can be tried offline (the dumps have no container items). It also loads the `hub`
  dump when the world is not on SkyBlock: the price module only runs on SkyBlock, so without the sandbox state the
  tooltip shows nothing and the item id suggestions are empty. The chat says when the prices arrive
  ("Item prices loaded", debug mode only).
- `NumberStyle` moved to `util` (it is shared by two features now).
## Files
- `src/main/kotlin/org/tnozamas/skyatlaz/ui/theme/Spacing.kt` - the shared rhythm.
- `src/main/kotlin/org/tnozamas/skyatlaz/ui/render/` - `Shapes`, `CachedText`, `KeyNames`.
- `src/main/kotlin/org/tnozamas/skyatlaz/core/keybind/` - `KeyCombo`, `KeyBind`, `KeyBindManager`, `KeyModifiers`.
- `src/main/kotlin/org/tnozamas/skyatlaz/ui/component/UiKeyBind.kt` - the row that captures a key.
- `src/main/kotlin/org/tnozamas/skyatlaz/ui/config/ConfigScreen.kt` - the "Controls" tab, the search field in the
  title row, `RESERVED_SECTIONS` (hud, keybind are not shown as tabs).
- `src/main/kotlin/org/tnozamas/skyatlaz/feature/economy/itemprice/` - the price feature (module, config, provider).
- `src/main/kotlin/org/tnozamas/skyatlaz/feature/economy/heldprice/` - the held item card (module, config, pure logic, HUD).
- `src/main/kotlin/org/tnozamas/skyatlaz/util/PriceLines.kt` - the price lines shared by the two price features.
- `src/main/kotlin/org/tnozamas/skyatlaz/ui/palette/` - the command palette (screen, pure search, key characters).
- `src/main/kotlin/org/tnozamas/skyatlaz/core/debug/DebugCapture.kt` - saves the state as a dump, with a name from the screen.
- `src/main/kotlin/org/tnozamas/skyatlaz/feature/mining/hotm/HotmLogic.kt` - the perks of the HOTM read from the lore
  of the container and the rows of the card.
- `src/main/kotlin/org/tnozamas/skyatlaz/api/repo/HotmLayout.kt` - the layout of the tree (names, maximum levels).
- `docs/api/` - the spec of the public API of Hypixel and its index (not used, and why).
- `src/main/kotlin/org/tnozamas/skyatlaz/core/tooltip/TooltipLines.kt` - where features add lines to the item tooltip.
- `src/main/java/org/tnozamas/skyatlaz/mixin/ScreenTooltipMixin.java` + `CreativeTooltipMixin.java` - the mixins that
  merge those lines (the creative screen builds its own tooltip and needs its own hook).
- `src/main/kotlin/org/tnozamas/skyatlaz/util/NumberStyle.kt` - the shared number format choice.
- `src/main/kotlin/org/tnozamas/skyatlaz/core/module/ModuleContext.kt` - `keyBind(bind) { }`.
- `src/main/kotlin/org/tnozamas/skyatlaz/SkyAtlaz.kt` - `KEYBINDS`, the tick, the core "open settings" binding.
- `src/main/resources/assets/skyatlaz/lang/en_us.json` - `keybind.*`, `config.tab.controls`,
  `config.keybind.open_settings.*`.
- `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/DECISIONS.md` - updated.

## Commands
- `.\gradlew check` - Spotless + 215 tests.
- `.\gradlew runWorld` - dev client; agent mods need `skyatlaz.agentMods=true`.
- `node tools/agent/bridge.mjs` - MCP Fabric bridge on `127.0.0.1:25599` (`cmd`, `chat`, `screenshot`, `call`).
- `.\run\agent-input.ps1 -X <clientX> -Y <clientY>` - Win32 click/keys/typing for the dev window (git-ignored
  helper): click, `-Wheel`, `-Key` (F1..F12, ESC, TAB...), `-Type`, `-Backspace`, `-Out`.
- In game: `/skyatlaz config` (real settings), `/skyatlaz hud` (HUD editor), `/skyatlaz debug load hub` (replay a
  real Hypixel dump into the shared state), `/skyatlaz debug state`.

## Rules / notes
- UI: calm and typographic, soft palette, no decorative icons, blank-line spacing. Colors only from `ui.theme`;
  spacing only from `ui.theme.Spacing`; text only through `Tr.tr` + `en_us.json`.
- Do not re-add YACL/Cloth/SpruceUI/owo/MoulConfig unless the owner reverses the decision. MoulConfig is the
  documented fallback only (LGPL-3.0-or-later, fixed styling, cannot use `ui.theme`).
- The bridge cannot click custom widgets; use the Win32 helper above. The dev window is now 1280x720 and the
  screenshot is 1:1 with the client area, so the helper takes the screenshot coordinates directly (the old
  "logical x 2.4" note is outdated). The GUI scale is 3 (the logical screen is 427x240).
- Synthetic keys need a scan code in the message, otherwise the game reports them without a key code and the
  capture cannot map them (the helper already sends the Windows scan codes for F1..F12).
- `run/options.txt` has `pauseOnLostFocus:false`, otherwise the world is paused (Game Menu) when the agent takes
  screenshots.
