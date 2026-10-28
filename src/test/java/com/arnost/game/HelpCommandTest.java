package com.arnost.game;

import com.arnost.game.controller.GameEngine;
import com.arnost.game.controller.MapGenerator;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpCommandTest {

    @Test
    void helpScreenFitsTheStandardScreenWidth() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        TUIRenderer renderer = new TUIRenderer(
            new PrintStream(outputBuffer, true, StandardCharsets.UTF_8), false);

        renderer.renderHelp();

        String visibleOutput = outputBuffer.toString(StandardCharsets.UTF_8)
            .replaceAll("\u001B\\[[0-9;]*m", "");
        assertTrue(visibleOutput.lines().allMatch(line -> line.length() <= 80),
            "Every help line must fit inside the 80-column screen.");
    }

    @Test
    void helpCommandShowsInstructionsAndReturnsToTheGame() {
        String commands = String.join("\n",
            "",              // Dismiss title screen banner
            "",              // Dismiss the prologue cutscene
            "help",          // Open the help screen
            "",              // Dismiss the help screen
            "go north",      // Game must still accept commands afterwards
            "quit"
        ) + "\n";

        InputStream inputBuffer = new ByteArrayInputStream(commands.getBytes());
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream testPrintStream = new PrintStream(outputBuffer);

        DungeonMap map = MapGenerator.generateSeededMap(99999L);
        Player player = new Player("Sir Arnost", 5, 5);
        GameState gameState = new GameState();
        TUIRenderer renderer = new TUIRenderer(testPrintStream, false);

        GameEngine engine = new GameEngine(player, map, gameState, inputBuffer, renderer,
            new ScriptedDiceRoller(12));
        engine.startLoop();

        String output = outputBuffer.toString();
        assertTrue(output.contains("PROLOGUE"), "The prologue must be shown before the level.");
        assertTrue(output.contains("HOW TO PLAY"), "Help screen must be rendered.");
        assertTrue(output.contains("go north"), "Help screen must list movement commands.");
        assertTrue(output.contains("2d6"), "Help screen must explain the combat roll.");
        assertTrue(output.contains("Ashgorath has 3 lives"),
            "Help screen must explain the dragon's three lives.");
        assertTrue(output.contains("2d6 sum of 9 or more"),
            "Help screen must explain the dragon's strike threshold.");
        assertTrue(output.contains("Type 'help' for instructions."), "Game screen must advertise the help command.");
        assertEquals(5, player.getY(), "The game must continue accepting commands after help is closed.");
    }
}
