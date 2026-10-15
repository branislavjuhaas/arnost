package com.arnost.game.model.map;

public class Tile {
    private final int x, y;
    private String type;
    public Tile(int x, int y, String type) { this.x = x; this.y = y; this.type = type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}