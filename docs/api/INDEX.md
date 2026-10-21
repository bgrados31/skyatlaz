# Hypixel public API (v2) index

The official spec is `docs/api/hypixel-swagger.json` (OpenAPI 3.0.3, the file Hypixel publishes for the developer
dashboard). This file is the index: what each endpoint gives, which ones need a key and how the answers look. Grep
here before reading the spec.

**SkyAtlaz does not use this API.** The policy of Hypixel about the keys (below) makes it unusable for a mod: the
key of the developer can never travel inside the mod, and asking every player to register an application to paste
their own key is not a thing a mod can ask. The file is here to know what exists and why we do not use it.

## The rules of Hypixel about the keys (https://developer.hypixel.net/policies)

- **"Do not share your API key with 3rd parties, such as entering them into a website or a mod. This applies to any
  publicly available mod, regardless of it being allowed on the Hypixel Server."** and **"Do not include your API key
  in your code, especially if you plan to distribute a binary or have the code available publicly"**.
  → The key of the developer of a mod can never be inside the mod. A key per player would be legal on paper, but it
  asks every player to register an application in the developer portal, so the mod cannot depend on it.
- **"All projects that make use of authenticated API endpoints must be registered in our Hypixel Developer Portal"**
  and **"Each project should be registered as its own application"**.
  → Even with a key of our own, the mod would have to be a registered application.
- **"Users should not rely solely on developer keys for long-term use, developer keys may expire sooner based on
  heavy long-term usage."**
  → A key is not something a feature can count on.
- **"Projects should utilize caching to the best of their abilities to reduce requests on the API."** and **"Our API
  is designed to be used by players, and not for automated data collection at scale."**
  → The mod reads the game, not the API: the tree of the Heart of the Mountain is read out of the menu that the
  player has open (`feature/mining/hotm`), which is the same information the game shows.
- **"You may not create an application for the purpose of proxying the Public API to 3rd party developers."**
  → No proxy of ours in the middle either.

What the mod uses instead:
- The **lore of the open menu** for the perks of the tree (`feature/mining/hotm/HotmLogic`): the level, the maximum
  and the state of every perk the menu shows.
- The **community layout** of the tree (`constants/hotmlayout.json` in NEU-REPO, fetched on demand like the item
  pages) for the names and the maximum levels of all the perks: `api/repo/HotmLayout.kt`.

## Basics

- Base URL: `https://api.hypixel.net`. Every path below is relative to it.
- **Authentication**: the header `API-Key: <key>`, taken from the developer dashboard
  (https://developer.hypixel.net). Endpoints marked *key* refuse the call without it:
  `{"success": false, "cause": "Missing API-Key header"}`.
- **Rate limits**: per key, "a maximum amount of requests per 5 minute intervals" (the exact limit depends on the
  application). The answers carry `RateLimit-Limit`, `RateLimit-Remaining` and `RateLimit-Reset` headers.
- The old v1 endpoints are gone: `/skyblock/profile/<uuid>` answers `{"success":false,"cause":"Unknown endpoint"}`.
  Everything is v2 now.
- `uuid` parameters accept the dashed and the undashed form.
- The policies: https://developer.hypixel.net/policies

## Endpoints

| Method | Path | Gives | Key | Parameters |
|---|---|---|---|---|
| GET | `/v2/player` | The data of a player, with the stats of every game | yes | `uuid` |
| GET | `/v2/recentgames` | The games the player played recently | yes | `uuid` |
| GET | `/v2/status` | Whether the player is online, and where | yes | `uuid` |
| GET | `/v2/guild` | A guild, by id, player or name | yes | `id`, `player`, `name` |
| GET | `/v2/leaderboards` | The current leaderboards | yes | - |
| GET | `/v2/counts` | The players online right now | yes | - |
| GET | `/v2/boosters` | The active network boosters | yes | - |
| GET | `/v2/punishmentstats` | The punishment statistics | yes | - |
| GET | `/v2/housing/active` | The public houses that are active | yes | - |
| GET | `/v2/housing/house` | One house | yes | `house` |
| GET | `/v2/housing/houses` | The public houses of a player | yes | `player` |
| GET | `/v2/skyblock/profiles` | The SkyBlock profiles of a player | yes | `uuid` |
| GET | `/v2/skyblock/profile` | One SkyBlock profile, by its id | yes | `profile` |
| GET | `/v2/skyblock/museum` | The museum of a profile | yes | `profile` |
| GET | `/v2/skyblock/garden` | The garden of a profile | yes | `profile` |
| GET | `/v2/skyblock/bingo` | The bingo data of a player | yes | `uuid` |
| GET | `/v2/skyblock/auction` | One auction, by its uuid, the player or the profile | yes | `uuid`, `player`, `profile` |
| GET | `/v2/skyblock/auctions` | The active auctions | no | `page` |
| GET | `/v2/skyblock/auctions_ended` | The auctions that ended recently | no | - |
| GET | `/v2/skyblock/bazaar` | The bazaar and its prices | no | - |
| GET | `/v2/skyblock/firesales` | The fire sales that are active or coming | no | - |
| GET | `/v2/skyblock/news` | The news of SkyBlock | no | - |
| GET | `/v2/resources/games` | The games of Hypixel | no | - |
| GET | `/v2/resources/achievements` | The achievements | no | - |
| GET | `/v2/resources/challenges` | The challenges | no | - |
| GET | `/v2/resources/quests` | The quests | no | - |
| GET | `/v2/resources/guilds/achievements` | The guild achievements | no | - |
| GET | `/v2/resources/vanity/pets` | The vanity pets | no | - |
| GET | `/v2/resources/vanity/companions` | The vanity companions | no | - |
| GET | `/v2/resources/packs` | The active resource packs | no | - |
| GET | `/v2/resources/skyblock/collections` | The collections | no | - |
| GET | `/v2/resources/skyblock/skills` | The skills | no | - |
| GET | `/v2/resources/skyblock/items` | The items | no | - |
| GET | `/v2/resources/skyblock/election` | The election and the mayor | no | - |
| GET | `/v2/resources/skyblock/bingo` | The current bingo event | no | - |

## The shape of a profile

`GET /v2/skyblock/profiles?uuid=<uuid>` answers `{"success": true, "profiles": [ ... ]}`. Every profile holds
`profile_id`, `cute_name`, `selected`, `community_upgrades`, `banking`, `game_mode` and `members`, a map keyed by the
uuid of each member (a co-op profile has more than one). The member is free-form; the paths SkyAtlaz reads:

| Path | What |
|---|---|
| `skill_tree.nodes.mining_1` ... `mining_5` | The five sub-trees of the mining tree: `<perk_id>` -> level, `toggle_<perk_id>` -> on/off |
| `skill_tree.selected_ability.mining_<slot>` | The pickaxe ability that is equipped |
| `skill_tree.selected_skill_tree_slot.mining` | Which sub-tree the player has selected (1..5) |
| `skill_tree.tokens_spent.mountain_<slot>` | The mountain tokens spent in that sub-tree |
| `skill_tree.experience.mining` | The mining experience |
| `mining_core.powder_mithril` / `_gemstone` / `_glacite` | The powder that is left |
| `dungeons` | The dungeon statistics of the player |
| `currencies` | The purse, the bank and the other currencies |
| `collection`, `items`, `inventory` | The collections and the items (base64 + gzipped NBT) |

The perk ids are the ones of the NEU-REPO layout (`constants/hotmlayout.json`), so a perk can be read by id and
shown with the name and the maximum level of the layout. The uuid of the player comes from the Mojang API with the
name of the session (`https://api.mojang.com/users/profiles/minecraft/<name>` -> `id`, without dashes), the same way
Skyblocker does it.

## What the API would give (not used)

- `/v2/skyblock/profiles` - the whole profile of the player: the mining tree, the dungeons, the currencies.
- `/v2/skyblock/museum` and `/v2/skyblock/garden` - the museum and the garden of a profile.
- `/v2/player`, `/v2/status`, `/v2/recentgames` - the data of a player and whether it is online.
- `/v2/skyblock/bazaar`, `/v2/skyblock/auctions` - the prices, without a key.
