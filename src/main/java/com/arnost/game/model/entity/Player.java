package com.arnost.game.model.entity;

import com.arnost.game.model.item.Inventory;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STARTING_HP = 3;
    public static final int STARTING_MAX_HP = 5;

    private String name;
    private int hp = 5;
    private int maxHp = 5;
    private int x = 3;
    private int y = 6;
    private int prevX = 3;
    private int prevY = 6;
    private final Inventory inventory = new Inventory();

    public Player(String name, int hp, int maxHp) {
        this.name = name == null || name.isBlank() ? "Sir Arnost" : name;
        this.hp = hp;
        this.maxHp = maxHp;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(maxHp, hp));
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - Math.max(0, dmg));
    }

    public int heal(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = hp;
        this.hp = Math.min(maxHp, this.hp + amount);
        return this.hp - before;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getPrevX() { return prevX; }
    public int getPrevY() { return prevY; }

    public void setPosition(int x, int y) {
        this.prevX = this.x;
        this.prevY = this.y;
        this.x = x;
        this.y = y;
    }

    public void teleportTo(int x, int y) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
    }

    public void revertPosition() {
        this.x = this.prevX;
        this.y = this.prevY;
    }

    public Inventory getInventory() { return inventory; }

    public void restoreFrom(Player other) {
        if (other == null) {
            return;
        }
        this.name = other.name;
        this.hp = other.hp;
        this.maxHp = other.maxHp;
        this.x = other.x;
        this.y = other.y;
        this.prevX = other.prevX;
        this.prevY = other.prevY;
        this.inventory.replaceWith(other.inventory.getItems());
    }

    public void resetForNewGame() {
        this.hp = Math.min(STARTING_HP, this.maxHp);
        teleportTo(3, 6);
        this.inventory.clear();
    }
}
