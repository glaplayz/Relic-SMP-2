# RelicBearers

A PvE-first relic power system for Paper/Purpur 1.21+ servers. PvP is on
server-wide, and killing another player is the *only* way to claim a relic
you don't already have.

## How it plays

- Every player is randomly assigned 1 of 8 relics on first join (Ember,
  Tide, Growth, Gust, Stone, Glimmer, Void, Aurum).
- Hold your relic item in your **offhand**, then **sneak + right-click**
  to activate its ability (costs Charge, has a cooldown).
- Charge regens passively over time and gets bonus refills from PvE
  actions (mining ore, killing mobs, farming, fishing, taming, advancements).
- Those same PvE actions grant lifetime **Mastery XP** for your relic,
  which tiers it up (Tier 1 -> 2 -> 3), making its ability stronger and
  its cooldown shorter. This part is 100% soloable, no PvP required.
- **PvP is on by default** (`global-pvp-enabled: true` in config.yml).
  Kill another player and you claim their relic at **Tier 1**, as long as
  you don't already own it and their relic was Tier 2+ (configurable via
  `kill.min-tier-for-relic-drop` — keeps people from farming brand-new
  spawns). There's no duel requirement, no cooldown per victim, and
  nothing decays — a clean kill is all it takes.
- Owning multiple relics: use `/relic switch <type>` to swap which one
  is in your offhand. Only your currently-equipped relic gains XP/Charge
  and can be activated — so even a "completionist" has to choose what
  to actively use at any given time.
- **Reroll**: don't like the relic you got? Craft it together with the
  catalyst item (default: 3x Ender Eye) in a crafting table. The result
  slot shows a "???" placeholder until you take it — that's when a new
  random relic type is rolled and your old relic's Tier/XP/Charge
  progress is reset. It'll never reroll into a relic you already own.

## Commands

| Command | Description |
|---|---|
| `/relic info` | Your owned relics, tiers, XP, and charge |
| `/relic list` | List all 8 relic types and what they do |
| `/relic switch <type>` | Equip a relic you already own |
| `/relic give <player> <type>` | *(admin)* grant a relic item/ownership directly |

## Building

Requires JDK 21 and Maven. Paper's API is pulled from their public repo
(already configured in `pom.xml`), so you'll need internet access when
building.

```bash
mvn clean package
```

The compiled plugin will be at `target/RelicBearers.jar`. Drop it into
your server's `plugins/` folder and restart.

## Config

See `src/main/resources/config.yml` — every number mentioned above
(XP amounts, charge costs, cooldowns, the min tier required for a kill
to drop a relic, and the reroll catalyst material/amount) is tunable
there without touching code. Set `global-pvp-enabled: false` if you
ever want to shut PvP off entirely (relics will then only spread via
`/relic give` from an admin).

## Recent bug fixes

- **Abilities weren't activating at all.** The sneak+right-click detection
  only listened for the `MAIN_HAND` variant of `PlayerInteractEvent`, but
  which hand actually fires that event depends on what's in your main hand
  and isn't reliable to predict. `AbilityListener` now activates on whichever
  hand's event actually arrives (with a same-tick dedupe so it never
  double-fires).
- **The reroll recipe didn't work.** It was registered as a formal Bukkit
  `ShapelessRecipe` with `RecipeChoice.ExactChoice`, which is unreliable for
  items carrying custom persistent data across Paper/Spigot versions.
  `RerollListener` now checks the crafting grid's contents directly
  (`PrepareItemCraftEvent`/`CraftItemEvent`) instead of depending on a
  registered recipe matching.
- **Relic items are now undroppable, unplaceable, and unthrowable.** Ember,
  Tide, Glimmer, and Aurum's base materials never had a vanilla action to
  begin with, but Growth (sapling), Stone (cobblestone), and Void (ender
  pearl) did - `RelicProtectionListener` now blocks `PlayerDropItemEvent`,
  `BlockPlaceEvent`, and `PlayerLaunchProjectileEvent` for any item carrying
  relic data, on top of the interact-event cancellation.
- **Dying no longer costs you your own relic.** Death drops are stripped of
  relic items and the item is handed back on respawn - your server-side
  ownership was never touched by dying in the first place, only by
  `RelicManager#awardRelicFromKill` when someone kills *you*.


Each relic's active ability lives in its own class under
`relic/abilities/` implementing `RelicAbility`. To add a 9th relic:
add an entry to `RelicType`, write a class implementing `RelicAbility`,
and register it in `RelicManager`'s constructor. Tier scaling is handled
inside each ability by reading the `tier` (1-3) parameter passed to
`activate()`.

## Known limitations / TODO for production use

- No anti-dupe protection on relic items yet (a player could in theory
  drop/store multiple copies of the same relic item — ownership itself
  is still tracked server-side per profile, so this is cosmetic, but
  you may want to block dropping/storing relic items in chests).
- No WorldGuard region integration (e.g. disabling PvP/abilities in
  spawn) — straightforward to add by checking a region flag inside
  `PvpListener` and `tryActivate`.
- No GUI; everything is chat-command based.
- Death drops: relic items themselves still drop/keep on death per
  normal Minecraft rules unless you add keepInventory or a custom drop
  handler — the relic *ownership* transfer on kill is independent of
  whether the physical item stays in the world.
