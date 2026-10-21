# Security policy

## Supported versions

Only the latest release of skyatlaz receives fixes. There is no long-term support branch, and there will not be one
until there is a reason to.

## What counts as a security bug

- Anything that lets one player see, change or download the data of another player through the mod or the optional
  service.
- Anything that lets a caller of the service forge the proof of identity (the Minecraft profile key signature) and
  read a profile that is not theirs.
- Anything that leaks a token, an API key or the session of a player.
- A crash or a freeze triggered from the outside (a chat message, a scoreboard line, an item name) counts, because
  on Hypixel the chat and the scoreboard are shared with strangers.

It does not count: a bug in a feature's math, a HUD that overlaps another HUD, or a crash you can only trigger with
your own files.

## Reporting a vulnerability

Do not open a public issue. Use one of these, in this order:

1. GitHub's **private vulnerability reporting** on this repository, if it is enabled: *Security* > *Report a
   vulnerability*.
2. A direct message to **bgrados31** on GitHub.

Give us what you have: what happens, how you reproduced it, which version, and the log. You will get an answer
acknowledging the report as soon as it is read, and a fix or an estimate before anything is disclosed. Please do not
publish the details until a fixed version is out; a coordinated disclosure protects the players who have not
updated.

## Scope notes for the service

The optional service answers only the player who authenticated, and it is the only holder of an Hypixel API key. A
report that reaches another player's profile, or that reaches the key, is the most serious kind. The service must
never be reachable from the internet by default without the owner knowing it: it binds to localhost and the
documentation does not tell you to open it.
