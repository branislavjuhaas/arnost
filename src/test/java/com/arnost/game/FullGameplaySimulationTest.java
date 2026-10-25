package com.arnost.game;

import com.arnost.game.controller.*;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class FullGameplaySimulationTest {
    @Test
    void testFullWinningScriptedPlaythrough() {
        String commands = "go north\nroll\nquit\n";
        InputStream inputBuffer = new ByteArrayInputStream(commands.getBytes());
        ScriptedDiceRoller scriptedDice = new ScriptedDiceRoller(12);

        DungeonMap map = new DungeonMap();
        Player player = new Player("Sir Arnost", 5, 5);
        GameState gameState = new GameState();
        TUIRenderer renderer = new TUIRenderer(new PrintStream(new ByteArrayOutputStream()), false);

        GameEngine engine = new GameEngine(player, map, gameState, inputBuffer, renderer, scriptedDice);
        engine.startLoop();

        assertTrue(gameState.isGameWon());
    }
}