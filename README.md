# Arnost: Legend of the Fallen Keep

A turn-based 2d6 dungeon crawler for the terminal. Explore three levels of the
Fallen Keep, recover the Dungeon Keys, draw Excalibur, and face the dragon
Ashgorath in his lair.

**Build status:** passing (58 tests)  
**Target:** Java 27  
**Build:** Apache Maven  
**Interface:** ANSI terminal, 80 columns

---

## Contents

- [Overview](#overview)
- [Story](#story)
- [Features](#features)
- [Game data](#game-data)
- [Requirements](#requirements)
- [Compile and package](#compile-and-package)
- [Run the game](#run-the-game)
- [Controls](#controls)
- [Game rules](#game-rules)
- [Gear](#gear)
- [Map legend](#map-legend)
- [Save and load](#save-and-load)
- [Testing](#testing)
- [Project layout](#project-layout)
- [Credits](#credits)
- [Troubleshooting](#troubleshooting)

---

## Overview

Arnost is a small, self-contained Java terminal game. Each run generates three
7x7 dungeon levels. The player starts at `[3,6]` with 3/5 HP, explores the
keep, fights monsters with a 2d6 roll plus gear bonuses, and collects the quest
items needed to descend.

The game is deliberately playable without a GUI, a database, or an external
service. It runs in a terminal, saves automatically, and includes story
cutscenes between the levels.

---

## Story

The Fallen Keep has stood silent for a hundred years, its gates sealed beneath
ash and ivy. Sir Arnost, last knight of the broken order, rides through the mist
with a worn blade and a vow he cannot break.

Two Dungeon Keys wait behind hidden guardians. On the third level, the holy
blade Excalibur lies hidden, and at the bottom of the Dragon's Abyss, Ashgorath
guards the lost Crown Jewels.

The road leads through the keep's gate, into the venomous dark of the Sunken
Crypts, and finally into the Dragon's Abyss. Sir Arnost must find the keys,
survive the guardians, claim Excalibur, and end the curse of the Fallen Keep.

The game opens with a prologue. Descending after Levels 1 and 2 reveals
additional story cutscenes, and a winning run ends with an epilogue.

---

## Features

- Three handcrafted 7x7 dungeon levels with a guaranteed reachable path.
- Turn-based combat using two six-sided dice plus the player's gear bonus.
- Hidden monster strength, scaling opposition, and stronger loot from stronger
  enemies.
- Two Dungeon Keys, the Knight Guardian, Excalibur, and the dragon finale.
- A three-life dragon boss with a dedicated strike rule.
- Health potions, hidden traps, venomous monsters, and pushback on failed
  attacks.
- Automatic save/load with optional manual save and title-screen load.
- Story prologue, level-transition cutscenes, and a victory epilogue.
- A colored, split-pane 80-column ANSI interface with ASCII art.
- Data-driven items, monsters, and story content stored in bundled JSON files.
- Automated tests covering combat, map generation, rendering, saves, story
  flow, and a complete winning playthrough.

---

## Game data

The editable content lives in three JSON resources:

- `src/main/resources/data/items.json` - item names, bonuses, colors, art,
  lootable order, and quest-item flags;
- `src/main/resources/data/monsters.json` - monster stats, art, lore, level
  strengths, spawn pools, fixed placements, loot tables, and dragon rules;
- `src/main/resources/data/story.json` - prologue, level-transition
  cutscenes, arrival messages, and the victory epilogue.

The files are copied into the jar by Maven and loaded once at startup by the
small built-in `Json` reader. There is no external JSON dependency, so the
game still runs from the thin executable jar. Edit the JSON, then rebuild and
rerun the tests; the files are the source of truth for the content they hold.

---

## Requirements

- **JDK 27** or newer. The Maven compiler is configured with
  `<release>27</release>`.
- **Apache Maven** (3.9 or newer is recommended).
- A terminal that supports ANSI escape sequences and UTF-8 output.
- At least 80 columns of terminal width for the intended layout.

Check the installed tools:

```bash
java -version
javac -version
mvn -version
```

`JAVA_HOME` must point to a JDK 27 installation. A Java runtime alone is not
enough to build the project.

The first Maven build downloads JUnit Jupiter 5.11.0 from Maven Central. The
game itself has no runtime dependencies outside the JDK.

---

## Compile and package

From the repository root:

```bash
# Compile the production sources only
mvn clean compile

# Compile, run the test suite, and build the executable jar
mvn clean package
```

The package command creates:

```text
target/arnost-1.0.0-SNAPSHOT.jar
```

The jar manifest points to `com.arnost.game.Main`, so it can be run directly
with `java -jar`.

Useful build variants:

```bash
# Build the jar without running tests
mvn -DskipTests package

# Compile and test without packaging
mvn clean test

# Remove generated build output
mvn clean
```

The build uses `maven-compiler-plugin` 3.13.0 with Java release 27 and
`maven-jar-plugin` 3.4.2 with `com.arnost.game.Main` as the jar entry point.
The result is a normal (non-fat) jar; the game has no runtime libraries beyond
the JDK.

The same Maven commands work in Windows PowerShell, Command Prompt, and a Unix
shell. The repository does not include a Maven wrapper, so use an installed
`mvn` command.

---

## Run the game

From the repository root, after packaging:

```bash
java -jar target/arnost-1.0.0-SNAPSHOT.jar
```

If a local `jpackage` build is present, the Windows application can also be
started from:

```text
dist\arnost\arnost.exe
```

`dist/` is a local build artifact and is intentionally ignored by Git. Build
it again after changing the source if you use that launcher.

The game reads commands from standard input and writes its interface to
standard output. Start a new run by pressing `ENTER` on the title screen, or
press `L` to load an existing save.

For a clean console on Windows PowerShell, UTF-8 output is recommended:

```powershell
chcp 65001
java -jar target/arnost-1.0.0-SNAPSHOT.jar
```

---

## Controls

### Title screen

| Input | Action |
| --- | --- |
| `ENTER` | Start a new game |
| `L` | Load the saved run, if one exists |

### Story screens

| Input | Action |
| --- | --- |
| `ENTER` | Continue past a prologue, cutscene, or epilogue |

### In the dungeon

| Command | Action |
| --- | --- |
| `n`, `s`, `e`, `w` | Move one tile north, south, east, or west |
| `go n`, `go s`, `go e`, `go w` | Move one tile in that direction |
| `north`, `south`, `east`, `west` | Alternate direction names |
| `left`, `right`, `up`, `down` | Alternate direction names |
| `gon`, `gos`, `goe`, `gow`, `gol` | Compact movement commands |
| `roll` | Fight the monster on the current tile |
| `drink`, `take potion`, `use potion` | Drink a Health Potion on the current tile |
| `descend`, `go stairs`, `stairs` | Use an Exit Staircase `[E]` |
| `save` | Write the current run to the save file |
| `help` | Show the in-game help screen |
| `quit` | Save and leave the keep |

### Defeat screen

| Input | Action |
| --- | --- |
| `r`, `restart` | Start a new run |
| `q`, `quit` | Exit the game |

The help screen is also available in-game and is the authoritative quick
reference for commands and rules.

---

## Game rules

### Movement and exploration

- Each level is a 7x7 grid. The player starts at `[3,6]`.
- The tile under the player and the four adjacent chambers are revealed as the
  player moves.
- Walls `[#]` block movement.
- Unexplored tiles show `[?]`; explored empty tiles show `[.]`.
- The player cannot walk onto the dragon's lair until the dragon is eligible to
  awaken.

### Combat

The player's attack is:

```text
Attack = 2d6 + combat bonus from gear
```

Resolve it against the monster's strength:

| Result | Outcome |
| --- | --- |
| `Attack > monster strength` | Victory: the monster is slain and may drop loot |
| `Attack = monster strength` | Draw: the player is pushed back and loses no HP |
| `Attack < monster strength` | Defeat: the player loses 1 HP and is pushed back |

Some monsters hide their strength as `???` until the player rolls. Venomous
monsters `[V]` deal 1 additional HP of damage when they defeat the player.
Monster strength scales with the player's growing combat bonus: every +3
combat bonus adds +1 STR to encountered monsters.

### Dungeon Keys and the exit

- A hidden guardian on Level 1 holds the first Dungeon Key.
- A hidden guardian on Level 2 holds the second Dungeon Key.
- The Exit Staircase is locked until the current level's key is collected.
- Using the staircase consumes the key and descends to the next level.

### Excalibur and the dragon

- The Knight Guardian on Level 3 holds Excalibur.
- Excalibur grants +4 combat bonus and is the only weapon that can wound
  Ashgorath.
- The dragon lair is at `[3,1]` on Level 3.
- The dragon awakens when the player has at least +4 combat bonus.
- **Ashgorath has 3 lives.**
- Every strike must roll a **2d6 sum of 9 or more** to remove one life.
- A roll of **8** is a draw: the player is pushed back and no life is lost.
- A roll of **7 or lower** is a defeat: the player loses 1 HP and is pushed
  back.
- The third successful strike slays the dragon and awards the Crown Jewels.
- Remaining dragon lives are visible in the encounter panel and persist across
  saves.

### Win and loss conditions

- The game is won by landing the third successful strike on Ashgorath.
- The game ends in defeat when the player's HP reaches 0.
- On victory or death, the save file is removed so the finished run cannot be
  resumed.

---

## Gear

Combat bonuses from carried gear:

| Item | Bonus |
| --- | ---: |
| Iron Dagger | +1 |
| Short Sword | +1 |
| Leather Armor | +1 |
| Silver Ring | +1 |
| Cavalry Saber | +2 |
| Fireball Scroll | +1 |
| War Axe | +3 |
| Iron Halberd | +3 |
| Runeblade | +4 |
| Excalibur | +4 |

Gear is found on `[I]` item tiles or dropped by defeated monsters. Stronger
monsters generally carry better loot. Quest items (`Dungeon Key` and
`Excalibur`) are handled separately from ordinary loot.

Potions restore 2 HP up to the maximum of 5. The player begins each run at
3/5 HP. Walking onto a potion tile reveals the `[P]` marker; type `drink` to
consume it.

---

## Map legend

| Marker | Meaning |
| --- | --- |
| `[*]` | The player |
| `[S]` | Starting tile |
| `[I]` | Item or gear |
| `[P]` | Health Potion |
| `[E]` | Exit Staircase |
| `[D]` | Dragon's Lair |
| `[M]` | Active monster |
| `[V]` | Venomous monster |
| `[T]` | Trap |
| `[#]` | Wall |
| `[?]` | Unexplored tile |
| `[.]` | Explored empty tile |

The top status bar shows the current level, HP, combat bonus, and the quest
item for that level (`KEY: [MISSING]` / `[ACQUIRED]` on Levels 1-2, or
`EXCALIBUR: [LOCKED]` / `[DRAWN]` on Level 3).

---

## Save and load

The game saves automatically after each command during an active run. The save
file is:

```text
arnost-save.dat
```

It is created in the process working directory. A temporary
`arnost-save.dat.tmp` file is used during atomic replacement. Both files are
ignored by Git.

- Press `L` on the title screen to load a saved run.
- Type `save` in the dungeon to save immediately.
- The save includes the player's state, all three levels, item positions, trap
  and potion state, explored tiles, and the dragon's remaining lives.
- The save is deleted after victory or death.
- The format is Java object serialization. Do not edit the save file by hand.

If loading fails, the game starts a new keep and reports that the saved run
could not be read.

---

## Testing

Run the complete test suite:

```bash
mvn test
```

For a clean build with tests:

```bash
mvn clean test
```

The current suite contains **58 JUnit Jupiter tests**. A successful run ends
with output similar to:

```text
Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
```

Run a single test class or test method:

```bash
mvn -Dtest=GameEngineRulesTest test
mvn -Dtest=FullGameplaySimulationTest test
mvn -Dtest=GameEngineRulesTest#dragonNeedsThreeStrikesOfNineOrMore test
```

Surefire writes detailed reports to:

```text
target/surefire-reports/
```

The test suite covers:

- `GameEngineRulesTest` - story flow, command rules, combat, dragon lives,
  traps, potions, keys, and Excalibur.
- `FullGameplaySimulationTest` - a complete three-level winning playthrough.
- `MapAndRendererTest` - map generation, quest placement, TUI output, help
  text, ASCII art, and the 80-column layout.
- `SaveLoadTest` - save/load persistence, including dragon lives.
- `DefeatAndRestartTest` - defeat screen and restart behavior.
- `HelpCommandTest` - in-game help contents and screen width.
- `ScriptedDiceRoller` - deterministic test dice support.

---

## Project layout

```text
arnost/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/arnost/game/
│   │   │   ├── Main.java
│   │   │   ├── controller/
│   │   │   │   ├── CombatResolver.java
│   │   │   │   ├── DiceRoller.java
│   │   │   │   ├── GameEngine.java
│   │   │   │   ├── MapGenerator.java
│   │   │   │   └── SaveManager.java
│   │   │   ├── data/Json.java
│   │   │   ├── model/
│   │   │   │   ├── entity/Player.java
│   │   │   │   ├── item/
│   │   │   │   ├── map/
│   │   │   │   ├── monster/
│   │   │   │   ├── state/
│   │   │   │   └── story/StoryCatalog.java
│   │   │   └── view/
│   │   │       ├── ANSIColor.java
│   │   │       └── TUIRenderer.java
│   │   └── resources/data/
│   │       ├── items.json
│   │       ├── monsters.json
│   │       └── story.json
│   └── test/java/com/arnost/game/
│       ├── DefeatAndRestartTest.java
│       ├── FullGameplaySimulationTest.java
│       ├── GameEngineRulesTest.java
│       ├── HelpCommandTest.java
│       ├── MapAndRendererTest.java
│       ├── SaveLoadTest.java
│       └── ScriptedDiceRoller.java
├── target/        # generated by Maven; ignored
├── dist/          # local jpackage output; ignored
└── tmp/           # local working notes; ignored
```

The main packages separate the game loop and rules (`controller`), the game
data (`model`), the bundled JSON loader and resources (`data`), and terminal
presentation (`view`). `GameEngine` owns the command loop and story flow;
`StoryCatalog` supplies narrative screens; `ItemCatalog` and `MonsterCatalog`
load item and enemy content from JSON; `TUIRenderer` owns the 80-column
display; `MapGenerator` builds each level and guarantees a reachable route;
and `SaveManager` handles the serialized save file.

---

## Credits

Arnost: Legend of the Fallen Keep was created collaboratively by:

| Contributor | Role | Contact |
| --- | --- | --- |
| **Branislav Juhás** | Lead Architect - build, engine refactoring, test doubles, end-to-end tests | [juhas.branislaw@gmail.com](mailto:juhas.branislaw@gmail.com) |
| **Šimon Štefánik** | Mid Developer - TUI renderer, ANSI palettes, graph solver, command parser | [stefanik.simon.glassmansk@gmail.com](mailto:stefanik.simon.glassmansk@gmail.com) |
| **Adam Mistrík** | Junior Developer - entity models, coordinate helpers, unit tests | [adamkom.2006@gmail.com](mailto:adamkom.2006@gmail.com) |
| **Tetiana Tkachenko** | Documentation and Scaffolding - JSON data, README, comment fixes | [tkachenkotetianach@gmail.com](mailto:tkachenkotetianach@gmail.com) |

The in-game `CREATED BY` line uses terminal-safe spellings of the same names.

---

## Troubleshooting

### `release version 27 not supported`

Maven is running on an older JDK. Install JDK 27 and make sure both
`JAVA_HOME` and the `java`/`javac` on `PATH` point to it:

```bash
java -version
mvn -version
```

### `mvn` is not recognized

Install Apache Maven and add its `bin` directory to `PATH`. There is no Maven
wrapper in this repository.

### The display looks garbled or the colors are missing

Use a modern terminal with ANSI support and UTF-8 encoding. On Windows,
Windows Terminal or PowerShell is recommended. The layout is designed for at
least 80 columns; a narrower terminal will wrap lines.

### The game cannot save

Make sure the working directory is writable. `arnost-save.dat` and
`arnost-save.dat.tmp` are created in the directory from which the game was
started. Delete a corrupt save file to start a new run.

### `target` cannot be deleted or the jar cannot be replaced

Close any running instance of the game, then run `mvn clean package` again.

### The `dist` launcher still runs an old build

Rebuild the jar and refresh the local package:

```bash
mvn clean package
```

Then rebuild or update the local `dist` application, if you use it. The `dist`
directory is not part of the Git-tracked source.

---

## Quick start

```bash
mvn clean package
java -jar target/arnost-1.0.0-SNAPSHOT.jar
```

Press `ENTER` to enter the Fallen Keep. Type `help` at any time for the
in-game rules, and type `quit` when you are ready to leave.
