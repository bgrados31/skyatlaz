---
name: skyblock-refs
description: Use when you need to know how other Hypixel SkyBlock mods (Skyblocker, SkyHanni, Firmament), the Hypixel Mod API, the Hypixel public API (v2) or NEU-REPO data solve or represent something (chat patterns, scoreboard parsing, item ids, dungeon logic, mixin targets). Token-efficient lookup in references/ and docs/api/.
---

# Looking things up in reference mods

`references/` holds shallow clones (git-ignored, read-only). Each repo has an `INDEX.md` with one line per
source file: `path | declared types`.

| Folder | What | Language |
|---|---|---|
| `references/skyblocker` | Skyblocker (Fabric, 26.2) | Java |
| `references/skyhanni` | SkyHanni (big feature set, many chat regexes) | Kotlin |
| `references/firmament` | Firmament (clean modern Fabric code) | Kotlin |
| `references/hypixel-mod-api` | Official Mod API: packets and fields | Java |
| `references/neu-repo` | Item/mob/constant data (JSON) | Data |
| `docs/api/INDEX.md` | The Hypixel public API (v2): the endpoints, the key and the shape of the answers (the spec is `docs/api/hypixel-swagger.json`) | Doc |

If `references/` is missing or stale: `node tools/refs/sync.mjs` (or `node tools/refs/sync.mjs skyhanni`).

## Procedure (keep token use low)
1. **Grep the index first**, never read it whole:
   `Grep pattern="(?i)dungeon.*score" path="references/skyhanni/INDEX.md"`
2. Grep inside the candidate files for the specific thing (regex, function name) with a few lines of context.
3. Read only the needed line range (`Read` with offset/limit). Never read entire large files.
4. Compare 2 mods when it matters; note which approach is cheaper at runtime.

## Rules
- **Never copy code.** The references are LGPL; skyatlaz is private. Understand the approach, then write our own
  implementation that fits our architecture (events, shared state, theme, i18n).
- Data facts (item ids, chat message formats, coordinates) are fine to reuse as data; still write our own code.
- Chat formats found here become test fixtures in `src/test/resources/fixtures/`.
- Never edit files in `references/` (denied by settings).
