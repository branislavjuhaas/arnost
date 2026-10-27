package com.arnost.game.model.monster;

import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.view.ANSIColor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class MonsterCatalog {
    private static final List<String> SPAWNABLE_IDS =
        List.of("rat", "spider", "venom_spider", "mummy", "plague_mummy",
            "guardian", "wraith");
    private static final List<String> LEVEL_ONE_SPAWNS = List.of("rat", "spider");
    private static final List<String> LEVEL_TWO_SPAWNS =
        List.of("spider", "venom_spider", "mummy", "guardian");
    private static final List<String> LEVEL_THREE_SPAWNS =
        List.of("mummy", "plague_mummy", "guardian", "wraith");

    private static final List<String> LEVEL_ONE_IDS =
        List.of("rat", "spider", "warden");
    private static final List<String> LEVEL_TWO_IDS =
        List.of("spider", "venom_spider", "mummy", "guardian", "mummy_lord");
    private static final List<String> LEVEL_THREE_IDS =
        List.of("mummy", "plague_mummy", "guardian", "wraith", "knight");

    private static final Set<String> VENOMOUS_IDS =
        Set.of("venom_spider", "plague_mummy");

    private static final Map<String, MonsterType> MONSTERS = Map.ofEntries(
        Map.entry("rat", new MonsterType(
            "rat", "Giant Rat", 6, 0, "dagger", ANSIColor.GOLD,
            List.of(
                "  /\\_/\\  ",
                " ( o.o ) ",
                "  > ^ <  "
            )
        )),
        Map.entry("spider", new MonsterType(
            "spider", "Cave Spider", 8, 0, "short_sword", ANSIColor.PURPLE,
            List.of(
                "  /\\ /\\  ",
                " <  o  > ",
                "  /   \\  "
            )
        )),
        Map.entry("venom_spider", new MonsterType(
            "venom_spider", "Venom Spider", 15, 0, "short_sword", ANSIColor.EMERALD,
            List.of(
                "  /\\ /\\  ",
                " <  V  > ",
                "  /   \\  "
            )
        )),
        Map.entry("mummy", new MonsterType(
            "mummy", "Restless Mummy", 18, 0, "saber", ANSIColor.BONE,
            List.of(
                "  _____  ",
                " / o o \\ ",
                " \\_____/ "
            )
        )),
        Map.entry("plague_mummy", new MonsterType(
            "plague_mummy", "Plague Mummy", 25, 0, "saber", ANSIColor.LAVA,
            List.of(
                "  _____  ",
                " / x x \\ ",
                " \\__V__/ "
            )
        )),
        Map.entry("guardian", new MonsterType(
            "guardian", "Iron Guardian", 21, 0, "halberd", ANSIColor.SKY_BLUE,
            List.of(
                "   [H]   ",
                "  /|_|\\  ",
                "   | |   "
            )
        )),
        Map.entry("wraith", new MonsterType(
            "wraith", "Barrow Wraith", 31, 0, "runeblade", ANSIColor.PURPLE,
            List.of(
                "  .---.  ",
                " ( o o ) ",
                "  ~   ~  "
            )
        )),
        Map.entry("warden", new MonsterType(
            "warden", "Goblin Warden", 10, 0, "key", ANSIColor.EMERALD,
            List.of(
                "  /\\ /\\  ",
                " ( o.o ) ",
                "  /|_|\\  "
            )
        )),
        Map.entry("mummy_lord", new MonsterType(
            "mummy_lord", "Mummy Lord", 22, 0, "key", ANSIColor.GOLD,
            List.of(
                "  ^^^^^  ",
                " / o o \\ ",
                " \\_____/ "
            )
        )),
        Map.entry("knight", new MonsterType(
            "knight", "Knight Guardian", 33, 0, "excalibur", ANSIColor.SKY_BLUE,
            List.of(
                "  /---\\  ",
                " | o o | ",
                "  \\___/  "
            )
        )),
        Map.entry("dragon", new MonsterType(
            "dragon", "Ashgorath the Dragon", 37, 0, "crown_jewels", ANSIColor.CRIMSON,
            List.of(
                "  /\\___/\\  ",
                " <  o o  > ",
                "  \\_^^^_/  "
            )
        ))
    );

    private static final Map<String, Double> DROP_CHANCES = Map.ofEntries(
        Map.entry("rat", 0.5),
        Map.entry("spider", 1.0),
        Map.entry("venom_spider", 1.0),
        Map.entry("mummy", 1.0),
        Map.entry("plague_mummy", 1.0),
        Map.entry("guardian", 1.0),
        Map.entry("wraith", 1.0),
        Map.entry("warden", 1.0),
        Map.entry("mummy_lord", 1.0),
        Map.entry("knight", 1.0),
        Map.entry("dragon", 1.0)
    );

    private static final Map<String, List<String>> LOOT_TABLES = Map.ofEntries(
        Map.entry("rat", List.of("dagger", "short_sword", "silver_ring", "leather_armor")),
        Map.entry("spider", List.of("dagger", "short_sword", "silver_ring", "leather_armor")),
        Map.entry("venom_spider", List.of("dagger", "short_sword", "silver_ring", "leather_armor")),
        Map.entry("mummy", List.of("saber", "leather_armor", "fireball")),
        Map.entry("plague_mummy", List.of("saber", "leather_armor", "fireball")),
        Map.entry("guardian", List.of("war_axe", "halberd")),
        Map.entry("wraith", List.of("runeblade", "halberd")),
        Map.entry("dragon", List.of("crown_jewels"))
    );

    private static final Map<String, String> QUEST_DROPS = Map.of(
        "warden", "key",
        "mummy_lord", "key",
        "knight", "excalibur"
    );

    private MonsterCatalog() {
    }

    public static MonsterType get(String id) {
        return id == null ? null : MONSTERS.get(id);
    }

    public static MonsterType randomSpawn(Random random) {
        String id = SPAWNABLE_IDS.get(random.nextInt(SPAWNABLE_IDS.size()));
        return MONSTERS.get(id);
    }

    public static MonsterType randomSpawn(Random random, int level) {
        List<String> ids = randomSpawnIdsForLevel(level);
        String id = ids.get(random.nextInt(ids.size()));
        return MONSTERS.get(id);
    }

    public static List<String> spawnableIds() {
        return SPAWNABLE_IDS;
    }

    public static List<String> spawnableIdsForLevel(int level) {
        return switch (level) {
            case 1 -> LEVEL_ONE_IDS;
            case 2 -> LEVEL_TWO_IDS;
            case 3 -> LEVEL_THREE_IDS;
            default -> SPAWNABLE_IDS;
        };
    }

    public static List<String> randomSpawnIdsForLevel(int level) {
        return switch (level) {
            case 1 -> LEVEL_ONE_SPAWNS;
            case 2 -> LEVEL_TWO_SPAWNS;
            case 3 -> LEVEL_THREE_SPAWNS;
            default -> SPAWNABLE_IDS;
        };
    }

    public static int baseStrengthOf(String id) {
        MonsterType monster = get(id);
        return monster == null ? 5 : monster.baseStrength();
    }

    /**
     * Base strength for a monster as it appears on a given level. A shared
     * monster is tougher deeper in the keep, before the dungeon menace scaling
     * from the player's own combat bonus is applied.
     */
    public static int strengthForLevel(String id, int level) {
        return switch (level) {
            case 1 -> switch (id) {
                case "rat" -> 6;
                case "spider" -> 8;
                case "warden" -> 10;
                default -> baseStrengthOf(id);
            };
            case 2 -> switch (id) {
                case "spider" -> 13;
                case "venom_spider" -> 15;
                case "mummy" -> 18;
                case "guardian" -> 21;
                case "mummy_lord" -> 22;
                default -> baseStrengthOf(id);
            };
            case 3 -> switch (id) {
                case "mummy" -> 23;
                case "plague_mummy" -> 25;
                case "guardian" -> 27;
                case "wraith" -> 31;
                case "knight" -> 33;
                case "dragon" -> 37;
                default -> baseStrengthOf(id);
            };
            default -> baseStrengthOf(id);
        };
    }

    /**
     * The keep answers strength with strength: every three points of the
     * player's combat bonus add one point to an encountered monster. The
     * player still keeps most of every upgrade, but enemies never fall far
     * behind a fully equipped hero.
     */
    public static int effectiveStrength(int baseStrength, int combatBonus) {
        return Math.max(1, baseStrength) + Math.max(0, combatBonus) / 3;
    }

    public static String rollLoot(String monsterId, Random random) {
        MonsterType monster = get(monsterId);
        if (monster == null) {
            return null;
        }

        String questDrop = QUEST_DROPS.get(monsterId);
        if (questDrop != null) {
            return questDrop;
        }

        double chance = DROP_CHANCES.getOrDefault(monsterId, 1.0);
        if (chance < 1.0 && random.nextDouble() >= chance) {
            return null;
        }

        List<String> table = LOOT_TABLES.get(monsterId);
        if (table == null || table.isEmpty()) {
            return monster.dropItemId();
        }
        return table.get(random.nextInt(table.size()));
    }

    public static double dropChanceOf(String monsterId) {
        if (monsterId == null) {
            return 0.0;
        }
        return QUEST_DROPS.containsKey(monsterId)
            ? 1.0
            : DROP_CHANCES.getOrDefault(monsterId, 1.0);
    }

    public static List<String> lootTableOf(String monsterId) {
        if (monsterId == null) {
            return List.of();
        }
        String questDrop = QUEST_DROPS.get(monsterId);
        if (questDrop != null) {
            return List.of(questDrop);
        }
        return LOOT_TABLES.getOrDefault(monsterId, List.of());
    }

    public static int maxLootBonusOf(String monsterId) {
        int best = 0;
        for (String itemId : lootTableOf(monsterId)) {
            best = Math.max(best, ItemCatalog.combatBonus(itemId));
        }
        return best;
    }

    public static String lootHintOf(String monsterId) {
        if (monsterId == null) {
            return "Unknown";
        }
        return switch (monsterId) {
            case "rat" -> "Light gear or nothing";
            case "spider", "venom_spider" -> "Light gear";
            case "mummy", "plague_mummy" -> "Good gear";
            case "guardian" -> "Strong gear";
            case "wraith" -> "Elite gear";
            case "warden", "mummy_lord" -> "Dungeon Key";
            case "knight" -> "Excalibur";
            case "dragon" -> "Crown Jewels";
            default -> get(monsterId) == null ? "Unknown" : "Gear";
        };
    }

    public static boolean isQuestGuardian(String monsterId) {
        return monsterId != null && QUEST_DROPS.containsKey(monsterId);
    }

    public static boolean isVenomous(String monsterId) {
        return monsterId != null && VENOMOUS_IDS.contains(monsterId);
    }

    public static int poisonDamageOf(String monsterId) {
        return isVenomous(monsterId) ? 1 : 0;
    }
}
