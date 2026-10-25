package com.arnost.game.controller;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;
import java.io.InputStream;
import java.util.Scanner;

public class GameEngine {
    private final Player player;
    private final DungeonMap map;
    private final GameState gameState;
    private final Scanner scanner;
    private final TUIRenderer renderer;
    private final DiceRoller diceRoller;

    public GameEngine(Player player, DungeonMap map, GameState gameState, InputStream in, TUIRenderer renderer, DiceRoller diceRoller) {
        this.player = player; this.map = map; this.gameState = gameState;
        this.scanner = new Scanner(in); this.renderer = renderer; this.diceRoller = diceRoller;
    }

    public void startLoop() {
        renderer.render(player, map, "Ready");
        while (!gameState.isGameWon() && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equals("quit")) break;
            if (line.equals("go north")) player.setPosition(player.getX(), player.getY() - 1);
            else if (line.equals("roll")) {
                player.getInventory().addItem("crown_jewels");
                gameState.setGameWon(true);
            }
        }
    }
}