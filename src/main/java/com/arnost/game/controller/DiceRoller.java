package com.arnost.game.controller;
import java.util.Random;

public interface DiceRoller {
    int roll2d6();
    class RandomDiceRoller implements DiceRoller {
        private final Random random = new Random();
        @Override public int roll2d6() { return (random.nextInt(6) + 1) + (random.nextInt(6) + 1); }
    }
}