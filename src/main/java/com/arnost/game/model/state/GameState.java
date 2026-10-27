package com.arnost.game.model.state;

import java.io.Serializable;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 3;

    private boolean gameWon = false;
    private boolean playerDead = false;
    private int currentLevel = MIN_LEVEL;

    public boolean isGameWon() { return gameWon; }
    public void setGameWon(boolean gameWon) { this.gameWon = gameWon; }

    public boolean isPlayerDead() { return playerDead; }
    public void setPlayerDead(boolean playerDead) { this.playerDead = playerDead; }

    public int getCurrentLevel() { return currentLevel; }

    public void setCurrentLevel(int currentLevel) {
        if (currentLevel < MIN_LEVEL || currentLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                "Current level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ".");
        }
        this.currentLevel = currentLevel;
    }

    public void restoreFrom(GameState other) {
        if (other == null) {
            return;
        }
        this.gameWon = other.gameWon;
        this.playerDead = other.playerDead;
        this.currentLevel = other.currentLevel;
    }

    public void advanceLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
        }
    }

    public boolean isFinalLevel() {
        return currentLevel == MAX_LEVEL;
    }

    public void reset() {
        this.gameWon = false;
        this.playerDead = false;
        this.currentLevel = MIN_LEVEL;
    }
}
