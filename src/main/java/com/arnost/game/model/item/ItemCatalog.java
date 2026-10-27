package com.arnost.game.model.item;

import com.arnost.game.view.ANSIColor;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ItemCatalog {
    private static final List<String> LOOTABLE_IDS = List.of(
        "dagger",
        "short_sword",
        "leather_armor",
        "silver_ring",
        "saber",
        "fireball",
        "war_axe",
        "halberd"
    );

    private static final Map<String, ItemType> ITEMS = Map.ofEntries(
        Map.entry("dagger", new ItemType(
            "dagger", "Iron Dagger", 1, ANSIColor.BRIGHT_WHITE,
            List.of(
                "   /\\    ",
                "  /  \\   ",
                "  |__|   "
            )
        )),
        Map.entry("short_sword", new ItemType(
            "short_sword", "Short Sword", 1, ANSIColor.BRIGHT_WHITE,
            List.of(
                "   /|    ",
                "  / |    ",
                " /__|    "
            )
        )),
        Map.entry("leather_armor", new ItemType(
            "leather_armor", "Leather Armor", 1, ANSIColor.BONE,
            List.of(
                "  ___    ",
                " /   \\   ",
                " \\___/   "
            )
        )),
        Map.entry("silver_ring", new ItemType(
            "silver_ring", "Silver Ring", 1, ANSIColor.SKY_BLUE,
            List.of(
                "  .-.    ",
                " (   )   ",
                "  '-'    "
            )
        )),
        Map.entry("saber", new ItemType(
            "saber", "Cavalry Saber", 2, ANSIColor.EMERALD,
            List.of(
                "   __    ",
                "  / /    ",
                " /_/     "
            )
        )),
        Map.entry("war_axe", new ItemType(
            "war_axe", "War Axe", 3, ANSIColor.LAVA,
            List.of(
                "  _____  ",
                " /  _  \\ ",
                "  |_|    "
            )
        )),
        Map.entry("halberd", new ItemType(
            "halberd", "Iron Halberd", 3, ANSIColor.GOLD,
            List.of(
                "  /\\     ",
                " /--\\    ",
                "  ||     "
            )
        )),
        Map.entry("runeblade", new ItemType(
            "runeblade", "Runeblade", 4, ANSIColor.PURPLE,
            List.of(
                "  * /\\ * ",
                "   /  \\  ",
                "   |__|  "
            )
        )),
        Map.entry("fireball", new ItemType(
            "fireball", "Fireball Scroll", 1, ANSIColor.LAVA,
            List.of(
                "  _____  ",
                " /~~~~~\\ ",
                " \\_____/ "
            )
        )),
        Map.entry("crown_jewels", new ItemType(
            "crown_jewels", "Crown Jewels", 0, ANSIColor.GOLD,
            List.of(
                "  .-. .-. ",
                "  |_| |_| ",
                "   \\___/  "
            )
        )),
        Map.entry("key", new ItemType(
            "key", "Dungeon Key", 0, ANSIColor.GOLD,
            List.of(
                "  .--.   ",
                "  |__|   ",
                "  /  \\   "
            )
        )),
        Map.entry("excalibur", new ItemType(
            "excalibur", "Excalibur", 4, ANSIColor.GOLD,
            List.of(
                "   /\\    ",
                "  /  \\   ",
                "  |__|   "
            )
        ))
    );

    private ItemCatalog() {
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
        return "key".equals(id) || "excalibur".equals(id);
    }
}
