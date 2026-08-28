# ImposterPlugin

A "10 YouTubers vs 1 Secret Liar"-style Among Us minigame for **Paper 1.21.11** (Java 21).

## Features implemented

- **Secret role assignment** — choose how many hidden Imposters there are per game (default 1, configurable, or set per-round via command); everyone else is a Crewmate. Multiple Imposters are told who their fellow Imposters are.
- **Shared crew task** — right-click a Villager while in **The End**, holding **Diamond Leggings** with a **Netherite armor trim** and **Swift Sneak III**, to equip it and complete the task.
- **Emergency meeting bells** — every player gets one Bell item (configurable) that calls a meeting when right-clicked. Consumed on use. **Locked out for a few seconds (configurable) after being hit by the Imposter**, so a victim can't bell-escape mid-attack.
- **Imposter's one-shot weapon** — Imposters spawn with a "Silent Blade" that instantly eliminates any crewmate it hits, regardless of their current health - no combat skill/health-whittling required. It's on a **60-second cooldown (configurable)** between kills; swinging it while on cooldown does nothing. Ordinary weapons/fists never kill during a game (damage that would be lethal is clamped) — the Silent Blade is the only way to eliminate someone.
- **Voting** — calling a meeting teleports everyone to a voting room, strips their inventory (restored after), waits a short "discussion" period, then opens an interactive GUI (player heads + Skip) for everyone at once. Votes are tallied; the top vote gets ejected (ties/skips eject no one).
- **Win conditions**
  - Imposter(s) win immediately if all crewmates are eliminated before the task is done.
  - Crewmates win once the task is complete **and** every Imposter has been ejected.
- **Chat works normally** — players can talk freely.
- **Death messages are always suppressed**, even though chat itself is on: eliminations are dealt as silent, cancelled damage (health manually zeroed) rather than triggering a normal combat-log death, and every `PlayerDeathEvent` has its message cleared via `deathMessage(null)`, `setShowDeathMessages(false)`, and the legacy `setDeathMessage(null)` for good measure.

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
    │   │   ├── PlayerListener.java    (death msg suppression, attack tracking)
    │   │   ├── MeetingListener.java   (bell right-click)
    │   │   ├── TaskListener.java      (villager equip task)
    │   │   ├── ImposterWeaponListener.java (Silent Blade one-shot + cooldown)
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
| `/imposter give-weapon` | `imposter.admin` | Gives yourself a Silent Blade for testing |

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

- Chat is fully open now (no cancellation of any chat events).
- Adventure Mode is only applied to players during an active emergency meeting
  (`Meeting.begin()`), and is restored to Survival when the meeting concludes,
  either normally (everyone votes / the timer runs out) or via `/imposter stop`
  mid-meeting, which now also aborts and restores the meeting cleanly instead
  of leaving people stuck.
- **Only the Silent Blade kills.** Ordinary combat (fists, any other weapon)
  between game participants is capped so it can never actually finish someone
  off - `PlayerListener.onDamage` zeroes out any hit that would otherwise be
  lethal. This keeps eliminations tied to the one-shot weapon + cooldown rather
  than who wins a drawn-out fight.
- **Meeting lockout while under attack.** Any hit from an Imposter (whether or
  not it's the Silent Blade) marks the victim as "recently attacked"; they can't
  call an emergency meeting for `meeting-lockout-after-hit-seconds` (default 5)
  afterward, so they can't bell-teleport away mid-kill.
- The voting GUI currently lists **every** alive player (including the voter) as
  a candidate, matching the "everyone's a suspect" spirit of the format.
- Meeting timing (`meeting-discussion-seconds`, `meeting-voting-seconds`),
  `bells-per-player`, `weapon-cooldown-seconds`, and
  `meeting-lockout-after-hit-seconds` are all tunable in `config.yml`.
