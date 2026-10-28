package com.arnost.game;

import com.arnost.game.controller.MapGenerator;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.monster.MonsterCatalog;
import com.arnost.game.view.ANSIColor;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapAndRendererTest {

    @Test
    void levelOnePopulatesRatsAndSpidersWithExitAtSixZero() {
        DungeonMap map = MapGenerator.generateSeededMap(12345L, 1);

        assertTrue(map.hasExitPosition());
        assertEquals(6, map.getExitX());
        assertEquals(0, map.getExitY());
        assertEquals("EXIT", map.getTile(6, 0).getType());
        assertFalse(map.hasDragonLairPosition());

        assertEquals("rat", map.getTile(3, 5).getMonsterId());
        assertEquals(6, map.getTile(3, 5).getMonsterStrength());
        assertEquals("spider", map.getTile(3, 4).getMonsterId());
        assertEquals(8, map.getTile(3, 4).getMonsterStrength());
        assertEquals("warden", map.getTile(4, 3).getMonsterId());
        assertEquals(10, map.getTile(4, 3).getMonsterStrength());

        assertMonstersBelongToLevel(map, 1);
        assertTrue(countWalls(map) >= 6, "The map must contain several impassable walls.");
        assertTrue(countItems(map) > 0, "The level must contain loot.");
        assertEquals(2, countPotions(map),
            "Level 1 must provide potions for a hero who starts at 3/5 HP.");
        assertEquals(2, countTraps(map),
            "Every level must contain visible traps.");
    }

    @Test
    void levelTwoHasSpiderAndMummyPopulationTwoPotionsAndTwoTraps() {
        DungeonMap map = MapGenerator.generateSeededMap(12345L, 2);

        assertTrue(map.hasExitPosition());
        assertEquals(0, map.getExitX());
        assertEquals(0, map.getExitY());
        assertEquals("EXIT", map.getTile(0, 0).getType());
        assertFalse(map.hasDragonLairPosition());

        assertEquals(2, countPotions(map), "Level 2 must contain exactly two ground potions.");
        assertEquals(2, countTraps(map), "Level 2 must contain exactly two traps.");
        assertMonstersBelongToLevel(map, 2);

        assertEquals("venom_spider", map.getTile(3, 5).getMonsterId(),
            "Level 2 must introduce a venomous enemy on the main path.");
        assertEquals(15, map.getTile(3, 5).getMonsterStrength());
        assertEquals("mummy", map.getTile(3, 4).getMonsterId());
        assertEquals(18, map.getTile(3, 4).getMonsterStrength());
        assertEquals("mummy_lord", map.getTile(4, 3).getMonsterId());
        assertEquals(22, map.getTile(4, 3).getMonsterStrength());
    }

    @Test
    void levelThreeHasMummiesAndDragonLairAtThreeOne() {
        DungeonMap map = MapGenerator.generateSeededMap(12345L, 3);

        assertFalse(map.hasExitPosition(), "The final keep has no further stairs.");
        assertTrue(map.hasDragonLairPosition());
        assertEquals(3, map.getDragonLairX());
        assertEquals(1, map.getDragonLairY());
        assertTrue(map.getTile(3, 1).isDragonLair());
        assertNull(map.getTile(3, 1).getMonsterId(),
            "The lair remains dormant until the player has +4 combat bonus.");
        assertEquals("plague_mummy", map.getTile(3, 5).getMonsterId(),
            "Level 3 must introduce a venomous plague enemy.");
        assertEquals(25, map.getTile(3, 5).getMonsterStrength());
        assertEquals("mummy", map.getTile(3, 4).getMonsterId());
        assertEquals(23, map.getTile(3, 4).getMonsterStrength());
        assertEquals("knight", map.getTile(4, 2).getMonsterId());
        assertEquals(33, map.getTile(4, 2).getMonsterStrength());
        assertMonstersBelongToLevel(map, 3);
        assertEquals(2, countPotions(map), "Level 3 must provide potions before the dragon.");
        assertEquals(2, countTraps(map), "Level 3 must contain visible traps.");

        DungeonMap awakened = MapGenerator.generateSeededMap(12345L, 3, 4);
        assertEquals("dragon", awakened.getTile(3, 1).getMonsterId());
        assertEquals(37, awakened.getTile(3, 1).getMonsterStrength());
        assertFalse(awakened.getTile(3, 1).isMonsterStrengthHidden(),
            "The dragon's strength must remain visible.");
    }

    @Test
    void questItemsAreCarriedByHiddenGuardiansInsteadOfLyingOnTheFloor() {
        DungeonMap levelOne = MapGenerator.generateSeededMap(12345L, 1);
        assertEquals("warden", levelOne.getTile(4, 3).getMonsterId());
        assertEquals("key", MonsterCatalog.get("warden").dropItemId());
        assertEquals(0, countItemId(levelOne, "key"),
            "Level 1's key must be carried by its guardian, not placed on the floor.");
        assertEquals(0, countItemId(levelOne, "excalibur"));

        DungeonMap levelTwo = MapGenerator.generateSeededMap(12345L, 2);
        assertEquals("mummy_lord", levelTwo.getTile(4, 3).getMonsterId());
        assertEquals("key", MonsterCatalog.get("mummy_lord").dropItemId());
        assertEquals(0, countItemId(levelTwo, "key"),
            "Level 2's key must be carried by its guardian, not placed on the floor.");
        assertEquals(0, countItemId(levelTwo, "excalibur"));

        DungeonMap levelThree = MapGenerator.generateSeededMap(12345L, 3);
        assertEquals("knight", levelThree.getTile(4, 2).getMonsterId());
        assertEquals("excalibur", MonsterCatalog.get("knight").dropItemId());
        assertEquals(0, countItemId(levelThree, "key"),
            "The final level has no exit to unlock.");
        assertEquals(0, countItemId(levelThree, "excalibur"),
            "Excalibur must be guarded by the Knight Guardian, not placed on the floor.");
    }

    @Test
    void strongerMonstersOfferBetterLootAndWeakRatsCanDropNothing() {
        assertTrue(MonsterCatalog.dropChanceOf("rat") < 1.0,
            "The weakest enemy must be able to drop nothing.");
        assertEquals(1, MonsterCatalog.maxLootBonusOf("rat"));
        assertEquals(1, MonsterCatalog.maxLootBonusOf("spider"));
        assertEquals(2, MonsterCatalog.maxLootBonusOf("mummy"));
        assertEquals(3, MonsterCatalog.maxLootBonusOf("guardian"));
        assertEquals(4, MonsterCatalog.maxLootBonusOf("wraith"));
        assertTrue(MonsterCatalog.maxLootBonusOf("guardian")
                > MonsterCatalog.maxLootBonusOf("mummy"),
            "Guardians must outclass ordinary mummies.");
        assertTrue(MonsterCatalog.maxLootBonusOf("wraith")
                > MonsterCatalog.maxLootBonusOf("guardian"),
            "The deepest random enemy must outclass earlier guardians.");

        Random alwaysNothing = new Random(0L) {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
        assertNull(MonsterCatalog.rollLoot("rat", alwaysNothing));
        assertEquals("key", MonsterCatalog.rollLoot("warden", alwaysNothing),
            "Quest drops must not be lost to a bad loot roll.");
        assertEquals("excalibur", MonsterCatalog.rollLoot("knight", alwaysNothing));
    }

    @Test
    void rendererUsesVariantOneMarkersBordersAndHeader() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true); // Start

        Tile monsterTile = map.getTile(1, 1);
        monsterTile.setExplored(true);
        monsterTile.setMonsterId("rat");

        Tile itemTile = map.getTile(2, 2);
        itemTile.setExplored(true);
        itemTile.setItemId("dagger");

        Tile potionTile = map.getTile(1, 2);
        potionTile.setHasPotion(true);

        Tile trapTile = map.getTile(2, 3);
        trapTile.setHasTrap(true);

        Tile exitTile = map.getTile(6, 0);
        exitTile.setType("EXIT");

        Tile dragonTile = map.getTile(4, 4);
        dragonTile.setExplored(true);
        dragonTile.setType("DRAGON_LAIR");
        dragonTile.setMonsterId("dragon");
        map.setDragonLairPosition(4, 4);

        map.getTile(6, 1).setType("WALL");
        map.getTile(5, 5).setExplored(true); // Empty explored tile

        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(0, 0);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.render(player, map, "Test");

        String hiddenOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertFalse(hiddenOutput.contains("[P]"),
            "Ground potions must stay hidden until their tile is discovered.");
        assertFalse(hiddenOutput.contains("[T]"),
            "Traps must stay hidden until their tile is explored.");
        assertTrue(hiddenOutput.contains("[M]"),
            "An active monster on an explored tile must remain visible.");
        assertFalse(hiddenOutput.contains("[V]"), "No venomous monster was placed.");
        assertFalse(hiddenOutput.contains("[K]"), "Quest items must not use map markers.");
        assertFalse(hiddenOutput.contains("[X]"), "Quest items must not use map markers.");

        potionTile.setExplored(true);
        trapTile.setExplored(true);
        outputBuffer.reset();
        renderer.render(player, map, "Test");

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        String visibleOutput = stripAnsi(output);

        assertTrue(visibleOutput.contains("[?] [?]"),
            "Map tiles must be separated by exactly one space.");
        assertTrue(output.contains(ANSIColor.SKY_BLUE + "[?]"),
            "Unexplored tiles must render as a bright [?] marker.");
        assertTrue(output.contains("[*]"), "The player tile must render as [*].");
        assertTrue(output.contains("[S]"), "The start tile must render as [S].");
        assertTrue(output.contains("[D]"), "The dragon lair must render as [D].");
        assertTrue(output.contains("[M]"),
            "An active monster on an explored tile must render as [M].");
        assertFalse(output.contains("[K]"), "Floor keys must not be rendered as map markers.");
        assertFalse(output.contains("[X]"), "Excalibur must not be rendered as a map marker.");
        assertTrue(output.contains("[I]"), "An uncleared item tile must render as [I].");
        assertTrue(output.contains("[P]"), "A ground potion must render as [P].");
        assertTrue(output.contains("[T]"), "An armed trap must render as [T].");
        assertTrue(output.contains("[E]"), "Exit stairs must render as [E].");
        assertTrue(output.contains("[#]"), "A wall tile must render as [#].");
        assertTrue(output.contains("[.]"), "A cleared/empty explored tile must render as [.].");

        assertTrue(visibleOutput.contains(
                "ARNOST :: LEVEL 1/3 :: HP [5/5] :: COMBAT +0 :: KEY: [MISSING]"),
            "Level 1's header must show only the key indicator.");
        assertFalse(visibleOutput.contains("EXCALIBUR:"),
            "Level 1's header must not mention Excalibur.");
        assertTrue(visibleOutput.contains(".--- [ LOCATION: EMPTY CHAMBER ]"),
            "The right pane must use a thin labelled top border.");
        assertTrue(visibleOutput.contains("'" + "-".repeat(44) + "'"),
            "The right pane must use a thin bottom border.");

        long panelRows = visibleOutput.lines()
            .filter(line -> line.matches("^.{27}   \\|   .{46}$"))
            .count();
        assertEquals(10, panelRows, "The map and inspect pane must share ten aligned rows.");
    }

    @Test
    void dragonSiteIsVisibleFromTheStart() {
        DungeonMap map = MapGenerator.generateSeededMap(42L, 3);

        Player player = new Player("Sir Arnost", 5, 5);
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.render(player, map, "Test");

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[D]"), "The dragon site must be visible before the tile is explored.");
        assertFalse(map.getTile(3, 1).isExplored(), "Visibility must not depend on exploration.");
    }

    @Test
    void safeTilesShowLocationInsteadOfStaleGroundLoot() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true);

        Player player = new Player("Sir Arnost", 5, 5);
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.render(player, map, "Home");
        String homeOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(homeOutput.contains("[ LOCATION: START FOUNTAIN ]"),
            "The start tile must show the location summary.");
        assertTrue(homeOutput.contains("Equipped : None"));
        assertTrue(homeOutput.contains("Bonus    : +0 STR"));

        player.getInventory().addItem("dagger");
        map.getTile(2, 2).setExplored(true);
        player.setPosition(2, 2);
        outputBuffer.reset();
        renderer.render(player, map, "Looted");

        String emptyTileOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(emptyTileOutput.contains("[ LOCATION: EMPTY CHAMBER ]"),
            "An empty tile must show the location summary after loot is collected.");
        assertFalse(emptyTileOutput.contains("[ INVENTORY ]"),
            "Collected loot must not remain as a ground-loot panel.");
        assertFalse(emptyTileOutput.contains("[ GROUND LOOT ]"),
            "Collected loot must not remain as a ground-loot panel.");
        assertTrue(emptyTileOutput.contains("Equipped : Iron Dagger"));
        assertTrue(emptyTileOutput.contains("Bonus    : +1 STR"));
    }

    @Test
    void currentSpecialTileKeepsItsSpecialColorAndMonsterPanel() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true);

        Player player = new Player("Sir Arnost", 5, 5);
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.render(player, map, "At the fountain");
        String startOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(startOutput.contains(ANSIColor.EMERALD + "[*]"),
            "The current start tile must keep its emerald color.");

        Tile monsterTile = map.getTile(1, 1);
        monsterTile.setExplored(true);
        monsterTile.setMonsterId("rat");
        monsterTile.setMonsterStrength(5);
        player.setPosition(1, 1);

        outputBuffer.reset();
        renderer.render(player, map, "Encounter");
        String encounterOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(encounterOutput.contains(ANSIColor.GOLD + "[*]"),
            "The current monster tile must keep the monster color.");
        assertTrue(encounterOutput.contains("Giant Rat"));
        assertTrue(encounterOutput.contains("STR 5"));
    }

    @Test
    void roughlyHalfOfGeneratedMonstersConcealTheirStrength() {
        int hidden = 0;
        int total = 0;
        for (long seed = 1; seed <= 40; seed++) {
            for (int level = 1; level <= 3; level++) {
                DungeonMap map = MapGenerator.generateSeededMap(seed, level);
                for (int y = 0; y < DungeonMap.SIZE; y++) {
                    for (int x = 0; x < DungeonMap.SIZE; x++) {
                        Tile tile = map.getTile(x, y);
                        if (tile.getMonsterId() != null) {
                            total++;
                            if (tile.isMonsterStrengthHidden()) {
                                hidden++;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(total >= 100, "The sample must contain enough monsters to be meaningful.");
        double concealedRatio = (double) hidden / total;
        assertTrue(concealedRatio >= 0.30 && concealedRatio <= 0.70,
            "About half of all monsters must conceal their strength, but was " + concealedRatio);
    }

    @Test
    void concealedMonsterStrengthIsUnknownUntilRevealed() {
        DungeonMap map = new DungeonMap();
        Tile monsterTile = map.getTile(1, 1);
        monsterTile.setExplored(true);
        monsterTile.setMonsterId("rat");
        monsterTile.setMonsterStrength(5);
        monsterTile.setMonsterStrengthHidden(true);

        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(1, 1);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.render(player, map, "Encounter");

        String hiddenOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(hiddenOutput.contains("STR ???"),
            "A concealed monster must not reveal its strength.");
        assertTrue(hiddenOutput.contains("Type 'roll' to reveal and fight."));
        assertFalse(hiddenOutput.contains("STR 5"));

        monsterTile.setMonsterStrengthHidden(false);
        outputBuffer.reset();
        renderer.render(player, map, "Revealed");

        String revealedOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(revealedOutput.contains("STR 5"),
            "The strength must be visible once revealed.");
        assertTrue(revealedOutput.contains("Type 'roll' to fight."));
    }

    @Test
    void monsterStrengthRisesWithLevelAndPlayerCombatBonus() {
        assertEquals(5, MonsterCatalog.effectiveStrength(5, 0));
        assertEquals(10, MonsterCatalog.effectiveStrength(5, 16),
            "Every +3 combat bonus must add +1 monster strength.");
        assertEquals(12, MonsterCatalog.effectiveStrength(5, 21));
        assertTrue(MonsterCatalog.strengthForLevel("mummy", 3)
                > MonsterCatalog.strengthForLevel("mummy", 2),
            "Shared monsters must be tougher on deeper levels.");
        assertTrue(MonsterCatalog.strengthForLevel("mummy_lord", 2)
                > MonsterCatalog.strengthForLevel("mummy", 2),
            "The key guardian must outclass ordinary monsters on its level.");
    }

    @Test
    void headerTracksOnlyTheQuestItemForTheCurrentLevel() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true);
        Player player = new Player("Sir Arnost", 5, 5);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.render(player, map, 1, "Empty pockets");
        String emptyOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(emptyOutput.contains("KEY: [MISSING]"),
            "Level 1 must show the missing key.");
        assertFalse(emptyOutput.contains("EXCALIBUR:"),
            "Level 1 must not mention Excalibur in the top bar.");

        outputBuffer.reset();
        renderer.render(player, map, 2, "Key still missing");
        String missingKeyOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(missingKeyOutput.contains("KEY: [MISSING]"),
            "Level 2 must show the key as missing until the Mummy Lord falls.");
        assertFalse(missingKeyOutput.contains("EXCALIBUR:"),
            "Level 2 must not mention Excalibur in the top bar.");

        player.getInventory().addItem("key");
        outputBuffer.reset();
        renderer.render(player, map, 2, "Quest key held");
        String levelTwoOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(levelTwoOutput.contains("KEY: [ACQUIRED]"),
            "Level 2 must show the acquired key.");
        assertFalse(levelTwoOutput.contains("EXCALIBUR:"),
            "Level 2 must not mention Excalibur in the top bar.");

        outputBuffer.reset();
        renderer.render(player, map, 3, "Excalibur still locked");
        String lockedExcaliburOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(lockedExcaliburOutput.contains("EXCALIBUR: [LOCKED]"),
            "Level 3 must show Excalibur as locked until the Knight Guardian falls.");
        assertFalse(lockedExcaliburOutput.contains("KEY:"),
            "Level 3 must not mention the key in the top bar.");

        player.getInventory().addItem("excalibur");
        outputBuffer.reset();
        renderer.render(player, map, 3, "Final level");
        String levelThreeOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(levelThreeOutput.contains("EXCALIBUR: [DRAWN]"),
            "Level 3 must show the drawn Excalibur.");
        assertFalse(levelThreeOutput.contains("KEY:"),
            "Level 3 must not mention the key in the top bar.");
    }

    @Test
    void venomousMonsterUsesVMarkerAndWarnsInThePanel() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true);

        Tile venomTile = map.getTile(1, 1);
        venomTile.setExplored(true);
        venomTile.setMonsterId("venom_spider");
        venomTile.setMonsterStrength(15);

        Player player = new Player("Sir Arnost", 5, 5);
        player.setPosition(0, 0);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.render(player, map, "Venom ahead");

        String mapOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(mapOutput.contains("[V]"),
            "A venomous monster must be marked [V] on the map.");

        player.setPosition(1, 1);
        outputBuffer.reset();
        renderer.render(player, map, "Face the venom");
        String panelOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(panelOutput.contains("Venom Spider"));
        assertTrue(panelOutput.contains("VENOM"),
            "The encounter panel must warn that the monster is venomous.");
        assertTrue(panelOutput.contains("STR 15"));
    }

    @Test
    void storyMonstersShowTheirTwoLineLoreInTheInspectPane() {
        String[][] monsters = {
            {"venom_spider", "15", "Chitinous fangs drip with green bile.", "Venom Spider"},
            {"mummy_lord", "22", "Guards the Dungeon Key to Level 3.", "Mummy Lord"},
            {"plague_mummy", "25", "Rotting bandages leak deadly poison.", "Plague Mummy"},
            {"guardian", "27", "An animated suit of zweihänder-wielding steel.", "Iron Guardian"},
            {"wraith", "31", "A screaming phantom clad in frost and shadow.", "Barrow Wraith"},
            {"knight", "33", "Slay him to unearth the holy blade Excalibur!", "Knight Guardian"},
            {"dragon", "37", "The ancient wyrm snarls! Excalibur burns brightly.",
                "Ashgorath the Dragon"}
        };

        for (String[] monster : monsters) {
            DungeonMap map = new DungeonMap();
            Tile monsterTile = map.getTile(1, 1);
            monsterTile.setExplored(true);
            monsterTile.setMonsterId(monster[0]);
            monsterTile.setMonsterStrength(Integer.parseInt(monster[1]));

            Player player = new Player("Sir Arnost", 5, 5);
            player.setPosition(1, 1);

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            TUIRenderer renderer = new TUIRenderer(
                new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
            renderer.render(player, map, "Inspect");

            String output = normalizePanelText(
                outputBuffer.toString(StandardCharsets.UTF_8));
            assertTrue(output.contains(monster[3]),
                "The inspect pane must name " + monster[3] + ".");
            assertTrue(output.contains(monster[2]),
                "The inspect pane must show the lore for " + monster[3] + ".");
        }
    }

    @Test
    void storyScreenWrapsNarrationInsideTheStandardWidth() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.renderStoryScreen("PROLOGUE", "THE GATE OF THE FALLEN KEEP",
            "Press [ENTER] to enter the keep...",
            "The Fallen Keep has stood silent for a hundred years, its gates sealed beneath ash and ivy.",
            "Sir Arnost, last knight of the broken order, rides through the mist with a worn blade and a vow he cannot break.");

        String visibleOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(visibleOutput.contains("PROLOGUE"));
        assertTrue(visibleOutput.contains("THE GATE OF THE FALLEN KEEP"));
        assertTrue(visibleOutput.contains("The Fallen Keep has stood silent"));
        assertTrue(visibleOutput.contains("Press [ENTER] to enter the keep..."));
        assertTrue(visibleOutput.lines().allMatch(line -> line.length() <= 80),
            "Every story line must fit inside the 80-column screen.");
    }

    @Test
    void victoryAndDefeatArtDoNotLabelTheHeroOrDragon() {
        Player player = new Player("Sir Arnost", 5, 5);
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.renderVictoryScreen(player, "The keep is free.");
        String victoryOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertFalse(victoryOutput.contains("SIR ARNOST"),
            "Victory art must not label the hero.");
        assertFalse(victoryOutput.contains("ANCIENT"),
            "Victory art must not label the dragon as an ancient wyrm.");
        assertTrue(victoryOutput.contains("VICTORY - ASHGORATH IS SLAIN"));
        assertTrue(victoryOutput.contains("EPILOGUE - THE FALLEN KEEP IS FREE"),
            "The victory screen must present the post-win story as an epilogue.");
        assertTrue(victoryOutput.contains("EXCALIBUR"));
        assertTrue(victoryOutput.contains("CROWN JEWELS"));
        assertTrue(stripAnsi(victoryOutput).lines().allMatch(line -> line.length() <= 80),
            "The victory screen must stay inside the 80-column layout.");

        outputBuffer.reset();
        renderer.renderDefeatScreen(player, "The keep claims another hero.");
        String defeatOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        assertFalse(defeatOutput.contains("SIR ARNOST"),
            "Defeat art must not label the hero.");
        assertTrue(defeatOutput.contains("ANOTHER HERO HAS FALLEN"));
    }

    @Test
    void statusTextWrapsInsideTheStandardScreenWidth() {
        DungeonMap map = new DungeonMap();
        map.getTile(3, 6).setExplored(true);
        Player player = new Player("Sir Arnost", 5, 5);

        String longMessage = "The hidden guardian raises its blade and the walls tremble "
            + "while ancient dust falls from the ceiling and every step echoes through "
            + "the keep before the final challenge begins";
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.render(player, map, longMessage);

        String visibleOutput = stripAnsi(outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(visibleOutput.lines().allMatch(line -> line.length() <= 80),
            "No rendered game line may exceed the 80-column screen.");
        assertTrue(visibleOutput.contains("STATUS :"));
        assertTrue(visibleOutput.contains("final challenge begins"));
    }

    @Test
    void titleScreenUsesDefeatStyleArtAndDimPrompt() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);
        renderer.renderTitleScreen(true);

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ARNOST - LEGEND OF THE FALLEN KEEP"));
        assertTrue(output.contains("THE FALLEN KEEP AWAITS"));
        assertTrue(output.contains("/____\\____/       \\____/____\\"),
            "The title screen must show a compact castle rather than a tall house.");
        assertTrue(output.contains("| [] | [] |"),
            "The castle should have visible battlements.");
        assertTrue(output.contains("Press [L] to load the saved keep."));
        assertTrue(output.contains(ANSIColor.CRIMSON + "Branislav Juhas"),
            "Branislav must be credited in red.");
        assertTrue(output.contains("Simon Stefanik"));
        assertTrue(output.contains("Adam Mistrik"));
        assertTrue(output.contains("Tetiana Tkachenko"));
        assertTrue(output.contains(ANSIColor.SHADOW_GRAY + "  Press [ENTER]"),
            "The Press Enter prompt must be dim rather than bright white.");
        assertFalse(output.contains(ANSIColor.BRIGHT_WHITE + "  Press [ENTER]"));
    }

    private static void assertMonstersBelongToLevel(DungeonMap map, int level) {
        Set<String> knownMonsters = Set.copyOf(MonsterCatalog.spawnableIdsForLevel(level));
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                Tile tile = map.getTile(x, y);
                String monsterId = tile.getMonsterId();
                if (monsterId == null) {
                    continue;
                }
                assertTrue(knownMonsters.contains(monsterId),
                    "Unexpected monster id generated on level " + level + ": " + monsterId);
                assertEquals(MonsterCatalog.strengthForLevel(monsterId, level),
                    tile.getMonsterStrength(),
                    "Generated monsters must use their level strength.");
            }
        }
    }

    private static int countPotions(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).hasPotion()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countTraps(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).hasTrap()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countWalls(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).isWall()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countItems(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (map.getTile(x, y).getItemId() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countItemId(DungeonMap map, String itemId) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (itemId.equals(map.getTile(x, y).getItemId())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String stripAnsi(String output) {
        return output.replaceAll("\u001B\\[[0-9;]*[A-Za-z]", "");
    }

    private static String normalizePanelText(String output) {
        return stripAnsi(output).lines()
            .filter(line -> line.contains("   |   "))
            .map(line -> line.substring(line.indexOf("   |   ") + 7))
            .map(line -> line.replace("|", " "))
            .collect(Collectors.joining(" "))
            .replaceAll("\\s+", " ");
    }
}
