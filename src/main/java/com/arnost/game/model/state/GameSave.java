package com.arnost.game.model.state;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;

import java.io.Serializable;

public record GameSave(
    long seed,
    Player player,
    DungeonMap[] levels,
    GameState gameState
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
