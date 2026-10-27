package com.arnost.game;

import com.arnost.game.controller.*;
import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;

public class Main {
    public static void main(String[] args) {
        DungeonMap map = MapGenerator.generateSeededMap(System.currentTimeMillis());
        Player player = new Player("Sir Arnost", 5, 5);
        GameState state = new GameState();
        TUIRenderer renderer = new TUIRenderer(System.out, true);
        DiceRoller dice = new DiceRoller.RandomDiceRoller();

        GameEngine engine = new GameEngine(player, map, state, System.in, renderer, dice);
        engine.startLoop();
    }
}