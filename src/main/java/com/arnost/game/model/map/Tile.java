package com.arnost.game.model.map;

import java.io.Serializable;

public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;
    private String type;
    private boolean explored;
    private boolean cleared;
    private String monsterId;
    private int monsterStrength;
    private boolean monsterStrengthHidden;
    private String itemId;

    private boolean hasPotion;
    private boolean hasTrap;
    private boolean trapTriggered;

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
    public void setMonsterId(String monsterId) {
        this.monsterId = monsterId;
        this.monsterStrength = 0;
        this.monsterStrengthHidden = false;
    }

    public int getMonsterStrength() { return monsterStrength; }
    public void setMonsterStrength(int monsterStrength) { this.monsterStrength = monsterStrength; }

    public boolean isMonsterStrengthHidden() { return monsterStrengthHidden; }
    public void setMonsterStrengthHidden(boolean monsterStrengthHidden) {
        this.monsterStrengthHidden = monsterStrengthHidden;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public boolean hasPotion() { return hasPotion; }
    public void setHasPotion(boolean hasPotion) { this.hasPotion = hasPotion; }

    public boolean isHasPotion() { return hasPotion; }

    public boolean hasTrap() { return hasTrap; }
    public void setHasTrap(boolean hasTrap) { this.hasTrap = hasTrap; }

    public boolean isTrapTriggered() { return trapTriggered; }
    public void setTrapTriggered(boolean trapTriggered) { this.trapTriggered = trapTriggered; }

    public boolean isTrapArmed() { return hasTrap && !trapTriggered; }

    public boolean isWall() { return "WALL".equals(type); }
    public boolean isStart() { return "START".equals(type); }
    public boolean isExit() {
        return "EXIT".equals(type) || "STAIRS".equals(type) || "EXIT_STAIRS".equals(type);
    }
    public boolean isDragonLair() { return "DRAGON_LAIR".equals(type); }

    public void clearMonster() {
        this.monsterId = null;
        this.monsterStrength = 0;
        this.monsterStrengthHidden = false;
        this.cleared = false;
    }

    public void clearGroundFeatures() {
        this.hasPotion = false;
        this.hasTrap = false;
        this.trapTriggered = false;
        this.itemId = null;
    }
}
