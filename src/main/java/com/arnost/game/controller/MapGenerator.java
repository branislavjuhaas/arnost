package com.arnost.game.controller;

import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import java.util.*;

public class MapGenerator {
    public static int calculateDistance(int x, int y) { return Math.abs(x - 3) + Math.abs(y - 6); }

    public static DungeonMap generateSeededMap(long seed) {
        Random rng = new Random(seed);
        DungeonMap map = new DungeonMap();
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (x == 3 && y == 6) continue;
                if (rng.nextDouble() < 0.55) map.getTile(x, y).setType("ROOM");
            }
        }
        ensureGraphSolvability(map);
        return map;
    }

    public static void ensureGraphSolvability(DungeonMap map) {
        boolean[][] visited = new boolean[DungeonMap.SIZE][DungeonMap.SIZE];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{3, 6});
        visited[6][3] = true;
        int[] dx = {0, 0, 1, -1}, dy = {1, -1, 0, 0};
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = curr[0] + dx[i], ny = curr[1] + dy[i];
                if (nx >= 0 && nx < DungeonMap.SIZE && ny >= 0 && ny < DungeonMap.SIZE && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }
}