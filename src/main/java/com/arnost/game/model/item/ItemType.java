package com.arnost.game.model.item;

import java.util.List;

public record ItemType(
    String id,
    String name,
    int combatBonus,
    String color,
    List<String> art
) {
}
