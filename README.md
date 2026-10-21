<h1 align="center">skyatlaz</h1>

<p align="center">
  All-in-one quality of life mod for Hypixel SkyBlock.<br>
  Client side, Fabric, Kotlin, Minecraft 26.2.
</p>

<p align="center">
  <a href="https://github.com/bgrados31/skyatlaz/actions/workflows/build.yml"><img src="https://github.com/bgrados31/skyatlaz/actions/workflows/build.yml/badge.svg" alt="build"></a>
  <a href="https://github.com/bgrados31/skyatlaz/blob/main/LICENSE"><img src="https://img.shields.io/github/license/bgrados31/skyatlaz?color=blue" alt="license"></a>
  <a href="https://github.com/bgrados31/skyatlaz/releases"><img src="https://img.shields.io/github/downloads/bgrados31/skyatlaz/total?label=downloads" alt="downloads"></a>
  <img src="https://img.shields.io/badge/Minecraft-26.2-brightgreen" alt="minecraft 26.2">
  <img src="https://img.shields.io/badge/Fabric-loader%200.19.5%2B-DBB989?logo=fabric&logoColor=white" alt="fabric">
  <img src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white" alt="kotlin">
</p>

skyatlaz reads what the game shows you — the scoreboard, the tab list, the menus and the item lore — and puts the
numbers you were opening three screens for on top of your screen. It ships a HUD, prices in the tooltip, the Heart of
the Mountain tree and a command palette, and it stays inside the [rules of Hypixel](docs/HYPIXEL_RULES.md): every
feature carries a tier (Allowed / Gray / Forbidden) and anything Forbidden never ships.

## Features

<details open>
<summary>What is in the box</summary>

### Status HUD
A card with your SkyBlock status: area, profile, level, purse, bank and bits — read from the scoreboard, so it works
anywhere in SkyBlock and costs nothing outside of it.

### Item prices in the tooltip
Hover any item and the tooltip grows three lines: the Bazaar instant sell price, the lowest BIN of the auction house
and the NPC price. No key, no account, no auction sniper: just the public prices.

### Held item price
A card that follows the item in your hand and shows what it is worth right now.

### Heart of the Mountain
The mining tree as a card: every perk with its level, the maximum level from the community layout and the pickaxe
ability you have equipped. The tree is read from the menu you have open, so it is always what the game itself shows.

### Command palette
`Ctrl+K` opens a search box over every command of the mod and every vanilla command you can run. Type, arrow keys,
`Enter`.

### HUD editor
`/skyatlaz hud` puts every element of the HUD on screen so you can drag it where you want, with snapping and a reset.
No restart, no editing a file by hand.

### Settings screen
`/skyatlaz config` opens the mod settings: every feature, every keybind and the theme.

### Captures for bug reports
`F7` saves a capture of the scoreboard, the tab list, the recent chat, the held item and the open container to a file.
Paired with `/skyatlaz debug ...`, it is how bugs get reproduced without you typing five paragraphs.

</details>

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.2 (loader 0.19.5 or newer).
2. Drop these mods into your `mods` folder, next to skyatlaz:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
   - [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api)
3. Download skyatlaz from the [releases page](https://github.com/bgrados31/skyatlaz/releases) and drop the jar in too.
4. Join Hypixel SkyBlock. Everything is on by default; `/skyatlaz config` turns features on and off.

The config lives in `.minecraft/config/skyatlaz/config.json` and is rewritten by the settings screen, so you rarely
have to touch it by hand.

## Commands

| Command | What it does |
|---|---|
| `/skyatlaz help` | The commands of the mod, grouped by area |
| `/skyatlaz version` | Mod, Minecraft, loader and Fabric API versions |
| `/skyatlaz config` | The settings screen |
| `/skyatlaz hud` | Move the HUD elements on screen |
| `Ctrl+K` | The command palette |
| `F7` | Save a capture for a bug report |
| `/skyatlaz debug dump <name>` | Save a capture of the game state to a file |
| `/skyatlaz debug open <name>` | Open a saved capture |
| `/skyatlaz debug load <name>` | Replay a capture into the mod state |
| `/skyatlaz debug ...` | The rest of the developer commands: chat, state, item, theme, price, neu, http, hypixel, service |

The debug commands are developer tooling, not gameplay: they read the same state the mod already has on screen and
never talk to the game on your behalf.

## Building from source

You need JDK 25 (IntelliJ: *Project Structure > SDK > Download JDK*, the JetBrains Runtime is the one that hot reloads
best) and `JAVA_HOME` pointing to it.

```sh
git clone https://github.com/bgrados31/skyatlaz.git
cd skyatlaz
./gradlew check        # format check, compile, unit and architecture tests
./gradlew runClient    # launch the game with the mod (login: docs/TESTING.md)
./gradlew build        # the jar lands in build/libs/
```

The first build installs the git hooks (commit message and formatting). To work on the mod, start at
[CLAUDE.md](CLAUDE.md) — the rules of the code — and [docs/](docs/) — the reasons.

## The optional service

Some numbers Hypixel only publishes through its developer API (your profile, the museum, the garden) live in
**skyatlaz-service**, a small companion server. The mod itself never holds an API key: it signs a challenge with the
Minecraft profile key pair, the service answers with a token bound to your profile, and the token only ever fetches
your own data. The mod is fully usable without the service — features degrade to the last data they had or turn
themselves off.

The contract lives in [docs/api/INDEX.md](docs/api/INDEX.md). The service is developed alongside this mod and is not
required to install it.

## Documentation

| Document | What it answers |
|---|---|
| [CLAUDE.md](CLAUDE.md) | The rules every change to this mod follows |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | How the pieces fit together |
| [docs/CONVENTIONS.md](docs/CONVENTIONS.md) | Naming, structure and style |
| [docs/TESTING.md](docs/TESTING.md) | How to test in game, and how to log in in dev |
| [docs/HYPIXEL_RULES.md](docs/HYPIXEL_RULES.md) | The tier of every feature, with the rules that decide it |
| [docs/I18N.md](docs/I18N.md) | How translations work |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why the mod does things this way |
| [docs/PROGRESS.md](docs/PROGRESS.md) | What is done and what comes next |

## Playing by the rules

Hypixel is explicit about what a mod may do, and skyatlaz is explicit about where every feature sits:

- **Allowed**: it only shows you information the game already shows you.
- **Gray**: it shows you public information the game does not show you right now. Read the tier before you rely on it.
- **Forbidden**: it plays for you. It does not exist in this mod and it will not.

The tier of each feature, with the rule that decides it, is in [docs/HYPIXEL_RULES.md](docs/HYPIXEL_RULES.md).

## Data sources and credits

| Data | Source |
|---|---|
| Bazaar prices, item catalog | [Hypixel Public API](https://api.hypixel.net/) (keyless endpoints) |
| Location, party | [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api) |
| Lowest BIN auction prices | [lowestbins](https://github.com/Tricked-dev/lowestbins) by **Tricked**, hosted instance `lb.tricked.dev`. Thank you! |
| Item pages and recipes | [NEU-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO) (MIT), fetched per item on demand |

## Contributing

Bug reports and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first — it is short, and it is the
difference between a review and a rewrite.

## License

skyatlaz is free software licensed under the [GNU Lesser General Public License v3.0 or later](LICENSE).
