package com.arnost.game.controller;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
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

    public GameEngine(Player player, DungeonMap map, GameState gameState,
                      InputStream in, TUIRenderer renderer, DiceRoller diceRoller) {
        this.player = player;
        this.map = map;
        this.gameState = gameState;
        this.scanner = new Scanner(in);
        this.renderer = renderer;
        this.diceRoller = diceRoller;
    }

    public void startLoop() {
        renderer.renderTitleScreen();

        if (scanner.hasNextLine()) {
            scanner.nextLine(); // Wait for user to press ENTER
        }

        String lastMsg = "Welcome to Fallen Keep, Sir Arnost!";
        renderer.render(player, map, lastMsg);

        while (!gameState.isGameWon() && !gameState.isPlayerDead() && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("quit")) break;

            if (line.startsWith("go ")) {
                String dir = line.substring(3).trim();
                int nx = player.getX();
                int ny = player.getY();

                if (dir.equals("north")) ny--;
                else if (dir.equals("south")) ny++;
                else if (dir.equals("east")) nx++;
                else if (dir.equals("west")) nx--;

                Tile target = map.getTile(nx, ny);
                if (target != null) {
                    player.setPosition(nx, ny);
                    target.setExplored(true);

                    if (MapGenerator.calculateDistance(nx, ny) >= 5 && player.getInventory().getCombatBonus() >= 4) {
                        target.setType("DRAGON_LAIR");
                        target.setMonsterId("dragon");
                    }

                    lastMsg = "Moved " + dir + " to [" + nx + "," + ny + "]";
                } else {
                    lastMsg = "Blocked by dungeon boundary.";
                }
            } else if (line.equals("roll")) {
                Tile currentTile = map.getTile(player.getX(), player.getY());
                int roll = diceRoller.roll2d6();

                int monsterStr = 5;
                if ("dragon".equals(currentTile.getMonsterId())) monsterStr = 15;
                else if ("spider".equals(currentTile.getMonsterId())) monsterStr = 6;
                else if ("mummy".equals(currentTile.getMonsterId())) monsterStr = 7;

                CombatResolver.Outcome outcome = CombatResolver.resolveCombat(player, monsterStr, roll);
                if (outcome == CombatResolver.Outcome.VICTORY) {
                    currentTile.setCleared(true);
                    if ("dragon".equals(currentTile.getMonsterId())) {
                        player.getInventory().addItem("crown_jewels");
                        gameState.setGameWon(true);
                        lastMsg = "VICTORY! You defeated the Dragon and secured the Crown Jewels!";
                    } else if ("rat".equals(currentTile.getMonsterId())) {
                        player.getInventory().addItem("dagger");
                        lastMsg = "Defeated Giant Rat! Dropped Iron Dagger (+1 Strength).";
                    } else if ("spider".equals(currentTile.getMonsterId())) {
                        player.getInventory().addItem("saber");
                        lastMsg = "Defeated Spider! Dropped Saber (+2 Strength).";
                    } else if ("mummy".equals(currentTile.getMonsterId())) {
                        player.getInventory().addItem("fireball");
                        lastMsg = "Defeated Mummy! Dropped Fireball Scroll (+1 Strength).";
                    }
                } else if (outcome == CombatResolver.Outcome.DRAW) {
                    lastMsg = "Combat DRAW! Pushed back to previous tile.";
                } else {
                    lastMsg = "DEFEAT! Lost 1 HP and pushed back.";
                }
            }

            renderer.render(player, map, lastMsg);
        }
    }
}