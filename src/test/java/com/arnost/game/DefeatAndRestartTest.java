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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefeatAndRestartTest {

    @Test
    void defeatScreenOffersRestartAndResetsTheRun() {
        DungeonMap map = MapGenerator.generateSeededMap(99999L);
        Player player = new Player("Sir Arnost", 1, 5);
        player.getInventory().addItem("dagger");
        GameState state = new GameState();

        String commands = String.join("\n",
            "",          // Dismiss title screen
            "",          // Dismiss the prologue cutscene
            "go north",  // Enter the guaranteed rat encounter
            "roll",      // Roll 2 (+1 dagger) against strength 5 -> defeat and death
            "r",         // Restart from the defeat screen
            "quit"       // Leave the newly started game
        ) + "\n";

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream testPrintStream = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8);

        GameEngine engine = new GameEngine(
            player,
            map,
            state,
            new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
            new TUIRenderer(testPrintStream, false),
            new ScriptedDiceRoller(2)
        );
        engine.startLoop();

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("PROLOGUE"), "A new game must open with the story prologue.");
        assertEquals(1, output.lines().filter(line -> line.contains("PROLOGUE")).count(),
            "Restarting must not replay the prologue cutscene.");
        assertTrue(output.contains("YOU HAVE FALLEN"));
        assertTrue(output.contains("R.I.P."), "Defeat must render the ASCII tombstone screen.");
        assertTrue(output.contains("[R] Restart the Keep"));
        assertFalse(state.isPlayerDead(), "Restart must clear the defeated state.");
        assertFalse(state.isGameWon());
        assertEquals(3, player.getHp(), "Restart must restore the player to 3/5 HP.");
        assertEquals(5, player.getMaxHp());
        assertEquals(3, player.getX(), "Restart must return the player to the start tile.");
        assertEquals(6, player.getY(), "Restart must return the player to the start tile.");
        assertTrue(player.getInventory().getItems().isEmpty(), "Restart must clear the inventory.");
    }

    @Test
    void aFreshRunStartsAtThreeOfFiveHp() {
        Player player = new Player("Sir Arnost", 5, 5);
        player.takeDamage(2);
        player.getInventory().addItem("dagger");

        player.resetForNewGame();

        assertEquals(3, player.getHp(), "A fresh run must start with 3 HP.");
        assertEquals(5, player.getMaxHp(), "The maximum HP must remain 5.");
        assertEquals(3, player.getX());
        assertEquals(6, player.getY());
    }
}
