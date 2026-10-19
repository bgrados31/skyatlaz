# Hypixel rules for features

No mod is officially approved by Hypixel and the rules can change. Source of truth:
https://hypixel.net/allowed-mods (re-check before each release). When in doubt, do not build it.

## Tiers
| Tier | Meaning | Examples | Policy |
|---|---|---|---|
| Allowed | Shows information the player already has, or improves menus/visuals | HUDs, timers, reminders, calculators, price tooltips, chat filters, inventory search, item highlighting in menus | Build normally |
| Gray | Information the player could not easily see, commonly used by popular mods | Entity highlight through walls, dungeon puzzle solvers, waypoints | Off by default, marked "use at your own risk" in config |
| Forbidden | Automation or unfair advantage | Macros, auto-click, auto-anything, packet modification/spoofing, freecam, freelook, movement changes, cancelling world interactions (block/entity clicks), reach/hitbox changes, x-ray | Never build |

## Checklist before writing a feature
1. Does it send any input/packet the player did not trigger? -> Forbidden.
2. Does it change movement, camera or interaction with the world? -> Forbidden.
3. Does it reveal information not visible to the player? -> Gray at best.
4. Only reads/displays existing info or reorganizes UI? -> Allowed.

Record the tier in the feature's KDoc and in its config description.

## Development tooling
Agent tooling that drives input (MCP Fabric in `run/agent-mods`) is loaded only by `./gradlew runWorld`
(singleplayer). Never add it to `runClient`/`runHypixel`, the mod's dependencies or a release jar, and never
connect to Hypixel from a client that has it loaded.

## Minecraft version policy
Hypixel SkyBlock only accepts the latest two Minecraft content updates. We target the newest version accepted
by Hypixel and the Hypixel Mod API (currently 26.2). Bump when Hypixel announces a new one.
