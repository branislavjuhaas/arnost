package com.arnost.game.model.entity;
import com.arnost.game.model.item.Inventory;

public class Player {
    private int hp = 5, maxHp = 5;
    private int x = 3, y = 6, prevX = 3, prevY = 6;
    private final Inventory inventory = new Inventory();
    public Player(String name, int hp, int maxHp) { this.hp = hp; this.maxHp = maxHp; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void takeDamage(int dmg) { this.hp = Math.max(0, this.hp - dmg); }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.prevX = this.x; this.prevY = this.y; this.x = x; this.y = y; }
    public void revertPosition() { this.x = this.prevX; this.y = this.prevY; }
    public Inventory getInventory() { return inventory; }
}