package com.arnost.game;

import com.arnost.game.controller.DiceRoller;
import com.arnost.game.controller.GameEngine;
import com.arnost.game.controller.SaveManager;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;

public class Main {
    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        Player player = new Player(
            "Sir Arnost", Player.STARTING_HP, Player.STARTING_MAX_HP);
        GameState state = new GameState();
        TUIRenderer renderer = new TUIRenderer(System.out, true);
        DiceRoller dice = new DiceRoller.RandomDiceRoller();
        SaveManager saveManager = new SaveManager();

        GameEngine engine = new GameEngine(player, seed, state, System.in,
            renderer, dice, saveManager);
        engine.startLoop();
    }
}
