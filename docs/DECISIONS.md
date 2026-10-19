# Decision log

Newest first. Format: date, decision, why, consequences.

## 2026-10-19 - Fabric API events before mixins; legacy text for fixtures
Chat, tick and connection hooks use Fabric API events (`GameHooks`): stable across game updates and shared with
other mods. Mixins are reserved for what Fabric lacks (first one: tab header/footer accessor). Dumps store text
with legacy `§` codes because Hypixel patterns (rarities, chat formats) are written in them and they diff well
in fixtures. Dumps are pure data (`Snapshot`), read by tests without the game.

## 2026-10-19 - Own config model, flat versioned JSON
Config is our own typed model (`core.config`) rather than a library, so the future screen (own UI or YACL behind
an adapter, still open) only reads `ConfigEntry` metadata. Flat sorted keys make migrations simple key edits and
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
