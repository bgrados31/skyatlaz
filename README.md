# SkyAtlaz

All-in-one quality of life mod for Hypixel SkyBlock. Fabric, Kotlin, Minecraft 26.2.

## Setup
1. Install JDK 25 (IntelliJ: *Project Structure > SDK > Download JDK*, JetBrains Runtime 25 recommended for hot
   reload) and set `JAVA_HOME` to it.
2. Open the folder in IntelliJ and import the Gradle project.
3. The first build runs `installGitHooks` (commit message + formatting hooks in `.githooks/`).
4. Optional: `node tools/refs/sync.mjs` to download reference mods for study (`references/`, git-ignored).

## Commands
| Command | What |
|---|---|
| `./gradlew check` | Format check, compile, unit + architecture tests |
| `./gradlew spotlessApply` | Auto-format |
| `./gradlew runClient` | Launch the game (login: `docs/TESTING.md`) |
| `./gradlew build` | Build the jar into `build/libs/` |

## Docs
Start with [CLAUDE.md](CLAUDE.md) (rules) and [docs/PROGRESS.md](docs/PROGRESS.md) (status).
