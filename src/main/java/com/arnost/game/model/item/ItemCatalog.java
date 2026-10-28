package com.arnost.game.model.item;

import com.arnost.game.data.Json;
import com.arnost.game.view.ANSIColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Item definitions loaded from {@code data/items.json}.
 *
 * <p>The JSON file is the source of truth for names, bonuses, colors, art,
 * lootable order, and quest-item flags. Keeping the data outside the Java
 * source lets content changes be reviewed without touching game logic.</p>
 */
public final class ItemCatalog {
    private static final Map<String, Object> DATA =
        Json.readObject("/data/items.json");

    private static final List<String> LOOTABLE_IDS = List.copyOf(
        Json.stringList(DATA.get("lootableIds"), "items.lootableIds"));

    private static final Map<String, ItemType> ITEMS = loadItems();
    private static final Set<String> QUEST_ITEM_IDS = loadQuestItemIds();

    private ItemCatalog() {
    }

    private static Map<String, ItemType> loadItems() {
        List<Map<String, Object>> definitions =
            Json.objectList(DATA.get("items"), "items.items");
        Map<String, ItemType> items = new LinkedHashMap<>();

        for (Map<String, Object> definition : definitions) {
            String id = Json.string(definition, "id");
            ItemType item = new ItemType(
                id,
                Json.string(definition, "name"),
                Json.integer(definition, "combatBonus"),
                ANSIColor.named(Json.string(definition, "color")),
                List.copyOf(Json.stringList(definition.get("art"), "items." + id + ".art"))
            );
            if (items.putIfAbsent(id, item) != null) {
                throw new IllegalStateException("Duplicate item id in items.json: " + id);
            }
        }
        return Collections.unmodifiableMap(items);
    }

    private static Set<String> loadQuestItemIds() {
        List<Map<String, Object>> definitions =
            Json.objectList(DATA.get("items"), "items.items");
        Set<String> questItems = new LinkedHashSet<>();
        for (Map<String, Object> definition : definitions) {
            if (Boolean.TRUE.equals(definition.get("questItem"))) {
                questItems.add(Json.string(definition, "id"));
            }
        }
        return Collections.unmodifiableSet(questItems);
    }

    public static ItemType get(String id) {
        return id == null ? null : ITEMS.get(id);
    }

    public static ItemType randomLoot(Random random) {
        return ITEMS.get(LOOTABLE_IDS.get(random.nextInt(LOOTABLE_IDS.size())));
    }

    public static List<String> lootableIds() {
        return LOOTABLE_IDS;
    }

    public static int combatBonus(String id) {
        ItemType item = get(id);
        return item == null ? 0 : item.combatBonus();
    }

    public static String nameOf(String id) {
        ItemType item = get(id);
        return item == null ? id : item.name();
    }

    public static boolean isQuestItem(String id) {
        return id != null && QUEST_ITEM_IDS.contains(id);
    }
}
