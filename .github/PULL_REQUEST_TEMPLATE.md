## What this pull request does

<!-- One or two sentences. What it does, and for whom. -->

## Hypixel tier

<!-- Allowed / Gray / Forbidden, with the rule that decides it (docs/HYPIXEL_RULES.md). Forbidden does not merge. -->

## How it was tested

<!-- `./gradlew check` at minimum. UI changes: `./gradlew runClient` and a screenshot. -->

## Checklist

- [ ] `./gradlew check` is green (format, compile, unit and architecture tests).
- [ ] Every user-facing string goes through `Tr.tr` and has a key in `en_us.json`.
- [ ] Colors and spacing come from `ui.theme`, never raw ARGB in a feature.
- [ ] Features do not import other features.
- [ ] Logic lives in `*Logic.kt` with tests next to it.
- [ ] A disabled feature registers no listener, no task and no download.
- [ ] Docs updated (`docs/`, `en_us.json`, or this template itself) if the behavior changed.
- [ ] Screenshots attached if anything on screen changed.
