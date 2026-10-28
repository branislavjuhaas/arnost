package com.arnost.game.model.monster;

import com.arnost.game.data.Json;
import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.view.ANSIColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Monster definitions and encounter data loaded from
 * {@code data/monsters.json}.
 *
 * <p>The JSON file is the source of truth for monster stats, art, lore,
 * level-specific strength overrides, spawn pools, fixed placements, loot
 * tables, quest drops, and dragon constants.</p>
 */
public final class MonsterCatalog {
    private static final Map<String, Object> DATA =
        Json.readObject("/data/monsters.json");

    // Ashgorath ignores the normal strength comparison: he has three lives,
    // and each successful strike requires a 2d6 sum of 9 or more.
    public static final int DRAGON_LIVES = Json.integer(DATA, "dragonLives");
    public static final int DRAGON_MIN_ROLL = Json.integer(DATA, "dragonMinRoll");

    private static final int FALLBACK_BASE_STRENGTH =
        Json.integer(DATA, "fallbackBaseStrength");
    private static final int VENOM_DAMAGE = Json.integer(DATA, "venomDamage");

    private static final List<String> SPAWNABLE_IDS = List.copyOf(
        Json.stringList(DATA.get("spawnableIds"), "monsters.spawnableIds"));
    private static final Set<String> VENOMOUS_IDS = Collections.unmodifiableSet(
        new LinkedHashSet<>(Json.stringList(DATA.get("venomousIds"),
            "monsters.venomousIds")));

    private static final Map<String, MonsterType> MONSTERS = loadMonsters();
    private static final Map<Integer, List<String>> LEVEL_IDS = loadLevelLists("levelIds");
    private static final Map<Integer, List<String>> LEVEL_SPAWNS =
        loadLevelLists("randomSpawnIds");
    private static final Map<Integer, Map<String, Integer>> LEVEL_STRENGTHS =
        loadLevelStrengths();
    private static final Map<Integer, List<FixedPlacement>> FIXED_PLACEMENTS =
        loadFixedPlacements();

    private static final Map<String, Double> DROP_CHANCES = Collections.unmodifiableMap(
        Json.doubleMap(DATA.get("dropChances"), "monsters.dropChances"));
    private static final Map<String, List<String>> LOOT_TABLES =
        Collections.unmodifiableMap(Json.stringListMap(DATA.get("lootTables"),
            "monsters.lootTables"));
    private static final Map<String, String> QUEST_DROPS = Collections.unmodifiableMap(
        Json.stringMap(DATA.get("questDrops"), "monsters.questDrops"));
    private static final Map<String, String> LOOT_HINTS = Collections.unmodifiableMap(
        Json.stringMap(DATA.get("lootHints"), "monsters.lootHints"));

    private MonsterCatalog() {
    }

    public record FixedPlacement(int x, int y, String monsterId) {
    }

    private static Map<String, MonsterType> loadMonsters() {
        List<Map<String, Object>> definitions =
            Json.objectList(DATA.get("monsters"), "monsters.monsters");
        Map<String, MonsterType> monsters = new LinkedHashMap<>();

        for (Map<String, Object> definition : definitions) {
            String id = Json.string(definition, "id");
            MonsterType monster = new MonsterType(
                id,
                Json.string(definition, "name"),
                Json.integer(definition, "baseStrength"),
                Json.integer(definition, "strengthVariance"),
                Json.string(definition, "dropItemId"),
                ANSIColor.named(Json.string(definition, "color")),
                List.copyOf(Json.stringList(definition.get("art"),
                    "monsters." + id + ".art")),
                Json.optionalString(definition, "lore")
            );
            if (monsters.putIfAbsent(id, monster) != null) {
                throw new IllegalStateException("Duplicate monster id in monsters.json: " + id);
            }
        }
        return Collections.unmodifiableMap(monsters);
    }

    private static Map<Integer, List<String>> loadLevelLists(String key) {
        Map<String, Object> raw = Json.object(DATA.get(key), "monsters." + key);
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            int level = parseLevel(entry.getKey(), key);
            result.put(level, List.copyOf(Json.stringList(entry.getValue(),
                "monsters." + key + "." + entry.getKey())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<Integer, Map<String, Integer>> loadLevelStrengths() {
        Map<String, Object> raw = Json.object(DATA.get("levelStrengths"),
            "monsters.levelStrengths");
        Map<Integer, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            int level = parseLevel(entry.getKey(), "levelStrengths");
            result.put(level, Collections.unmodifiableMap(Json.integerMap(
                entry.getValue(), "monsters.levelStrengths." + entry.getKey())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<Integer, List<FixedPlacement>> loadFixedPlacements() {
        Map<String, Object> raw = Json.object(DATA.get("fixedPlacements"),
            "monsters.fixedPlacements");
        Map<Integer, List<FixedPlacement>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            int level = parseLevel(entry.getKey(), "fixedPlacements");
            List<Map<String, Object>> placements = Json.objectList(
                entry.getValue(), "monsters.fixedPlacements." + entry.getKey());
            List<FixedPlacement> levelPlacements = new ArrayList<>(placements.size());
            for (Map<String, Object> placement : placements) {
                levelPlacements.add(new FixedPlacement(
                    Json.integer(placement, "x"),
                    Json.integer(placement, "y"),
                    Json.string(placement, "monsterId")
                ));
            }
            result.put(level, List.copyOf(levelPlacements));
        }
        return Collections.unmodifiableMap(result);
    }

    private static int parseLevel(String value, String context) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid level key in " + context + ": " + value, e);
        }
    }

    private static List<String> idsForLevel(Map<Integer, List<String>> source,
                                            int level, List<String> fallback) {
        List<String> ids = source.get(level);
        return ids == null ? fallback : ids;
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
        return idsForLevel(LEVEL_IDS, level, SPAWNABLE_IDS);
    }

    public static List<String> randomSpawnIdsForLevel(int level) {
        return idsForLevel(LEVEL_SPAWNS, level, SPAWNABLE_IDS);
    }

    public static List<FixedPlacement> fixedPlacementsForLevel(int level) {
        return FIXED_PLACEMENTS.getOrDefault(level, List.of());
    }

    public static int baseStrengthOf(String id) {
        MonsterType monster = get(id);
        return monster == null ? FALLBACK_BASE_STRENGTH : monster.baseStrength();
    }

    /**
     * Base strength for a monster as it appears on a given level. A shared
     * monster is tougher deeper in the keep, before the dungeon menace scaling
     * from the player's own combat bonus is applied.
     */
    public static int strengthForLevel(String id, int level) {
        Map<String, Integer> strengths = LEVEL_STRENGTHS.get(level);
        if (strengths != null) {
            Integer strength = strengths.get(id);
            if (strength != null) {
                return strength;
            }
        }
        return baseStrengthOf(id);
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
        String hint = LOOT_HINTS.get(monsterId);
        return hint != null ? hint : get(monsterId) == null ? "Unknown" : "Gear";
    }

    public static String loreOf(String monsterId) {
        MonsterType monster = get(monsterId);
        return monster == null ? null : monster.lore();
    }

    public static boolean isQuestGuardian(String monsterId) {
        return monsterId != null && QUEST_DROPS.containsKey(monsterId);
    }

    public static boolean isVenomous(String monsterId) {
        return monsterId != null && VENOMOUS_IDS.contains(monsterId);
    }

    public static int poisonDamageOf(String monsterId) {
        return isVenomous(monsterId) ? VENOM_DAMAGE : 0;
    }
}
