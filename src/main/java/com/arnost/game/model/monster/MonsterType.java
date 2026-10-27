package com.arnost.game.model.monster;

import java.util.List;
import java.util.Random;

public record MonsterType(
    String id,
    String name,
    int baseStrength,
    int strengthVariance,
    String dropItemId,
    String color,
    List<String> art
) {
    public int minStrength() {
        return baseStrength - strengthVariance;
    }

    public int maxStrength() {
        return baseStrength + strengthVariance;
    }

    public int rollStrength(Random random) {
        if (strengthVariance <= 0) {
            return baseStrength;
        }
        int offset = random.nextInt(strengthVariance * 2 + 1) - strengthVariance;
        return baseStrength + offset;
    }
}
