# Arnost: Legend of the Fallen Keep

Build Status: PASSED. Target: Java 27.

A 2d6 dungeon crawl through three levels of the Fallen Keep. Explore, fight
hidden guardians, collect the Dungeon Keys and Excalibur, and slay Ashgorath
the Dragon.

## Story

A new game opens with a prologue after the title screen. Press `ENTER` to enter
Level 1. Descending after Levels 1 and 2 reveals story cutscenes, and victory
ends the run with an epilogue.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/arnost-1.0.0-SNAPSHOT.jar
```

The game saves automatically. Press `L` on the title screen to load a saved
run. The interface is designed for an 80-column terminal.
