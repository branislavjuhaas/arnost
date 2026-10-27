package com.arnost.game;

import com.arnost.game.controller.*;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class FullGameplaySimulationTest {

    private ByteArrayOutputStream outputBuffer;
    private PrintStream testPrintStream;

    @BeforeEach
    void setUp() {
        outputBuffer = new ByteArrayOutputStream();
        testPrintStream = new PrintStream(outputBuffer);
    }

    @Test
    void testFullWinningScriptedPlaythrough() {
        String commands = String.join("\n",
            "",              // Dismiss title screen banner
            "go north",      // Move to [3,5] (Rat)
            "roll",          // Defeat Rat -> Dagger (+1)
            "go north",      // Move to [3,4] (Spider)
            "roll",          // Defeat Spider -> Saber (+2)
            "go north",      // Move to [3,3] (Mummy)
            "roll",          // Defeat Mummy -> Fireball (+1, Total Bonus = 4)
            "go north",      // Move to [3,2] (Dist 4)
            "go north",      // Move to [3,1] (Dist 5 >= 5 AND Bonus 4 >= 4 -> Dragon Lair Spawns!)
            "roll",          // Defeat Dragon (12 + 4 = 16 vs 15) -> Crown Jewels
            "quit"
        ) + "\n";

        InputStream inputBuffer = new ByteArrayInputStream(commands.getBytes());
        ScriptedDiceRoller scriptedDice = new ScriptedDiceRoller(10, 10, 11, 12);

        DungeonMap map = MapGenerator.generateSeededMap(99999L);
        
        // Explicitly set monster encounters along the test path
        map.getTile(3, 5).setMonsterId("rat");
        map.getTile(3, 4).setMonsterId("spider");
        map.getTile(3, 3).setMonsterId("mummy");
        map.getTile(3, 2).setMonsterId("guardian");
        map.getTile(3, 1).setMonsterId("guardian");

        Player player = new Player("Sir Arnost", 5, 5);
        GameState gameState = new GameState();
        TUIRenderer renderer = new TUIRenderer(testPrintStream, false);

        GameEngine engine = new GameEngine(player, map, gameState, inputBuffer, renderer, scriptedDice);
        engine.startLoop();

        assertTrue(gameState.isGameWon(), "Game state must register Victory after defeating the Dragon.");
        assertFalse(gameState.isPlayerDead(), "Player must be alive at the end of a winning playthrough.");
        assertTrue(player.getInventory().contains("crown_jewels"), "Player must hold Crown Jewels upon victory.");
    }
}