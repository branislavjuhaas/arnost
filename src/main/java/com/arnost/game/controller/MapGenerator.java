package com.arnost.game.controller;

import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.model.item.ItemType;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.monster.MonsterCatalog;
import com.arnost.game.model.monster.MonsterType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public final class MapGenerator {
    private static final double ROOM_CHANCE = 0.55;
    private static final double MONSTER_CHANCE = 0.30;
    private static final double WALL_CHANCE = 0.14;
    private static final int MIN_WALL_TILES = 6;
    private static final int ITEM_TILE_COUNT = 6;
    private static final int TRAP_TILE_COUNT = 2;

    private static final int START_X = 3;
    private static final int START_Y = 6;

    private static final int[][] LEVEL_ONE_POTIONS = {{2, 5}, {4, 4}};
    private static final int[][] LEVEL_TWO_POTIONS = {{2, 5}, {4, 4}};
    private static final int[][] LEVEL_THREE_POTIONS = {{2, 5}, {4, 4}};

    private MapGenerator() {
    }

    public static int calculateDistance(int x, int y) {
        return Math.abs(x - 3) + Math.abs(y - 6);
    }

    public static DungeonMap generateSeededMap(long seed) {
        return generateSeededMap(seed, 1);
    }

    public static DungeonMap generateSeededMap(long seed, int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Level must be between 1 and 3.");
        }

        Random rng = new Random(seed + level * 1009L);
        Random hiddenRng = new Random(seed + level * 1009L + 0x5EEDL);
        DungeonMap map = new DungeonMap();
        map.setGenerationMetadata(seed, level);
        map.clearDragonLairPosition();

        int exitX = -1;
        int exitY = -1;
        if (level == 1) {
            exitX = 6;
            exitY = 0;
        } else if (level == 2) {
            exitX = 0;
            exitY = 0;
        }

        if (exitX >= 0) {
            map.setExitPosition(exitX, exitY);
            Tile exit = map.getTile(exitX, exitY);
            exit.setType("EXIT");
            exit.clearMonster();
            exit.clearGroundFeatures();
        }

        if (level == 3) {
            map.setDragonLairPosition(3, 1);
            Tile lair = map.getTile(3, 1);
            lair.setType("DRAGON_LAIR");
            lair.clearMonster();
            lair.clearGroundFeatures();
        }

        List<int[]> requiredPath = requiredPath(level);
        boolean[][] protectedTiles = protectedTiles(level, requiredPath);

        generateTerrainAndMonsters(map, rng, hiddenRng, level, protectedTiles);
        carveRequiredPath(map, requiredPath);

        placeFixedPotions(map, level);
        placeGuaranteedMonsters(map, level, hiddenRng);

        ensureMinimumWalls(map, rng, protectedTiles);
        ensureGraphSolvability(map);

        List<int[]> reachable = reachableTiles(map);
        placeItems(map, rng, reachable);
        placeTraps(map, rng, reachable, protectedTiles);

        ensureGraphSolvability(map);
        return map;
    }

    public static DungeonMap generateSeededMap(long seed, int level, int combatBonus) {
        DungeonMap map = generateSeededMap(seed, level);
        if (level == 3 && combatBonus >= 4) {
            activateDragonLair(map);
        }
        return map;
    }

    public static void activateDragonLair(DungeonMap map) {
        if (map == null || !map.hasDragonLairPosition()) {
            return;
        }
        Tile lair = map.getDragonLairTile();
        if (lair == null || lair.isCleared()) {
            return;
        }
        lair.setType("DRAGON_LAIR");
        lair.setMonsterId("dragon");
        lair.setMonsterStrength(MonsterCatalog.strengthForLevel("dragon", 3));
        lair.setMonsterLives(MonsterCatalog.DRAGON_LIVES);
        lair.setMonsterStrengthHidden(false);
        lair.setItemId(null);
        lair.setHasPotion(false);
        lair.setHasTrap(false);
    }

    private static List<int[]> requiredPath(int level) {
        List<int[]> path = new ArrayList<>();
        for (int y = START_Y; y >= 0; y--) {
            path.add(new int[]{3, y});
        }

        if (level == 1) {
            for (int x = 4; x <= 6; x++) {
                path.add(new int[]{x, 0});
            }
        } else if (level == 2) {
            for (int x = 2; x >= 0; x--) {
                path.add(new int[]{x, 0});
            }
        } else {
            // The lair is at [3,1]; the northern part of the corridor is not required.
            path.removeIf(position -> position[1] == 0);
        }
        return path;
    }

    private static boolean[][] protectedTiles(int level, List<int[]> requiredPath) {
        boolean[][] protectedGrid = new boolean[DungeonMap.SIZE][DungeonMap.SIZE];
        protectedGrid[START_Y][START_X] = true;

        for (int[] position : requiredPath) {
            protectedGrid[position[1]][position[0]] = true;
        }

        if (level == 1) {
            protectedGrid[0][6] = true;
        } else if (level == 2) {
            protectedGrid[0][0] = true;
        } else {
            protectedGrid[1][3] = true;
        }

        for (MonsterCatalog.FixedPlacement placement
                : MonsterCatalog.fixedPlacementsForLevel(level)) {
            protectedGrid[placement.y()][placement.x()] = true;
        }

        if (level == 2) {
            for (int[] position : LEVEL_TWO_POTIONS) {
                protectedGrid[position[1]][position[0]] = true;
            }
        } else if (level == 1) {
            for (int[] position : LEVEL_ONE_POTIONS) {
                protectedGrid[position[1]][position[0]] = true;
            }
        } else {
            for (int[] position : LEVEL_THREE_POTIONS) {
                protectedGrid[position[1]][position[0]] = true;
            }
        }
        return protectedGrid;
    }

    private static void generateTerrainAndMonsters(DungeonMap map, Random rng, Random hiddenRng,
                                                   int level, boolean[][] protectedTiles) {
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                Tile tile = map.getTile(x, y);
                if (tile.isStart() || tile.isExit() || tile.isDragonLair()) {
                    continue;
                }

                if (protectedTiles[y][x]) {
                    tile.setType("ROOM");
                    tile.clearMonster();
                    tile.clearGroundFeatures();
                    continue;
                }

                if (rng.nextDouble() < WALL_CHANCE) {
                    turnIntoWall(tile);
                    continue;
                }

                if (rng.nextDouble() < ROOM_CHANCE) {
                    tile.setType("ROOM");
                    if (rng.nextDouble() < MONSTER_CHANCE) {
                        MonsterType monster = MonsterCatalog.randomSpawn(rng, level);
                        tile.setMonsterId(monster.id());
                        tile.setMonsterStrength(MonsterCatalog.strengthForLevel(monster.id(), level));
                        tile.setMonsterStrengthHidden(hiddenRng.nextBoolean());
                    }
                }
            }
        }
    }

    private static void carveRequiredPath(DungeonMap map, List<int[]> requiredPath) {
        for (int[] position : requiredPath) {
            Tile tile = map.getTile(position[0], position[1]);
            if (tile == null || tile.isStart() || tile.isExit() || tile.isDragonLair()) {
                continue;
            }
            tile.setType("ROOM");
            tile.clearMonster();
            tile.clearGroundFeatures();
        }
    }

    private static void placeGuaranteedMonsters(DungeonMap map, int level, Random rng) {
        for (MonsterCatalog.FixedPlacement placement
                : MonsterCatalog.fixedPlacementsForLevel(level)) {
            placeMonster(map, level, placement.x(), placement.y(),
                placement.monsterId(), rng);
        }
    }

    private static void placeFixedPotions(DungeonMap map, int level) {
        int[][] potions = switch (level) {
            case 1 -> LEVEL_ONE_POTIONS;
            case 2 -> LEVEL_TWO_POTIONS;
            case 3 -> LEVEL_THREE_POTIONS;
            default -> new int[0][];
        };
        for (int[] position : potions) {
            Tile tile = map.getTile(position[0], position[1]);
            tile.setType("ROOM");
            tile.clearMonster();
            tile.clearGroundFeatures();
            tile.setHasPotion(true);
        }
    }

    private static void ensureMinimumWalls(DungeonMap map, Random rng, boolean[][] protectedTiles) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                Tile tile = map.getTile(x, y);
                if (protectedTiles[y][x] || tile.isStart() || tile.isExit() || tile.isDragonLair()) {
                    continue;
                }
                if (tile.hasPotion() || tile.hasTrap() || tile.getMonsterId() != null
                    || tile.getItemId() != null) {
                    continue;
                }
                if (!isWall(tile)) {
                    candidates.add(new int[]{x, y});
                }
            }
        }

        Collections.shuffle(candidates, rng);
        int walls = countWalls(map);
        for (int i = 0; i < candidates.size() && walls < MIN_WALL_TILES; i++) {
            int[] position = candidates.get(i);
            turnIntoWall(map.getTile(position[0], position[1]));
            walls++;
        }
    }

    private static void placeItems(DungeonMap map, Random rng, List<int[]> reachable) {
        List<int[]> candidates = new ArrayList<>();
        for (int[] position : reachable) {
            int x = position[0];
            int y = position[1];
            Tile tile = map.getTile(x, y);
            if (tile.isStart() || tile.isExit() || tile.isDragonLair() || isWall(tile)) {
                continue;
            }
            if (tile.getMonsterId() != null || tile.getItemId() != null
                || tile.hasPotion() || tile.hasTrap()) {
                continue;
            }
            candidates.add(position);
        }

        Collections.shuffle(candidates, rng);
        int itemCount = Math.min(ITEM_TILE_COUNT, candidates.size());
        for (int i = 0; i < itemCount; i++) {
            int[] position = candidates.get(i);
            ItemType item = ItemCatalog.randomLoot(rng);
            Tile tile = map.getTile(position[0], position[1]);
            tile.setItemId(item.id());
            tile.setCleared(false);
        }
    }

    private static void placeTraps(DungeonMap map, Random rng, List<int[]> reachable,
                                   boolean[][] protectedTiles) {
        List<int[]> preferred = new ArrayList<>();
        List<int[]> fallback = new ArrayList<>();
        for (int[] position : reachable) {
            int x = position[0];
            int y = position[1];
            Tile tile = map.getTile(x, y);
            if (!isTrapCandidate(tile)) {
                continue;
            }
            fallback.add(position);
            if (!protectedTiles[y][x]) {
                preferred.add(position);
            }
        }

        Collections.shuffle(preferred, rng);
        Collections.shuffle(fallback, rng);

        int placed = 0;
        for (int[] position : preferred) {
            if (placed >= TRAP_TILE_COUNT) {
                break;
            }
            armTrap(map.getTile(position[0], position[1]));
            placed++;
        }
        for (int[] position : fallback) {
            if (placed >= TRAP_TILE_COUNT) {
                break;
            }
            Tile tile = map.getTile(position[0], position[1]);
            if (tile.hasTrap()) {
                continue;
            }
            armTrap(tile);
            placed++;
        }
    }

    private static boolean isTrapCandidate(Tile tile) {
        return tile != null
            && !tile.isStart()
            && !tile.isExit()
            && !tile.isDragonLair()
            && !isWall(tile)
            && tile.getMonsterId() == null
            && tile.getItemId() == null
            && !tile.hasPotion()
            && !tile.hasTrap();
    }

    private static void armTrap(Tile tile) {
        tile.setHasTrap(true);
        tile.setTrapTriggered(false);
    }

    private static void placeMonster(DungeonMap map, int level, int x, int y,
                                     String monsterId, Random rng) {
        Tile tile = map.getTile(x, y);
        tile.setType("ROOM");
        tile.setMonsterId(monsterId);
        tile.setMonsterStrength(MonsterCatalog.strengthForLevel(monsterId, level));
        tile.setMonsterStrengthHidden(rng.nextBoolean());
        tile.clearGroundFeatures();
        tile.setCleared(false);
    }

    private static void turnIntoWall(Tile tile) {
        tile.setType("WALL");
        tile.clearMonster();
        tile.clearGroundFeatures();
    }

    private static boolean isWall(Tile tile) {
        return tile != null && tile.isWall();
    }

    private static int countWalls(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (isWall(map.getTile(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static List<int[]> reachableTiles(DungeonMap map) {
        boolean[][] visited = new boolean[DungeonMap.SIZE][DungeonMap.SIZE];
        Queue<int[]> queue = new LinkedList<>();
        List<int[]> reachable = new ArrayList<>();

        queue.add(new int[]{START_X, START_Y});
        visited[START_Y][START_X] = true;
        reachable.add(new int[]{START_X, START_Y});

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = current[0] + dx[i];
                int ny = current[1] + dy[i];
                if (nx < 0 || nx >= DungeonMap.SIZE || ny < 0 || ny >= DungeonMap.SIZE) {
                    continue;
                }
                if (visited[ny][nx] || isWall(map.getTile(nx, ny))) {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new int[]{nx, ny});
                reachable.add(new int[]{nx, ny});
            }
        }
        return reachable;
    }

    public static void ensureGraphSolvability(DungeonMap map) {
        if (map == null) {
            return;
        }

        List<int[]> reachable = reachableTiles(map);
        if (map.hasExitPosition() && !contains(reachable, map.getExitX(), map.getExitY())) {
            carveStraightPath(map, START_X, START_Y, map.getExitX(), map.getExitY());
        }
        if (map.hasDragonLairPosition()
            && !contains(reachable, map.getDragonLairX(), map.getDragonLairY())) {
            carveStraightPath(map, START_X, START_Y, map.getDragonLairX(), map.getDragonLairY());
        }
    }

    private static boolean contains(List<int[]> positions, int x, int y) {
        for (int[] position : positions) {
            if (position[0] == x && position[1] == y) {
                return true;
            }
        }
        return false;
    }

    private static void carveStraightPath(DungeonMap map, int fromX, int fromY,
                                          int toX, int toY) {
        int x = fromX;
        int y = fromY;
        while (x != toX || y != toY) {
            if (x != toX) {
                x += Integer.compare(toX, x);
            } else {
                y += Integer.compare(toY, y);
            }
            Tile tile = map.getTile(x, y);
            if (tile == null || tile.isExit() || tile.isDragonLair()) {
                continue;
            }
            tile.setType("ROOM");
            tile.clearMonster();
            tile.clearGroundFeatures();
            tile.setCleared(false);
        }
    }
}
