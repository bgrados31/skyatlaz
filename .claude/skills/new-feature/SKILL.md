---
name: new-feature
description: Use when adding a new skyatlaz feature/module (HUD, overlay, timer, solver, chat tweak, etc.). Gives the mandatory checklist and file template so every feature has the same shape.
---

# Adding a feature to skyatlaz

## 1. Before writing code
1. Classify it in `docs/HYPIXEL_RULES.md` (Allowed / Gray / Forbidden). Forbidden -> stop and tell the user.
2. Check `docs/PROGRESS.md`: the core pieces it needs must exist (module system, events, state, HUD...).
   If a needed shared piece is missing, build it in core/ui/util first, with tests, then the feature.
3. Look for an existing helper before writing any parsing, rendering, color, HTTP or scheduling code
   (`docs/CONVENTIONS.md#reuse`). Need inspiration? Use the `skyblock-refs` skill (study, never copy).

## 2. Files
```
src/main/kotlin/org/tnozamas/skyatlaz/feature/<category>/<feature>/
  <Feature>Module.kt      Module subclass: lifecycle + wiring only
  <Feature>Config.kt      `object XConfig : ConfigSection("<category>.<feature>")`; set `override val config = XConfig`
                          (the on/off toggle is automatic; keep `enabledByDefault = false` if Gray)
  <Feature>Logic.kt       Pure logic (parsing/calculations), no Minecraft calls -> unit-testable
  <Feature>Hud.kt         Only if it draws something (uses ui.hud + ui.theme)
src/test/kotlin/org/tnozamas/skyatlaz/feature/<category>/<feature>/<Feature>LogicTest.kt
src/test/resources/fixtures/<category>/...   Real game samples used by the test
src/main/resources/assets/skyatlaz/lang/en_us.json   New keys
```
Category and feature folder names: lowercase, no underscores (`dungeons/scorehud`).

## 3. Module template
```kotlin
/**
 * <What it does, one line>.
 * Hypixel rules tier: Allowed. <why>
 */
object ExampleModule : Module(id = "example", category = FeatureCategory.MISC) {
    override fun onEnable(ctx: ModuleContext) {
        ctx.on<SomeEvent> { event -> /* cheap, no allocations */ }
    }
}
```
- Register everything through `ctx` (`on`, `every`, `later`, `async`, `command`) so disabling removes it automatically.
- Commands: `ctx.command { literal("x") { category(...); description("command.x"); executes { } } }`. Every node needs
  a description key; nodes without `executes` show generated help. Never register with Fabric/Brigadier directly.
- Chat output: `Chat.info/success/warning/error(Tr.tr(...))`; colors and gradients from `ui.theme.Theme`.
- Never import another feature. Use events and `skyblock` state.
- All text via `Tr.tr("<category>.<feature>.<element>")`; config keys `config.<category>.<feature>.<entry>.name|description`.

## 4. Done when
- [ ] Logic unit test with fixtures passes.
- [ ] `./gradlew check` green.
- [ ] Keys added to `en_us.json`.
- [ ] Tested in game (or listed under "needs in-game test" in `docs/PROGRESS.md`).
- [ ] `docs/PROGRESS.md` updated. Suggest a commit like `feat(<category>): add <feature>`.
