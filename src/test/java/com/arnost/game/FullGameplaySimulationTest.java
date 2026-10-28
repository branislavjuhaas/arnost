package com.arnost.game;

import com.arnost.game.controller.GameEngine;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullGameplaySimulationTest {

    private ByteArrayOutputStream outputBuffer;
    private PrintStream testPrintStream;

    @BeforeEach
    void setUp() {
        outputBuffer = new ByteArrayOutputStream();
        testPrintStream = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8);
    }

    @Test
    void testFullWinningThreeLevelPlaythrough() {
        String commands = String.join("\n",
            "",              // Dismiss title screen
            "",              // Dismiss the prologue cutscene
            // Level 1: rat, spider, the hidden key guardian at [4,3], then the exit at [6,0].
            "n", "roll",
            "n", "roll",
            "n", "e", "roll", "w",
            "n", "n", "n",
            "e", "e", "e",
            "descend",
            "",              // Dismiss the Sunken Crypts cutscene
            // Level 2: spider, mummy, the hidden key guardian at [4,3], then the exit at [0,0].
            "n", "roll",
            "n", "roll",
            "n", "e", "roll", "w",
            "n", "n", "n",
            "w", "w", "w",
            "go stairs",
            "",              // Dismiss the Dragon's Abyss cutscene
            // Level 3: two mummies, the Knight Guardian at [4,2], then the Dragon's Lair at [3,1].
            "n", "roll",
            "n", "roll",
            "n", "n",
            "e", "roll", "w",
            "n",
            "roll",
            "roll",
            "roll",
            "quit"
        ) + "\n";

        InputStream inputBuffer = new ByteArrayInputStream(
            commands.getBytes(StandardCharsets.UTF_8));
        ScriptedDiceRoller scriptedDice = new ScriptedDiceRoller(12);

        Player player = new Player("Sir Arnost", 3, 5);
        // A veteran hero carrying a full pack: this test exercises level traversal and
        // the final quest chain rather than early-game loot pacing.
        for (int i = 0; i < 8; i++) {
            player.getInventory().addItem("runeblade");
        }
        player.getInventory().addItem("saber");
        GameState gameState = new GameState();
        TUIRenderer renderer = new TUIRenderer(testPrintStream, false);

        GameEngine engine = new GameEngine(player, 99999L, gameState, inputBuffer,
            renderer, scriptedDice);
        engine.startLoop();

        assertTrue(gameState.isGameWon(),
            "Game state must register Victory after defeating the Dragon.");
        assertFalse(gameState.isPlayerDead(),
            "Player must be alive at the end of a winning playthrough.");
        assertTrue(player.getHp() > 0,
            "Player must have HP remaining after the winning playthrough.");
        assertTrue(player.getInventory().contains("crown_jewels"),
            "Player must hold Crown Jewels upon victory.");

        Tile dragonTile = engine.getCurrentMap().getTile(3, 1);
        assertTrue(dragonTile.isCleared(), "The Dragon Lair tile must be cleared after victory.");

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("PROLOGUE"), "A new game must show the opening story.");
        assertTrue(output.contains("THE SUNKEN CRYPTS"),
            "Descending from Level 1 must show the Level 2 story cutscene.");
        assertTrue(output.contains("THE DRAGON'S ABYSS"),
            "Descending from Level 2 must show the Level 3 story cutscene.");
        assertTrue(output.contains("ARNOST :: LEVEL 1/3"));
        assertTrue(output.contains("ARNOST :: LEVEL 2/3"));
        assertTrue(output.contains("ARNOST :: LEVEL 3/3"));
        assertTrue(output.contains("KEY: [MISSING]"));
        assertTrue(output.contains("KEY: [ACQUIRED]"));
        assertTrue(output.contains("EXCALIBUR: [LOCKED]"));
        assertTrue(output.contains("EXCALIBUR: [DRAWN]"));
        assertTrue(output.contains("VICTORY - ASHGORATH IS SLAIN"),
            "Winning must render the ASCII victory screen.");
        assertTrue(output.contains("EPILOGUE - THE FALLEN KEEP IS FREE"),
            "The victory screen must present the story epilogue.");
        assertTrue(output.contains("EXCALIBUR AND THE CROWN JEWELS"),
            "The victory screen must show Excalibur and the Crown Jewels.");
        String normalized = output.replaceAll("\u001B\\[[0-9;]*[A-Za-z]", "")
            .replaceAll("\\s+", " ");
        assertTrue(normalized.contains(
                "With a final, holy strike of Excalibur, Ashgorath collapses into ashes. "
                    + "Sir Arnost emerges from the catacombs bearing the lost Crown Jewels. "
                    + "The Keep is fallen no more!"),
            "The victory screen must display the full epilogue.");
        assertFalse(output.contains("***"),
            "Victory and combat status messages must not contain leftover formatting markers.");
    }
}
