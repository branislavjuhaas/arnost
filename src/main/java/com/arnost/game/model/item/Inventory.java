package com.arnost.game.model.item;
import java.util.*;

public class Inventory {
    private final List<String> items = new ArrayList<>();
    public boolean addItem(String item) { items.add(item); return true; }
    public boolean contains(String item) { return items.contains(item); }
    public List<String> getItems() { return Collections.unmodifiableList(items); }
    public int getCombatBonus() {
        int bonus = 0;
        if (items.contains("dagger")) bonus += 1;
        if (items.contains("saber")) bonus += 2;
        if (items.contains("halberd")) bonus += 3;
        if (items.contains("fireball")) bonus += 1;
        return bonus;
    }
}