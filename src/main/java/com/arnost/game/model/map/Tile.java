package com.arnost.game.model.map;

public class Tile {
    private final int x;
    private final int y;
    private String type;
    private boolean explored;
    private boolean cleared;
    private String monsterId;
    private String itemId;

    public Tile(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.explored = false;
        this.cleared = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isExplored() { return explored; }
    public void setExplored(boolean explored) { this.explored = explored; }

    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }

    public String getMonsterId() { return monsterId; }
    public void setMonsterId(String monsterId) { this.monsterId = monsterId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
}