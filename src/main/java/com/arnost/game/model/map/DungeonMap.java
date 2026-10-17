package com.arnost.game.model.map;

public class DungeonMap {
    public static final int SIZE = 7;
    private final Tile[][] grid = new Tile[SIZE][SIZE];
    public DungeonMap() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) grid[y][x] = new Tile(x, y, "CORRIDOR");
        }
        grid[6][3].setType("START");
    }
    public Tile getTile(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return null;
        return grid[y][x];
    }
}