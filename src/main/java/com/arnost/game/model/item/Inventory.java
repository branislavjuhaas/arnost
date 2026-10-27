package com.arnost.game.model.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<String> items = new ArrayList<>();

    public boolean addItem(String item) {
        if (item == null) {
            return false;
        }
        items.add(item);
        return true;
    }

    public boolean contains(String item) { return items.contains(item); }

    public boolean removeItem(String item) {
        return items.remove(item);
    }

    public List<String> getItems() { return Collections.unmodifiableList(items); }
    public int size() { return items.size(); }
    public String getLatestItem() { return items.isEmpty() ? null : items.get(items.size() - 1); }

    public void clear() { items.clear(); }

    public void replaceWith(Collection<String> replacement) {
        items.clear();
        if (replacement != null) {
            items.addAll(replacement);
        }
    }

    public int getCombatBonus() {
        int bonus = 0;
        for (String item : items) {
            bonus += ItemCatalog.combatBonus(item);
        }
        return bonus;
    }
}
