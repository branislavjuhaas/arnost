package com.arnost.game;

import com.arnost.game.controller.DiceRoller;
import com.arnost.game.controller.GameEngine;
import com.arnost.game.controller.MapGenerator;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.monster.MonsterCatalog;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineRulesTest {

    @Test
    void prologueAppearsAfterTheTitleAndBeforeLevelOne() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = MapGenerator.generateSeededMap(42L, 1);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "quit");

        String normalized = normalizeWhitespace(output);
        int chapter = normalized.indexOf("PROLOGUE");
        int storyTitle = normalized.indexOf("THE GATE OF THE FALLEN KEEP");
        int levelOne = normalized.indexOf("ARNOST :: LEVEL 1/3");
        assertTrue(chapter >= 0, "A new game must open with a prologue cutscene.");
        assertTrue(storyTitle > chapter, "The prologue must name the keep's gate.");
        assertTrue(levelOne > storyTitle,
            "The level must only appear after the prologue has been dismissed.");
        assertTrue(normalized.contains("Descend. Claim the keys. Draw Excalibur."));
        assertEquals(3, player.getX(), "The command after the prologue must still move the player.");
        assertEquals(5, player.getY(), "The command after the prologue must still be read.");
    }

    @Test
    void invalidMoveArgumentsAreRejected() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = MapGenerator.generateSeededMap(42L, 1);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "go", "go northx", "go sideways", "gox", "quit");

        assertEquals(3, player.getX(), "Invalid commands must not move the player.");
        assertEquals(6, player.getY(), "Invalid commands must not move the player.");
        assertTrue(output.contains("Invalid direction."), "Invalid directions must be rejected.");
    }

    @Test
    void movementShortcutsAndAliasesMovePlayer() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        GameState state = new GameState();

        runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "goe", "gon", "gol", "s", "left", "up", "quit");

        assertEquals(2, player.getX(), "Shortcut commands must move the player westward.");
        assertEquals(4, player.getY(), "Shortcut commands must move the player northward.");
    }

    @Test
    void wallTilesBlockMovement() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setType("WALL");
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "go north", "quit");

        assertEquals(3, player.getX(), "A wall must not be entered.");
        assertEquals(6, player.getY(), "A wall must not be entered.");
        assertTrue(output.contains("Rubble blocks the way."),
            "Walls must report that movement is blocked.");
    }

    @Test
    void itemTilesArePickedUpAutomatically() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setItemId("short_sword");
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "quit");

        assertTrue(player.getInventory().contains("short_sword"));
        assertTrue(map.getTile(3, 5).isCleared(),
            "A collected item tile must be marked cleared.");
        assertTrue(output.contains("Found Short Sword (+1 Strength)!"),
            "The status line must announce the picked-up item.");
    }

    @Test
    void movingOntoAnActiveMonsterShowsEncounterMessage() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = MapGenerator.generateSeededMap(42L, 1);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "go north", "quit");

        Tile ratTile = map.getTile(3, 5);
        assertTrue(ratTile.isExplored(), "Destination tile must be marked explored.");
        assertTrue(map.getTile(3, 6).isExplored(),
            "The starting tile must stay explored after moving.");
        assertEquals("rat", ratTile.getMonsterId());
        assertFalse(ratTile.isCleared());
        assertTrue(output.contains("Encountered a hidden MONSTER!"),
            "Moving onto a monster must announce the encounter.");
    }

    @Test
    void rollDoesNothingWhenNoMonsterIsPresent() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = MapGenerator.generateSeededMap(42L, 1);
        GameState state = new GameState();

        DiceRoller explodingDice = () -> {
            throw new AssertionError("Dice must not be rolled when no monster is present.");
        };

        String output = runGame(player, map, state, explodingDice, "", "roll", "quit");

        assertTrue(output.contains("There is no monster here to fight."));
        assertEquals(5, player.getHp(), "A non-combat roll must not damage the player.");
    }

    @Test
    void guardianVictoryDropsQuestKeyAndUnlocksTheExit() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = MapGenerator.generateSeededMap(42L, 1);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "n", "n", "e", "roll", "quit");

        Tile guardianTile = map.getTile(4, 3);
        assertTrue(guardianTile.isCleared());
        assertEquals("key", guardianTile.getItemId());
        assertTrue(player.getInventory().contains("key"),
            "The guardian's quest drop must be added to the inventory.");
        assertTrue(output.contains("Dungeon Key"),
            "The victory message must identify the quest drop.");
    }

    @Test
    void victoryAddsLootFromAStrongerMonster() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setMonsterId("wraith");
        map.getTile(3, 5).setMonsterStrength(11);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "roll", "quit");

        assertTrue(map.getTile(3, 5).isCleared());
        assertEquals(1, player.getInventory().size(),
            "An elite monster must drop a piece of gear.");
        assertTrue(output.contains("Defeated Barrow Wraith! Dropped"),
            "Stronger monsters must announce their superior loot.");
    }

    @Test
    void concealedStrengthIsRevealedWhenThePlayerStartsRolling() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        Tile monsterTile = map.getTile(3, 5);
        monsterTile.setMonsterId("rat");
        monsterTile.setMonsterStrength(5);
        monsterTile.setMonsterStrengthHidden(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(1),
            "", "n", "roll", "quit");

        assertFalse(monsterTile.isMonsterStrengthHidden(),
            "Rolling must permanently reveal the monster's strength.");
        assertTrue(output.contains("STR ???"),
            "The encounter must begin with the strength concealed.");
        assertTrue(output.contains("Revealed STR 5."),
            "The roll must announce the revealed strength.");
        assertTrue(output.contains("STR 5"),
            "The side panel must show the revealed strength after the roll.");
    }

    @Test
    void combatUsesStrengthScaledByThePlayersCombatBonus() {
        Player player = new Player("Sir Arnost", 5, 5);
        for (String item : new String[]{
            "war_axe", "halberd", "runeblade", "saber",
            "dagger", "short_sword", "leather_armor", "silver_ring"
        }) {
            player.getInventory().addItem(item);
        }
        DungeonMap map = new DungeonMap();
        Tile monsterTile = map.getTile(3, 5);
        monsterTile.setMonsterId("rat");
        monsterTile.setMonsterStrength(5);
        monsterTile.setMonsterStrengthHidden(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "roll", "quit");

        assertTrue(monsterTile.isCleared());
        assertTrue(output.contains("Revealed STR 10."),
            "A +16 bonus must add +5 menace to a base strength of 5.");
        assertTrue(output.contains("Defeated Giant Rat!"),
            "The scaled strength must be used in combat.");
    }

    @Test
    void groundPotionRemainsUntilExplicitlyConsumed() {
        Player player = new Player("Sir Arnost", 3, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setHasPotion(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "s", "n", "quit");

        assertTrue(map.getTile(3, 5).hasPotion(),
            "Walking away and returning must not remove the ground potion.");
        assertEquals(3, player.getHp(), "An unconsumed potion must not heal the player.");
        assertTrue(normalizeWhitespace(output).contains(
            "You see a Health Potion on the ground. Type 'drink' to consume (+2 HP)."));
    }

    @Test
    void drinkConsumesPotionAndRestoresTwoHpUpToMaximum() {
        Player player = new Player("Sir Arnost", 3, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setHasPotion(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "drink", "quit");

        assertFalse(map.getTile(3, 5).hasPotion(),
            "Drinking must consume the potion from the tile.");
        assertEquals(5, player.getHp(), "The potion must restore 2 HP without exceeding 5.");
        assertTrue(output.contains("You drink the Health Potion."));
    }

    @Test
    void takePotionAliasAlsoConsumesTheGroundPotion() {
        Player player = new Player("Sir Arnost", 4, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setHasPotion(true);
        GameState state = new GameState();

        runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "take potion", "quit");

        assertFalse(map.getTile(3, 5).hasPotion());
        assertEquals(5, player.getHp());
    }

    @Test
    void armedTrapDamagesOnceAndThenStaysDisarmed() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setHasTrap(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "s", "n", "quit");

        assertEquals(4, player.getHp(), "A trap must deal one point of damage only once.");
        assertTrue(map.getTile(3, 5).isTrapTriggered());
        assertTrue(output.contains("A trap springs!"));
    }

    @Test
    void exploringRevealsNearbyTrapsBeforeTheyAreSteppedOn() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(2, 5).setHasTrap(true);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "quit");

        assertTrue(map.getTile(2, 5).isExplored(),
            "Moving beside a trap must explore its chamber.");
        assertTrue(normalizeWhitespace(output).contains("[T]"),
            "A discovered trap must be visible on the map before it is stepped on.");
        assertEquals(5, player.getHp(), "The revealed trap must not have been triggered.");
    }

    @Test
    void venomousMonsterAddsPoisonDamageAfterADefeat() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setMonsterId("venom_spider");
        map.getTile(3, 5).setMonsterStrength(30);
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(2),
            "", "n", "roll", "quit");

        assertEquals(3, player.getHp(),
            "A venomous defeat must cost the normal 1 HP plus 1 poison HP.");
        assertFalse(state.isPlayerDead());
        assertTrue(output.contains("Venom burns for 1 more HP!"),
            "The status line must explain the extra poison damage.");
        assertFalse(map.getTile(3, 5).isCleared(),
            "A defeat must not clear the venomous monster.");
    }

    @Test
    void movementRevealsTheCurrentTileAndItsNeighbours() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        GameState state = new GameState();

        runGame(player, map, state, new ScriptedDiceRoller(12), "", "n", "quit");

        assertTrue(map.getTile(3, 5).isExplored(), "The destination must be explored.");
        assertTrue(map.getTile(2, 5).isExplored(), "The west neighbour must be revealed.");
        assertTrue(map.getTile(4, 5).isExplored(), "The east neighbour must be revealed.");
        assertTrue(map.getTile(3, 4).isExplored(), "The north neighbour must be revealed.");
        assertTrue(map.getTile(3, 6).isExplored(), "The previous start tile must stay explored.");
    }

    @Test
    void exitStairsAdvanceToThePreGeneratedNextLevelAndResetPosition() {
        DungeonMap map = MapGenerator.generateSeededMap(777L, 1);
        map.getTile(3, 5).setCleared(true);
        map.getTile(3, 4).setCleared(true);

        Player player = new Player("Sir Arnost", 5, 5);
        player.getInventory().addItem("key");
        GameState state = new GameState();
        String commands = String.join("\n",
            "",          // Dismiss the title screen
            "",          // Dismiss the prologue cutscene
            "n", "n", "n", "n", "n", "n",
            "e", "e", "e",
            "go stairs",
            "",          // Dismiss the Level 2 cutscene
            "quit"
        ) + "\n";

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        GameEngine engine = new GameEngine(
            player,
            map,
            state,
            new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
            new TUIRenderer(new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false),
            new ScriptedDiceRoller(12)
        );
        engine.startLoop();

        assertEquals(2, state.getCurrentLevel(), "Stairs must advance the current level.");
        assertEquals(3, player.getX(), "Descending must reset the player to [3,6].");
        assertEquals(6, player.getY(), "Descending must reset the player to [3,6].");
        assertEquals(2, engine.getCurrentMap().getGenerationLevel());
        assertEquals(0, engine.getCurrentMap().getExitX());
        assertEquals(0, engine.getCurrentMap().getExitY());
        assertFalse(player.getInventory().contains("key"),
            "Descending must consume the key for the current level.");

        String output = normalizeWhitespace(
            outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(output.contains("ARNOST - LEGEND OF THE FALLEN KEEP LEVEL 2"),
            "Descending must open the Level 2 story cutscene.");
        assertTrue(output.contains("THE SUNKEN CRYPTS"));
        assertTrue(output.contains("You unlock the heavy iron grate and descend into the Sunken Crypts."),
            "Descending must output the Level 2 story block.");
        assertTrue(output.contains("The air grows thick with ancient venom..."));
        assertTrue(output.contains("Press [ENTER] to enter the Sunken Crypts..."),
            "The Level 2 cutscene must wait for the player to continue.");
    }

    @Test
    void descendingToLevelThreeShowsTheDragonAbyssStoryBlock() {
        DungeonMap map = MapGenerator.generateSeededMap(12345L, 2);
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(0, 0);
        player.getInventory().addItem("key");
        GameState state = new GameState();
        state.setCurrentLevel(2);

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "descend", "", "quit");

        assertEquals(3, state.getCurrentLevel());
        String normalized = normalizeWhitespace(output);
        assertTrue(normalized.contains("ARNOST - LEGEND OF THE FALLEN KEEP LEVEL 3"),
            "Descending must open the Level 3 story cutscene.");
        assertTrue(normalized.contains("THE DRAGON'S ABYSS"));
        assertTrue(normalized.contains(
                "Unlocking the vault with the Mummy Lord's Key, you descend into the Dragon's Abyss."),
            "Descending must output the Level 3 story block.");
        assertTrue(normalized.contains("Volcanic heat radiates from below..."));
        assertTrue(normalized.contains("Press [ENTER] to enter the Dragon's Abyss..."));
        assertTrue(stripAnsi(output).lines()
                .filter(line -> !line.startsWith("> "))
                .allMatch(line -> line.length() <= 80),
            "The story block must stay inside the 80-column layout.");
    }

    @Test
    void lockedExitCannotBeDescendedWithoutTheLevelKey() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(3, 5);
        DungeonMap map = new DungeonMap();
        map.setExitPosition(3, 5);
        map.getTile(3, 5).setType("EXIT");
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "descend", "quit");

        assertEquals(1, state.getCurrentLevel(), "A locked staircase must not advance the level.");
        assertTrue(normalizeWhitespace(output).contains(
            "The Exit Staircase is locked. A hidden guardian still holds the Dungeon Key."));
    }

    @Test
    void descendCommandRequiresThePlayerToStandOnStairs() {
        Player player = new Player("Sir Arnost", 5, 5);
        DungeonMap map = new DungeonMap();
        GameState state = new GameState();

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "descend", "quit");

        assertEquals(1, state.getCurrentLevel());
        assertTrue(output.contains("You must stand on an Exit Staircase [E] to descend."));
    }

    @Test
    void dragonLairAwakensOnLevelThreeAtThreeOneWithEnoughGear() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.getInventory().addItem("dagger");
        player.getInventory().addItem("saber");
        player.getInventory().addItem("fireball");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        runGame(player, map, state, new ScriptedDiceRoller(12), "", "n", "quit");

        Tile dragonTile = map.getTile(3, 1);
        assertEquals("DRAGON_LAIR", dragonTile.getType());
        assertEquals("dragon", dragonTile.getMonsterId());

        long dragonTiles = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).isDragonLair()) {
                    dragonTiles++;
                }
            }
        }
        assertEquals(1, dragonTiles, "Only the pre-generated dragon lair may awaken.");
    }

    @Test
    void dragonLairDoesNotActivateBelowFourCombatBonus() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.getInventory().addItem("dagger");
        player.getInventory().addItem("saber");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        runGame(player, map, state, new ScriptedDiceRoller(12), "", "n", "quit");

        assertTrue(map.getTile(3, 1).isDragonLair());
        assertNull(map.getTile(3, 1).getMonsterId(),
            "The dragon must remain dormant below +4 combat bonus.");
    }

    @Test
    void dragonNeedsThreeStrikesOfNineOrMore() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(3, 1);
        player.getInventory().addItem("excalibur");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        MapGenerator.activateDragonLair(map);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        String output = runGame(player, map, state, new ScriptedDiceRoller(9, 9, 9),
            "", "roll", "roll", "roll", "quit");

        assertTrue(state.isGameWon(), "The third successful strike must slay the dragon.");
        Tile dragonTile = map.getTile(3, 1);
        assertTrue(dragonTile.isCleared(), "The lair must clear only after the third hit.");
        assertEquals(0, dragonTile.getMonsterLives());
        assertEquals(1, player.getInventory().getItems().stream()
            .filter("crown_jewels"::equals).count(),
            "The dragon must drop the Crown Jewels on the final hit.");
        assertTrue(output.contains("LIVES 2/3"), "The first hit must leave two lives.");
        assertTrue(output.contains("LIVES 1/3"), "The second hit must leave one life.");
        assertTrue(output.contains("Ashgorath has 2 lives left."));
        assertTrue(output.contains("Ashgorath has 1 life left."));
    }

    @Test
    void dragonRollOfEightIsADrawAndDoesNotTakeALife() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(3, 1);
        player.getInventory().addItem("excalibur");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        MapGenerator.activateDragonLair(map);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        String output = runGame(player, map, state, new ScriptedDiceRoller(8),
            "", "roll", "quit");

        assertFalse(state.isGameWon(), "A roll of 8 must not damage the dragon.");
        assertEquals(MonsterCatalog.DRAGON_LIVES, map.getTile(3, 1).getMonsterLives(),
            "A roll of 8 must not remove a dragon life.");
        assertEquals(5, player.getHp(), "A draw must not cost HP.");
        assertTrue(output.contains("Draw. Ashgorath turns the blade aside"),
            "A roll of 8 must be reported as a draw.");
        assertTrue(output.contains("2d6 sum of 9+"));
    }

    @Test
    void dragonRollBelowEightCostsOneHpAndDoesNotTakeALife() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(3, 1);
        player.getInventory().addItem("excalibur");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        MapGenerator.activateDragonLair(map);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        String output = runGame(player, map, state, new ScriptedDiceRoller(7),
            "", "roll", "quit");

        assertFalse(state.isGameWon(), "A low roll must not damage the dragon.");
        assertEquals(MonsterCatalog.DRAGON_LIVES, map.getTile(3, 1).getMonsterLives());
        assertEquals(4, player.getHp(), "A failed dragon strike must cost 1 HP.");
        assertTrue(output.contains("Defeat. Ashgorath's claws cost you 1 HP"),
            "A low dragon roll must report the failed strike.");
    }

    @Test
    void dragonCannotBeSlainWithoutExcalibur() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(3, 2);
        player.getInventory().addItem("dagger");
        player.getInventory().addItem("saber");
        player.getInventory().addItem("fireball");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        String output = runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "roll", "quit");

        assertFalse(state.isGameWon(), "The dragon must survive attacks without Excalibur.");
        assertEquals(5, player.getHp(), "A failed dragon strike must not cost HP.");
        assertTrue(output.contains("Only Excalibur can slay it."));
    }

    @Test
    void dragonLairDoesNotSpreadAcrossTheLevel() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.getInventory().addItem("dagger");
        player.getInventory().addItem("saber");
        player.getInventory().addItem("fireball");

        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);
        GameState state = new GameState();
        state.setCurrentLevel(3);

        runGame(player, map, state, new ScriptedDiceRoller(12),
            "", "n", "s", "e", "w", "quit");

        long dragonTiles = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).isDragonLair()) {
                    dragonTiles++;
                }
            }
        }
        assertEquals(1, dragonTiles, "The dragon lair must not follow the player or spread.");
    }

    private static String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[0-9;]*[A-Za-z]", "");
    }

    private static String normalizeWhitespace(String text) {
        return stripAnsi(text).replaceAll("\\s+", " ");
    }

    private static String runGame(Player player, DungeonMap map, GameState state,
                                  DiceRoller diceRoller, String... lines) {
        List<String> commandLines = new ArrayList<>(List.of(lines));
        if (!commandLines.isEmpty()) {
            commandLines.add(1, ""); // Dismiss the prologue after the title screen.
        }
        String commands = String.join("\n", commandLines) + "\n";
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream testPrintStream = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8);

        GameEngine engine = new GameEngine(
            player,
            map,
            state,
            new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
            new TUIRenderer(testPrintStream, false),
            diceRoller
        );
        engine.startLoop();

        return outputBuffer.toString(StandardCharsets.UTF_8);
    }
}
