# Testing

## 1. Fast tests (always)
`./gradlew test` (part of `./gradlew check`). Must stay under ~10 s.
- **Architecture tests** (`architecture/ArchitectureTest`): rules from `config/source-rules.json` + structure rules.
- **i18n tests** (`i18n/TranslationKeysTest`): used keys exist, keys are namespaced and unique.
- **Unit tests**: one per parser/calculator/state machine. Keep logic out of Minecraft classes so it can be
  tested with plain inputs.

## 2. Fixtures from the real game [planned: core/debug]
Hypixel cannot run locally, so we capture reality:
1. In game, run `/skyatlaz debug dump <name>`: writes current scoreboard, tab list, recent chat and held item
   data to `run/skyatlaz-dumps/<name>.json`.
2. Copy useful dumps to `src/test/resources/fixtures/<area>/` and write a test against them.
3. Every bug found in game becomes a fixture + failing test first, then the fix.

## 3. Running on Hypixel
- `./gradlew runClient` (or the "Minecraft Client" run config in IntelliJ).
- The Hypixel Mod API Fabric mod is added to the dev runtime automatically.
- **Login:** enable DevAuth by adding to your user-level `~/.gradle/gradle.properties` (never the repo):
  ```
  skyatlaz.devauth=true
  ```
  then run with VM option `-Ddevauth.enabled=true`. First launch asks you to log in with Microsoft in the browser.
  If DevAuth fails on this Minecraft version, disable it and report it in `docs/PROGRESS.md`.
- Prefer an **alt account** for testing new features.
- Hot reload: use JetBrains Runtime 25 as the project JDK and debug mode; method body changes apply without restart.

## 4. Performance checks
- Spark profiler mod in dev runtime for sampling sessions (when needed).
- [planned] per-module timing overlay in debug mode.
