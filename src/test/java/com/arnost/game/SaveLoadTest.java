package com.arnost.game;

import com.arnost.game.controller.GameEngine;
import com.arnost.game.controller.SaveManager;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.state.GameSave;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveLoadTest {

    @TempDir
    Path tempDir;

    @Test
    void saveFileKeepsAllThreePreGeneratedLevels() {
        Path savePath = tempDir.resolve("levels-save.dat");
        SaveManager saves = new SaveManager(savePath);
        GameEngine engine = new GameEngine(
            new Player("Sir Arnost", 5, 5),
            424242L,
            new GameState(),
            input("", "", "quit"),
            new TUIRenderer(new PrintStream(new ByteArrayOutputStream(), true,
                StandardCharsets.UTF_8), false),
            new ScriptedDiceRoller(12),
            saves
        );

        engine.startLoop();

        assertTrue(saves.exists(), "Starting a run must create a savefile.");
        GameSave save = saves.load().orElseThrow();
        assertEquals(3, save.levels().length);
        for (int i = 0; i < 3; i++) {
            assertNotNull(save.levels()[i]);
            assertEquals(i + 1, save.levels()[i].getGenerationLevel());
        }
        assertEquals(1, save.gameState().getCurrentLevel());
    }

    @Test
    void titleLoadRestoresPotionTileAndRunState() {
        Path savePath = tempDir.resolve("potion-save.dat");
        SaveManager saves = new SaveManager(savePath);

        DungeonMap map = new DungeonMap();
        map.getTile(3, 5).setHasPotion(true);
        Player player = new Player("Sir Arnost", 3, 5);
        GameState state = new GameState();

        GameEngine firstRun = new GameEngine(
            player,
            map,
            state,
            input("", "", "n", "quit"),
            new TUIRenderer(new PrintStream(new ByteArrayOutputStream(), true,
                StandardCharsets.UTF_8), false),
            new ScriptedDiceRoller(12),
            saves
        );
        firstRun.startLoop();

        assertTrue(saves.exists());
        assertTrue(map.getTile(3, 5).hasPotion(),
            "The saved run must preserve the unconsumed ground potion.");

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        GameEngine secondRun = new GameEngine(
            new Player("Sir Arnost", 5, 5),
            new DungeonMap(),
            new GameState(),
            input("l", "drink", "quit"),
            new TUIRenderer(new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false),
            new ScriptedDiceRoller(12),
            saves
        );
        secondRun.startLoop();

        assertEquals(3, secondRun.getPlayer().getX());
        assertEquals(5, secondRun.getPlayer().getY());
        assertEquals(5, secondRun.getPlayer().getHp(),
            "Loading and drinking must restore the saved player's HP.");
        assertEquals(1, secondRun.getGameState().getCurrentLevel());
        assertFalse(secondRun.getCurrentMap().getTile(3, 5).hasPotion(),
            "Drinking after a load must consume the persisted potion.");

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Press [L] to load the saved keep."),
            "The title screen must advertise loading when a savefile exists.");
        assertTrue(output.contains("The saved keep remembers you, Sir Arnost."));
        assertFalse(output.contains("PROLOGUE"),
            "Loading a saved game must skip the new-game prologue.");
    }

    @Test
    void saveFilePreservesRemainingDragonLives() throws Exception {
        Path savePath = tempDir.resolve("dragon-lives-save.dat");
        SaveManager saves = new SaveManager(savePath);

        DungeonMap levelThree = new DungeonMap();
        Tile dragon = levelThree.getTile(3, 1);
        dragon.setType("DRAGON_LAIR");
        dragon.setMonsterId("dragon");
        dragon.setMonsterStrength(37);
        dragon.setMonsterLives(1);
        DungeonMap[] levels = {new DungeonMap(), new DungeonMap(), levelThree};

        saves.save(new GameSave(7L, new Player("Sir Arnost", 3, 5), levels, new GameState()));

        GameSave loaded = saves.load().orElseThrow();
        assertEquals(1, loaded.levels()[2].getTile(3, 1).getMonsterLives(),
            "The dragon's remaining lives must survive a save and load.");
    }

    @Test
    void saveFilePreservesConcealedMonsterStrength() throws Exception {
        Path savePath = tempDir.resolve("hidden-strength-save.dat");
        SaveManager saves = new SaveManager(savePath);

        DungeonMap levelOne = new DungeonMap();
        Tile hiddenMonster = levelOne.getTile(3, 5);
        hiddenMonster.setMonsterId("rat");
        hiddenMonster.setMonsterStrength(5);
        hiddenMonster.setMonsterStrengthHidden(true);
        DungeonMap[] levels = {levelOne, new DungeonMap(), new DungeonMap()};

        saves.save(new GameSave(7L, new Player("Sir Arnost", 3, 5), levels, new GameState()));

        GameSave loaded = saves.load().orElseThrow();
        Tile loadedMonster = loaded.levels()[0].getTile(3, 5);
        assertTrue(loadedMonster.isMonsterStrengthHidden(),
            "A concealed monster must remain concealed after loading.");
        assertEquals(5, loadedMonster.getMonsterStrength());
    }

    private static ByteArrayInputStream input(String... lines) {
        return new ByteArrayInputStream(
            (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
