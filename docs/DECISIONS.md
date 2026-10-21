# Decision log

Newest first. Format: date, decision, why, consequences.

## 2026-10-21 - The command palette ignores the character of its own key
The palette is opened with a key (Ctrl+K) and the search field is focused right away, so the player can type. The
key that opens it is also delivered as a character, and it landed in the field ("kpric" instead of "pric"). The
palette knows the key it was opened with and drops that character when it arrives right after opening
(`ui.palette.KeyCharacters` maps a GLFW key code to its character); a character typed later is kept. Consequence: the
palette works with any key the player binds to it, letter or not.

## 2026-10-21 - The command palette runs commands through the chat path
A command without arguments is run by sending it like the chat does (`ClientPacketListener.sendCommand`), so the
client dispatcher answers it before the server sees it; a command that needs an argument is typed into the chat
instead, with the cursor ready for the argument. Consequence: the palette never needs its own executor, and the
vanilla suggestions still work in the chat it opens.

## 2026-10-21 - A key binding that works with a screen open
Some states only exist while a container is open (the Heart of the Mountain tree is an inventory), and with a container
open the chat is blocked, so the player cannot type the dump command. `KeyBind` grew a `worksInScreen` flag: those
bindings are polled even while a screen is open, because they read the key state and not the key events. The capture
key (F7 by default) saves the state as a dump, named after the title of the open container ("Heart of the Mountain" ->
`heart_of_the_mountain`) or, with no container open, after the area of the player. Consequence: the states that need an
open inventory can be captured without the chat; the binding can be changed in the "Controls" tab like any other.

## 2026-10-21 - The price lines are shared in `util`, not owned by a feature
Two features show prices (the item tooltip and the card of the item in hand) and features must not import each
other, so the logic that turns the known prices into translatable lines lives in `util.PriceLines` (`ItemPrices`,
`PriceOptions`, `PriceLine`), next to `NumberStyle`, which moved there for the same reason. The translation keys moved
with it (`economy.price.*`), so the tooltip and the card say the same thing. Consequence: a third feature that shows
prices reuses the same lines; a feature that needs other wording adds its own keys.

## 2026-10-21 - The item in hand is watched once per tick, by reference
The card of the item in hand must follow the player switching items. Fabric has no event for that, so the hand is
read once per client tick: the stack object changes with the item, so one reference comparison tells that something
changed, and the prices are only looked up then (never per frame). Consequence: the card follows the switch one tick
later, and a feature that is off costs nothing (the module stops and its element disappears with it).

## 2026-10-20 - Prices go in the vanilla item tooltip, through a mixin
26.2 has no item tooltip event: Fabric only offers `ClientTooltipComponentCallback` (for special components like
the bundle preview), and the creative screen builds its own tooltip. So the price lines need a mixin, and the target is
`Screen.getTooltipFromItem` (the method every container screen calls). The mixin only merges the lines a feature
provides and never decides what they say: `core.tooltip.TooltipLines` is the registry, the feature installs a
provider while it runs and removes it when it stops, so a disabled feature adds nothing (and the tooltip costs one
empty check). The provider reads the SkyBlock data of the item once (the tooltip is rebuilt every frame, and the same
stack comes back every frame) and keeps the built lines until the prices or the settings change. The creative screen
overrides that method, so it needs a second mixin on `CreativeModeInventoryScreen.getTooltipFromContainerItem`:
without it the dev world (creative by default) would show no prices at all. Consequence: the feature works on the
inventory and on any container, and the prices download only while the feature runs on SkyBlock.
`/skyatlaz debug item <id>` gives the player an item with SkyBlock data and opens the inventory, so the feature can
be tried offline without joining Hypixel.
## 2026-10-20 - Our own key bindings, not the vanilla `KeyMapping`
The vanilla `KeyMapping` adds itself to a static list in its constructor and 26.2 has no way to remove it, so a
binding of a feature that is off would stay in the vanilla controls screen forever, doing nothing. We poll the keys
ourselves once per client tick (`InputConstants.isKeyDown`), fire on the press (not while held) and only while no
screen is open, and store the chosen key in our own config (`keybind.<id>.key`). Features bind through
`ctx.keyBind(bind) { }`, so a binding exists only while the feature runs. The player changes a key in the settings
screen (the "Controls" tab): click a row, press a key, Escape cancels. Consequence: the key names come from the
vanilla translations and the modifiers from ours, and two bindings on the same key are reported once in chat. The
settings screen had to grow a tab: the search field moved to the title row, because the tab bar could not hold four
tabs next to it.

## 2026-10-20 - One rhythm for every surface
The spacing of the mod was decided in each screen and each HUD card (`3`, `2`, `8`, `16`...). Now
`ui.theme.Spacing` owns the scale (`TIGHT`, `GAP`, `GAP_WIDE`, `PAD`, `PAD_WIDE`, `MARGIN`) and the
widget kit, the HUD cards and the settings screen take their values from it. Consequence: changing one number moves
the whole mod, and a new surface starts from the same rhythm instead of inventing numbers.
## 2026-10-20 - First vertical slice: the SkyBlock status card
Phase 4 starts with one feature that uses the whole stack at once: `feature/skyblock/statushud` reads the shared
SkyBlock state, shows area, profile, level and money on the HUD, and takes its settings from a `ConfigSection`
(`skyblock.statushud`), so the config screen, the module system, the HUD system and the i18n rules are all
exercised by real code instead of by the debug preview. The rows are built by a pure `StatusHudLogic` (unit tested
without the game) and only translated when the element draws them, so a state change costs one rebuild and a frame
costs nothing. The module's activation condition is "on SkyBlock", which needed `ModuleManager.refreshActivation()`
to actually be called: the entrypoint now wires it to `SkyBlockStateChangeEvent`, so a module that is not active
costs nothing and its HUD element disappears off SkyBlock. Consequence: the first feature needed a new
`FeatureCategory.SKYBLOCK` and the translation keys `config.skyblock.statushud.*`; the next ones follow the same
shape (`ConfigSection` + pure logic + HUD element + tests).

## 2026-10-20 - The UI sandbox was deleted with the prototype screens
The owner confirmed it: `ui/prototype` and `/skyatlaz debug ui` are gone, and with them the `skyatlaz.prototype.*`
translation keys. The real config screen is now the only consumer of `ui.component`, so there is a single place
where the engine is exercised and a single set of texts to keep.

## 2026-10-20 - Config screen groups sections by top-level id, saves live
The real `ConfigScreen` builds its tabs from `ConfigManager.registeredSections`: sections sharing a first id
segment (`dungeons.scorehud`, `dungeons.map`) land in one tab and keep their section headings; a single section
uses its own title as the tab. Module `module.<id>.enabled` toggles get a trailing Modules tab. `hud.*` sections
are excluded because `/skyatlaz hud` owns placement and scale, and the raw fractions would be a worse editor.
Every edit uses `ConfigEntry.set`, so the existing debounced/atomic save runs and modules react through `onChange`;
there is no Apply button, and Reset restores the active tab. Enum choices render through `UiChoice` and translate
each value as `config.<key>.value.<lowercase enum name>`. Consequence: adding a feature only needs a
`ConfigSection` and its translation keys; a top-level tab title (`config.tab.<group>`) is needed only when a tab
has more than one section.

## 2026-10-20 - Own UI engine chosen for the config screen
The owner compared the six prototypes in game and picked the own engine (`ui.component`) as the config screen.
It is the only option that shares `ui.theme` tokens and the chat/HUD typography, adds no dependency or license,
and gives full control over the calm/typographic layout. MoulConfig stays the documented fallback if NEU-style
familiarity ever outweighs that consistency (LGPL-3.0-or-later, fixed styling). The other prototype screens and
their temporary Gradle repositories, dependencies and `*_version` properties were deleted; `ui/prototype` now
only keeps the engine demo (`OwnPrototypeScreen` + mock `PrototypeState`), opened by `/skyatlaz debug ui`, until
the real screen replaces it. Consequence: we own the widgets the config needs, so dropdown, keybind and color
picker are future work; no UI library enters the jar. The real screen reads `core.config` metadata and extends
this kit rather than introducing a second style.

## 2026-10-19 - NEU-REPO item pages fetched per item, not bundled
Recipes and static item facts are not in Hypixel's public API. NEU-REPO (MIT, community-maintained) has one JSON
page per item, but ~8,900 files; bundling or mirroring them would bloat the jar and require shipping the upstream
license. Instead `NeuRepository` fetches `items/<ID>.json` from the repository's `master` branch through the shared
HTTP cache only when that item is actually needed, keeps a 128-entry LRU and stops retrying a missing id for a
minute. Consequence: recipe features need a network/cache hit per item, but startup stays free and data stays
current; if GitHub raw is unavailable, the last cached page still works. Credit is in README.

## 2026-10-19 - SkyBlock state from scoreboard/tab, captured every 10 ticks
The scoreboard and tab list are the only real-time sources available without a Hypixel API key. Capturing them
every 10 ticks (0.5 s) instead of every tick keeps the cost off the render/tick hot path; the sources are compared
by content and parsed only when they change. Parsing strips legacy codes first because Hypixel inserts codes inside
words and numbers (`Piggy: §6301,788§j§6,444`), and finds the sub-area by its private-use glyph (U+E067) rather
than by a name list. State is a plain data class updated by a pure tracker, so it is unit tested from real dumps and
`/skyatlaz debug load` can drive the same events offline. Features depend on this state, never on Minecraft.

## 2026-10-19 - Lowest BIN from a third-party API, only with permission
Hypixel does not publish lowest BIN prices; computing them from the official auctions endpoint means ~60 pages
per refresh on every client. The owner chose a third-party provider used with its author's explicit permission.
Checked 2026-10-19: Moulberry (old NEU) is down; EliteSkyblock (Firmament, SkyHanni) and hysky.de (Skyblocker's own
backend) are public but publish no terms; Tricked's lowestbins (Skytils, Apache-2.0) is the only one with a stated
condition: credit when using the hosted instance. Enabled: `lb.tricked.dev/lowestbins` (Hypixel ids, flat
`{ "ID": price }`), credited in README; priority bazaar > auction > NPC. If it goes away, the project can be
self-hosted; EliteSkyblock remains an option if its author agrees.

## 2026-10-19 - Fabric API events before mixins; legacy text for fixtures
Chat, tick and connection hooks use Fabric API events (`GameHooks`): stable across game updates and shared with
other mods. Mixins are reserved for what Fabric lacks (first one: tab header/footer accessor). Dumps store text
with legacy `§` codes because Hypixel patterns (rarities, chat formats) are written in them and they diff well
in fixtures. Dumps are pure data (`Snapshot`), read by tests without the game.

## 2026-10-19 - Own config model, flat versioned JSON
Config is our own typed model (`core.config`) rather than a library, so the screen (own UI engine, chosen
2026-10-20) only reads `ConfigEntry` metadata. Flat sorted keys make migrations simple key edits and
the file readable; unknown keys are preserved so removing or disabling a feature never loses settings. Module
toggles live in the config, and modules are off unless they opt in (`enabledByDefault`), which keeps "disabled
costs nothing" the default and Gray features safe.

## 2026-10-19 - Declarative command registry with generated help
The owner wants a professional, scalable command system with no hand-wired or dead-end commands. All commands
are declared as a tree (`CommandBuilder` DSL) in one `CommandRegistry`; help is generated from description keys
and categories, and every node without an action prints its help. Modules declare commands through
`ctx.command`, so they follow the module lifecycle. Brigadier cannot remove nodes, so nodes are reference counted
and hidden through `requires`; Fabric only re-filters suggestions on refresh, so every change triggers one.

## 2026-10-19 - MCP Fabric for agent access, singleplayer only
To let the AI see and test the game, MCP Fabric (MIT, supports 26.2, loopback + token) is loaded through
`fabric.addMods` only by `runWorld`, opt-in per machine. It automates input, so it must never reach Hypixel:
it is not a dependency, not in the release jar, and not loaded by `runClient`/`runHypixel`. Alternatives:
miziusLabs/Mod (no license), minecraft-driver-mcp (1.21.1 only).

## 2026-10-19 - Gradient identity: OKLab lookup tables, semantic colors
The owner wants a distinctive gradient look. Gradients mix stops in OKLab (keeps perceived lightness: red to
green does not turn dark olive; CSS uses the same default) and bake a 256-entry table at construction, so
sampling in render paths is an array read. Semantic gradients follow familiar hues (green success, amber
warning, red error, blue info). Revised the same day to a soft palette (aqua, lavender, rose; pastel semantics,
message bodies going from a deep to a soft tone of their own hue, never to white, and no icons)
because the saturated version read as noisy. Glyphs were vetted by rendering them in game: curved box corners
(╰ ╭) are 3px wide in the game font. The final help design drops decoration altogether: hierarchy comes from
indent, a fixed color role per element and blank lines around sections, which reads calmer than any glyph set.
Gradient text is built in
`i18n` (`withGradient`) because only `i18n` may create components from strings; all chat output goes through
`ui.chat.Chat`.

## 2026-10-19 - Test offline first, Hypixel last
Joining Hypixel is slow, needs a login and can't reproduce rare states on demand. Most logic is tested with
fixture-based unit tests; UI is iterated in a singleplayer sandbox fed by dumps of real Hypixel state
(`core.debug`). Hypixel is used to capture dumps and for the final check. Consequence: features must depend
only on shared SkyBlock state and events, so the sandbox can drive them. See `docs/TESTING.md`.

## 2026-10-19 - DevAuth opt-in from user gradle.properties
`skyatlaz.devauth=true` in `~/.gradle/gradle.properties` adds DevAuth and its JVM flags to the client run, so
nobody needs IDE-specific VM options and the repo never holds account settings.

## 2026-10-19 - Text-based rule tests instead of Konsist/ArchUnit/detekt
Pattern rules live in `config/source-rules.json`, read by both `ArchitectureTest` and the Claude Code
`guard-edit` hook, so a rule is defined once. Text scanning runs in milliseconds. detekt stable (1.23.x) does not
support Kotlin 2.4 yet; revisit when detekt 2.0 is stable.

## 2026-10-19 - Single client source set
The mod is client-only, so Loom's split environment source sets and the datagen/main entrypoints from the
template were removed. One `main` source set, `environment: client`.

## 2026-10-19 - Minecraft 26.2 (not 26.3)
The IntelliJ template picked 26.3, but the Hypixel Mod API Fabric build, Skyblocker and SkyHanni target 26.2,
and Hypixel only accepts the latest two content updates. Bump once Hypixel + Mod API support 26.3.

## 2026-10-19 - Private project, no code copying
All Rights Reserved. Reference mods (Skyblocker LGPL-3.0, SkyHanni LGPL-2.1) are studied for approach only;
`references/` is git-ignored and edits to it are denied.

## 2026-10-19 - Kotlin
Chosen by the owner. Java is limited to mixins. Performance rules in `docs/CONVENTIONS.md` compensate for
Kotlin's easy-to-miss allocations (lambdas, collection chains, boxing).

## 2026-10-19 - i18n from day one
No hardcoded user-facing text; English source file, other languages / translation API later.

## 2026-10-19 - UI library comparison in game (superseded by own engine)
The owner asked to compare the candidate config libraries before choosing one. Six prototype screens with the
same mock content (General: debug toggle, HUD scale, player name; Prices: bazaar toggle, lowest BIN toggle,
refresh rate) were built and rendered in `runWorld`; the picker itself is `/skyatlaz debug ui`. Screenshots:
`run/ui-comparison/*.png` (git-ignored). Findings:

- YACL 3.9.7 (LGPL-3.0, ~1.1 MB + Twelvemonkeys image libs): tabbed dark panel, side Reset/Undo/Done column,
  hover descriptions, background dim. Complete but visually its own product; overriding colors/fonts is limited.
- Cloth Config 26.2 (LGPL-3.0, ~1.1 MB): tab arrows, search box, big buttons, purple tooltips. Easiest to bind
  to existing `core.config` sections, but the chunky vanilla look is the furthest from the calm/typographic
  target.
- SpruceUI 11.0.1 (MIT, ~252 KB): translucent panel with separators, borderless sliders and toggles. Closest to
  the target look and the lightest dependency, but it only ships widgets: tabs, search, scrolling, save/cancel
  wiring and the dim are ours, and it is maintained by a single author.
- owo-lib 0.13.1 (MIT, ~2.6 MB + endec, jankson, kdl4j): flat transparent layout, chunky checkbox/slider, no
  dim or section headers by default. Its generated `ConfigScreen` needs an annotation-processed config class
  (not applicable to our runtime `ConfigManager`), so a real screen would be hand-built with `owo-ui` widgets.
  Loom also injects owo's mixin interfaces into the shared Minecraft jar, which forces owo onto the test runtime
  while the prototype exists.
- Yumi 1.1.3+26.2: cannot be compared; the artifact contains only entrypoints/events/crash reporting, no UI or
  config classes (it is a foundation library, not a config screen).
- MoulConfig 4.7.2 (LGPL-3.0-or-later, `org.notenoughupdates.moulconfig:modern-26.2`, ~519 KB from
  `maven.notenoughupdates.org`, not `repo.nea.moe`): the de-facto panel of NEU, SkyHanni and Firmament (Skyblocker
  shades it through Dandelion). Ships its own MC 26.2 platform adapter and built-in editors, so the prototype only
  describes categories/options; it opens with the familiar Categories column, search icon and option cards. The
  style is fixed (its own dark panel, aqua categories, green switches), so it cannot use `ui.theme` or match chat.
- Own engine (`ui.component`, no dependency): panel, tabs, search, scroll with scissor, focus ring, keyboard
  navigation, tooltips and toggle/slider/text/button widgets, all drawn from `ui.theme` tokens. The prototype
  reuses the mock content and adds filtering and a scrollbar. It is the only option that can share typography and
  palette with the HUD/chat, and the code becomes the real config screen if chosen; it is also the most code we
  own and the one that needs the most polish over time.

Consequence at the time: the prototypes were temporary (`ui/prototype`, their repos/deps in `build.gradle.kts`,
`*_version` properties) and either got removed or converted into `ui.component` + the real config screen once the
owner picked. On 2026-10-20 the owner chose the own engine, so the other screens and their dependencies were
removed (see the top entry). Own UI remains the consistency argument: `ui.theme` tokens, the same typography as
chat/HUD, blank-line spacing and no new runtime dependency.

## 2026-10-19 - HUD placement as fractions, one Fabric hook
HUD positions are stored as `x`/`y` in `0..1` of the free space after scaling, not as pixel offsets or anchor
enums. The same saved layout therefore survives window resizes and GUI-scale changes, and the editor only has to
drag a fraction. Scale is constrained to `0.5..3.0`. Every element registers its own `hud.<id>` config section
when it first appears, so modules that never run add no keys; values are clamped on load and on every edit.
A single Fabric `HudElement` (`skyatlaz:hud`) draws all elements, which keeps the hook count constant and makes
"no elements = no work" a single empty-list check. The editor hides the normal HUD to avoid double drawing and
uses the same placement math as gameplay, so what is dragged is exactly what renders. Debug mode may add a
`DebugHudPreview` element: it is the development fixture for the editor, not a player-facing feature.
## 2026-10-21 - The mining tree is read out of the game, not from the Hypixel API
The public API of Hypixel needs a developer key: the old v1 endpoint answers Unknown endpoint (it is gone) and the
v2 one answers Missing API-Key header. The policy of Hypixel about the keys (https://developer.hypixel.net/policies)
closes the door: "Do not share your API key with 3rd parties, such as entering them into a website or a mod. This
applies to any publicly available mod" and "Do not include your API key in your code, especially if you plan to
distribute a binary". The key of the owner of the mod can never travel inside it, and a key per player would ask
every player to register an application in the developer portal, so no feature can depend on it.

So the tree of the Heart of the Mountain is read out of the menu that the player has open: the lore of the items
gives the name, the level, the maximum and the state of every perk the menu shows (the pattern is the one Skyblocker
uses), and the community layout of the tree (constants/hotmlayout.json of NEU-REPO, fetched on demand like the item
pages) gives the names and the maximum levels of all the perks. The module reads the open menu once a second and the
card follows it; it costs nothing when the menu is closed.

The API route was written and thrown away (the commit that added it is in the history). The spec and an index of the
endpoints stay in docs/api, with the policy that stops us from using them, so nobody tries it again.

Consequence: the mining card is Allowed (it shows what the game shows the player) and it needs no key, no network
for the perks and no registration. The price features keep using the bazaar and the NEU-REPO, which are public and
need no key.
