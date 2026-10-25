package com.arnost.game;
import com.arnost.game.controller.DiceRoller;

public class ScriptedDiceRoller implements DiceRoller {
    private final int[] rolls;
    private int idx = 0;
    public ScriptedDiceRoller(int... rolls) { this.rolls = rolls; }
    @Override public int roll2d6() {
        if (idx >= rolls.length) return rolls[rolls.length - 1];
        return rolls[idx++];
    }
}