# Contributing to skyatlaz

Thank you for wanting to make this mod better. This file is the short version; the long version is
[CLAUDE.md](CLAUDE.md) (the rules) and [docs/CONVENTIONS.md](docs/CONVENTIONS.md) (the conventions).

## Before you start

- Use the newest Minecraft version Hypixel SkyBlock accepts. Bumping a version is a decision, not a chore: see
  [docs/DECISIONS.md](docs/DECISIONS.md).
- Read [docs/HYPIXEL_RULES.md](docs/HYPIXEL_RULES.md). A feature that is Forbidden on Hypixel does not ship, and a
  Gray one ships with its tier written down and a config to turn it off.
- Open an issue for anything big before you write the code. Half of a feature in the right direction beats a whole
  feature in the wrong one.

## The rules that matter

1. **No hardcoded user-facing text.** Every string a player reads goes through `Tr.tr` and has a key in
   `en_us.json`. The mod is translatable by construction, not by cleanup.
2. **Colors and spacing come from the theme.** `Theme` and `Spacing`, never raw ARGB in a feature.
3. **Features do not import each other.** A feature talks to the game through the shared packages (`core`, `api`,
   `util`, `ui`, `i18n`), never through another feature. `./gradlew check` fails if it does.
4. **Logic lives in `*Logic.kt` and has tests.** Parsing and arithmetic never live in a screen or a module: that is
   how they get tested without a running game.
5. **A disabled feature costs nothing.** A module that is off does not register listeners, does not run tasks and
   does not download anything.
6. **One feature per commit, and `./gradlew check` green.** The hook enforces the commit format
   ([Conventional Commits](https://www.conventionalcommits.org/)) and the formatter; the tests are on you.

## How a change lands

`main` is protected on GitHub: a direct push, a force push and a delete are all rejected. Only a pull request with a
green `check` workflow lands on it, with no bypass for anyone. That is the flow:

1. `git clone` your fork and open it in IntelliJ. The first Gradle build installs the git hooks.
2. Branch: `git switch -c feat/<name>` (or `fix/<name>`, `docs/<name>`...). Never work on `main`.
3. Write the code following [docs/CONVENTIONS.md](docs/CONVENTIONS.md). New features get a package under
   `feature/` with a `Module` object, a `*Logic.kt` for the reasoning and a test next to the logic.
4. Run `./gradlew spotlessApply check`. If it fails, read what it says: it is the same command CI runs.
5. For UI changes, run the game (`./gradlew runClient`) and look at your change. Screenshots in the pull request are
   how a UI review starts.
6. Push the branch and open the pull request (`gh pr create --fill` works). Say what it does, how you tested it and
   which Hypixel tier it has.
7. When the `check` workflow is green, merge. One commit per pull request gets squashed; a pull request with several
   logical commits is merged as it is.

## Reporting bugs

Use the [bug report](https://github.com/bgrados31/skyatlaz/issues/new?template=bug_report.yml) template. A capture
made with `F7` and the `latest.log` turn "it does not work" into a bug we can fix.

## Translations

`en_us.json` is the source of truth. A new feature adds its keys there first; a new language is a new file in
`assets/skyatlaz/lang/`. See [docs/I18N.md](docs/I18N.md).

## License

By contributing, you agree that your contribution is licensed under the same license as the mod
([LGPL-3.0-or-later](LICENSE)).
