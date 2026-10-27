package com.arnost.game.controller;
import java.util.Random;

public interface DiceRoller {
    int roll2d6();

    default DiceRoll rollDetailed() {
        int total = roll2d6();
        int first = Math.max(1, total / 2);
        int second = total - first;
        return new DiceRoll(first, second);
    }

    record DiceRoll(int first, int second) {
        public int total() { return first + second; }
    }

    class RandomDiceRoller implements DiceRoller {
        private final Random random = new Random();

        @Override
        public int roll2d6() { return rollDetailed().total(); }

        @Override
        public DiceRoll rollDetailed() {
            return new DiceRoll(random.nextInt(6) + 1, random.nextInt(6) + 1);
        }
    }
}
