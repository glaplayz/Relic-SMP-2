# ImposterPlugin

A "10 YouTubers vs 1 Secret Liar"-style Among Us minigame for **Paper 1.21.11** (Java 21).

## Features implemented

- **Secret role assignment** — choose how many hidden Imposters there are per game (default 1, configurable, or set per-round via command); everyone else is a Crewmate. Multiple Imposters are told who their fellow Imposters are.
- **Shared crew task** — right-click a Villager while in **The End**, holding **Diamond Leggings** with a **Netherite armor trim** and **Swift Sneak III**, to equip it and complete the task.
- **Emergency meeting bells** — every player gets one Bell item (configurable) that calls a meeting when right-clicked. Consumed on use.
- **Voting** — calling a meeting teleports everyone to a voting room, strips their inventory (restored after), waits a short "discussion" period, then opens an interactive GUI (player heads + Skip) for everyone at once. Votes are tallied; the top vote gets ejected (ties/skips eject no one).
- **Win conditions**
  - Imposter(s) win immediately if all crewmates are eliminated before the task is done.
  - Crewmates win once the task is complete **and** every Imposter has been ejected.
- **Chat is completely and unconditionally disabled**, at all times, for every player — not just during a match. Enforced at `MONITOR` priority with `ignoreCancelled=false` so it's cancelled last no matter what any other plugin does to the event, across:
  - Paper's native `AsyncChatEvent` (what actually fires for normal typed chat on modern Paper)
  - The legacy Bukkit `AsyncPlayerChatEvent` (in case anything re-routes through it)
  - `/say` and `/me` commands
  - Death messages (both the `PlayerDeathEvent` message field is nulled, and kills are dealt as silent cancelled damage rather than a real death, so there's no combat log either)

## Project layout

```
imposter-plugin/
├── pom.xml
└── src/main/
    ├── java/com/imposter/
    │   ├── ImposterPlugin.java        (main class)
    │   ├── game/
    │   │   ├── GameManager.java       (state, roles, win logic)
    │   │   ├── Meeting.java           (meeting/voting flow)
    │   │   ├── GameState.java
    │   │   └── Role.java
    │   ├── listeners/
    │   │   ├── PlayerListener.java    (chat disable, death msg, PvP kills)
    │   │   ├── MeetingListener.java   (bell right-click)
    │   │   ├── TaskListener.java      (villager equip task)
    │   │   └── VotingGUIListener.java
    │   ├── gui/
    │   │   ├── VotingGUI.java
    │   │   └── VotingInventoryHolder.java
    │   ├── commands/
    │   │   └── ImposterCommand.java
    │   └── util/
    │       └── ItemUtil.java
    └── resources/
        ├── plugin.yml
        └── config.yml
```

## Building

This targets **Paper API 1.21.11** and requires **Java 21** to compile and run.
My sandbox can't reach `repo.papermc.io` to download that dependency and compile
it for you, so build it yourself:

```bash
cd imposter-plugin
mvn clean package
```

The shaded jar will be at `target/ImposterPlugin.jar`. Drop it in your server's
`plugins/` folder (running on Paper 1.21.11, Java 21) and restart.

If you're targeting a different Minecraft version, change the `paper-api`
version in `pom.xml` and the `api-version` in `plugin.yml` to match.

## Setup on your server

1. Start the server once with the plugin installed so it generates
   `plugins/ImposterPlugin/config.yml`.
2. Edit `config.yml`:
   - `arena-world` — the world your game runs in.
   - `game-spawn` — where players spawn when a round starts.
   - `voting-room` — a small enclosed room/arena for meetings (build one — a
     circular room with a table works great, matching the original format).
3. You'll separately need an End dimension accessible with villagers present
   (or spawn one in) for the crew task.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/imposter join` | `imposter.play` | Join the lobby |
| `/imposter leave` | `imposter.play` | Leave the lobby |
| `/imposter start [imposters]` | `imposter.admin` | Start the game with everyone in the lobby. Optional argument sets how many secret Imposters this round has (defaults to `default-imposters` in config.yml) |
| `/startimposter [imposters]` | `imposter.admin` | Dedicated shortcut for starting — same as `/imposter start [imposters]` |
| `/imposter stop` | `imposter.admin` | Force-stop the current game |
| `/imposter status` | any | Show current game state / lobby size |
| `/imposter give-task-item` | `imposter.admin` | Gives yourself a correctly-enchanted reference legging (for testing) |

Aliases: `/imposter` → `/imp`, `/liar`. `/startimposter` → `/startliar`, `/startgame`.

Examples:
```
/imposter start          -> uses default-imposters from config.yml
/imposter start 2        -> starts with 2 secret Imposters
/startimposter 3         -> shortcut, starts with 3 secret Imposters
```

The plugin validates the count: it must be at least 1, and strictly less than
the number of players in the lobby (so there's always at least one Crewmate).

## Notes / things you may want to tune

- Chat disable is intentionally global and always-on per your request — if you'd
  rather it only apply while a game is `RUNNING`/`MEETING`, add a state check
  in `PlayerListener`'s chat handlers.
- Only Imposters' melee hits are lethal to crewmates (`PlayerListener.onDamage`
  checks the damager's role) — crewmates and other crewmates can't accidentally
  eliminate each other, and with multiple Imposters, any of them can kill.
- The voting GUI currently lists **every** alive player (including the voter) as
  a candidate, matching the "everyone's a suspect" spirit of the format.
- Meeting timing (`meeting-discussion-seconds`, `meeting-voting-seconds`) and
  `bells-per-player` are all in `config.yml`.
