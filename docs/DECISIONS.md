# Decision log

Newest first. Format: date, decision, why, consequences.

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
