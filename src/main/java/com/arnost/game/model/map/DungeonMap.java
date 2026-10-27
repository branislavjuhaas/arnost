package com.arnost.game.model.map;

import java.io.Serializable;

public class DungeonMap implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 7;

    private final Tile[][] grid = new Tile[SIZE][SIZE];
    private int dragonLairX = -1;
    private int dragonLairY = -1;
    private int exitX = -1;
    private int exitY = -1;
    private long generationSeed;
    private int generationLevel = 1;

    public DungeonMap() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                grid[y][x] = new Tile(x, y, "CORRIDOR");
            }
        }
        grid[6][3].setType("START");
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return null;
        }
        return grid[y][x];
    }

    public long getGenerationSeed() { return generationSeed; }
    public int getGenerationLevel() { return generationLevel; }
    public int getLevel() { return generationLevel; }

    public void setGenerationMetadata(long generationSeed, int generationLevel) {
        this.generationSeed = generationSeed;
        this.generationLevel = generationLevel;
    }

    public boolean hasDragonLairPosition() {
        return dragonLairX >= 0 && dragonLairY >= 0;
    }

    public void setDragonLairPosition(int x, int y) {
        if (getTile(x, y) == null) {
            throw new IllegalArgumentException("Dragon lair must be inside the map.");
        }
        this.dragonLairX = x;
        this.dragonLairY = y;
    }

    public void clearDragonLairPosition() {
        this.dragonLairX = -1;
        this.dragonLairY = -1;
    }

    public int getDragonLairX() { return dragonLairX; }
    public int getDragonLairY() { return dragonLairY; }

    public Tile getDragonLairTile() {
        return hasDragonLairPosition() ? getTile(dragonLairX, dragonLairY) : null;
    }

    public boolean hasExitPosition() {
        return exitX >= 0 && exitY >= 0;
    }

    public void setExitPosition(int x, int y) {
        if (getTile(x, y) == null) {
            throw new IllegalArgumentException("Exit must be inside the map.");
        }
        this.exitX = x;
        this.exitY = y;
    }

    public int getExitX() { return exitX; }
    public int getExitY() { return exitY; }

    public Tile getExitTile() {
        return hasExitPosition() ? getTile(exitX, exitY) : null;
    }
}
