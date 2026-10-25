package com.arnost.game.model.state;
public class GameState {
    private boolean gameWon = false, playerDead = false;
    public boolean isGameWon() { return gameWon; }
    public void setGameWon(boolean gameWon) { this.gameWon = gameWon; }
    public boolean isPlayerDead() { return playerDead; }
}