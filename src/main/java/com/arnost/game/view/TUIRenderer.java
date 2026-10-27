package com.arnost.game.view;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.model.item.ItemType;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.monster.MonsterCatalog;
import com.arnost.game.model.monster.MonsterType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class TUIRenderer {
    private static final int MAP_CELL_WIDTH = 3;
    private static final int MAP_WIDTH = DungeonMap.SIZE * MAP_CELL_WIDTH + (DungeonMap.SIZE - 1);
    private static final int PANEL_WIDTH = 46;
    private static final int PANEL_CONTENT_WIDTH = 42;
    private static final int PANEL_CONTENT_ROWS = 8;
    private static final int PANEL_HEIGHT = 10;
    private static final int SCREEN_WIDTH = 80;
    private static final int STORY_WIDTH = 76;
    private static final String SCREEN_RULE = "=".repeat(SCREEN_WIDTH);
    private static final String STATUS_RULE = "-".repeat(SCREEN_WIDTH);
    private static final String PANE_SEPARATOR = "   |   ";

    private static final String HEADER_BG = "\u001B[44;1m";
    private static final String ENCOUNTER_BG = "\u001B[41;1m";
    private static final String INVENTORY_BG = "\u001B[42;1m";
    private static final String ROLLING_BG = "\u001B[43;1m";
    private static final String STATUS_BG = "\u001B[100m";
    private static final String BLACK_TEXT = "\u001B[30m";

    private static final Map<String, String> MONSTER_LORE = Map.ofEntries(
        Map.entry("venom_spider", "Chitinous fangs drip with green bile."),
        Map.entry("mummy_lord", "Guards the Dungeon Key to Level 3."),
        Map.entry("plague_mummy", "Rotting bandages leak deadly poison."),
        Map.entry("guardian", "An animated suit of zweihänder-wielding steel."),
        Map.entry("wraith", "A screaming phantom clad in frost and shadow."),
        Map.entry("knight", "Slay him to unearth the holy blade Excalibur!"),
        Map.entry("dragon", "The ancient wyrm snarls! Excalibur burns brightly.")
    );

    private static final long ROLL_SETTLE_HOLD_MS = 1800;
    private static final long ROLL_RESULT_HOLD_MS = 2600;

    private final PrintStream out;
    private final boolean enableClearScreen;

    public TUIRenderer(PrintStream out, boolean enableClearScreen) {
        this.out = out;
        this.enableClearScreen = enableClearScreen;
    }

    public enum RollOutcomeStyle {
        WIN(ANSIColor.EMERALD, INVENTORY_BG, ANSIColor.BRIGHT_WHITE,
            "VICTORY", "The monster falls."),
        DRAW(ANSIColor.GOLD, ROLLING_BG, BLACK_TEXT,
            "DRAW", "Neither side yields."),
        LOSS(ANSIColor.CRIMSON, ENCOUNTER_BG, ANSIColor.BRIGHT_WHITE,
            "DEFEAT", "You are pushed back.");

        private final String color;
        private final String background;
        private final String foreground;
        private final String label;
        private final String tagline;

        RollOutcomeStyle(String color, String background, String foreground,
                         String label, String tagline) {
            this.color = color;
            this.background = background;
            this.foreground = foreground;
            this.label = label;
            this.tagline = tagline;
        }
    }

    public void renderTitleScreen() {
        renderTitleScreen(false);
    }

    public void renderTitleScreen(boolean hasSaveFile) {
        clearScreen();

        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("ARNOST - LEGEND OF THE FALLEN KEEP", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("THREE LEVELS. ONE DRAGON. NO MERCY.", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println();

        String[] art = {
            "    /\\                     /\\",
            "   /  \\      _______      /  \\",
            "  /____\\____/       \\____/____\\",
            "  |    |    |  ___  |    |    |",
            "  | [] | [] |  | |  | [] | [] |",
            "  |____|____|__|_|__|____|____|"
        };
        String[] colors = {
            ANSIColor.SKY_BLUE, ANSIColor.SKY_BLUE,
            ANSIColor.BONE, ANSIColor.BONE, ANSIColor.BONE, ANSIColor.BONE
        };
        printArt(art, colors);

        out.println();
        out.println(ANSIColor.GOLD + centerPlain("THE FALLEN KEEP AWAITS", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.BONE +
            centerPlain("A 2d6 dungeon crawl through three levels", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY +
            centerPlain("[S] Start [I] Item [E] Stairs [D] Dragon [*] You", SCREEN_WIDTH) +
            ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY +
            centerPlain("[M] Monster [V] Venom [T] Trap [P] Potion [#] Wall [?] Unknown [.] Empty",
                SCREEN_WIDTH) +
            ANSIColor.RESET);
        out.println();
        renderCredits();
        out.println();
        out.println(ANSIColor.SHADOW_GRAY + "  Press [ENTER] to start a new game." + ANSIColor.RESET);
        if (hasSaveFile) {
            out.println(ANSIColor.EMERALD + "  Press [L] to load the saved keep." + ANSIColor.RESET);
        }
        out.println(ANSIColor.SHADOW_GRAY +
            "  Commands: n/s/e/w, go n/s/e/w, roll, drink, descend, help, quit" +
            ANSIColor.RESET);
        out.print(ANSIColor.SHADOW_GRAY + "> " + ANSIColor.RESET);
        out.flush();
    }

    public void renderStoryScreen(String chapter, String title, String prompt,
                                  String... paragraphs) {
        clearScreen();

        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("ARNOST - LEGEND OF THE FALLEN KEEP", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain(chapter, SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println();

        out.println(ANSIColor.GOLD + centerPlain(title, SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + centerPlain("* * *", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println();

        for (String paragraph : paragraphs) {
            for (String line : wrapPlain(paragraph, STORY_WIDTH)) {
                out.println(ANSIColor.BRIGHT_WHITE + "  " + line + ANSIColor.RESET);
            }
            out.println();
        }

        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + centerPlain(prompt, SCREEN_WIDTH) + ANSIColor.RESET);
        out.flush();
    }

    private void renderCredits() {
        String credits = ANSIColor.CRIMSON + "Branislav Juhas" + ANSIColor.RESET +
            ANSIColor.SHADOW_GRAY + "  ·  " + ANSIColor.RESET +
            ANSIColor.GOLD + "Simon Stefanik" + ANSIColor.RESET +
            ANSIColor.SHADOW_GRAY + "  ·  " + ANSIColor.RESET +
            ANSIColor.SKY_BLUE + "Adam Mistrik" + ANSIColor.RESET +
            ANSIColor.SHADOW_GRAY + "  ·  " + ANSIColor.RESET +
            ANSIColor.PURPLE + "Tetiana Tkachenko" + ANSIColor.RESET;

        out.println(ANSIColor.SHADOW_GRAY +
            centerPlain("CREATED BY", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(centerVisible(credits, SCREEN_WIDTH));
    }

    public void renderHelp() {
        clearScreen();

        out.println(ANSIColor.GOLD + SCREEN_RULE + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + centerPlain("HOW TO PLAY", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + SCREEN_RULE + ANSIColor.RESET);

        out.println(ANSIColor.CYAN + "  THE QUEST" + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + """
          Explore three levels of the Fallen Keep. Hidden guardians carry the
          Dungeon Key on Levels 1 and 2. On Level 3, a hidden champion guards
          Excalibur. Claim it, then slay the Dragon to win the game.
        """ + ANSIColor.RESET);

        out.println(ANSIColor.CYAN + "  COMMANDS" + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + """
          n / s / e / w                - move one tile (same as 'go north' etc.)
          go n / go s / go e / go w    - move one tile
          gon / gos / goe / gow / gol  - compact movement
          left / right / up / down     - alternate directions
          roll                         - fight the monster on your current tile
          drink / take potion          - consume a Health Potion on your tile
          descend / go stairs          - take an Exit Staircase [E]
          save                         - write the current run to the savefile
          help                         - show this screen
          quit                         - leave the keep
        """ + ANSIColor.RESET);

        out.println(ANSIColor.CYAN + "  COMBAT" + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + """
          Your attack is 2d6 plus the combat bonus from your gear.
            Attack > monster strength  -> VICTORY! Stronger monsters drop better
                                          loot. Weak monsters may drop nothing.
            Attack = monster strength  -> DRAW! You are pushed back, but lose no HP.
            Attack < monster strength  -> DEFEAT! You lose 1 HP and are pushed back.

          The keep answers your growing power: every +3 combat bonus adds +1 STR
          to the monsters you meet. About half of them hide their strength as
          "???" until you start rolling. Venomous monsters [V] deal 1 extra HP
          when they defeat you.
        """ + ANSIColor.RESET);

        out.println(ANSIColor.EMERALD + "  GEAR: Dagger +1 | Short Sword +1 | Leather Armor +1 | Silver Ring +1" + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "        Saber +2 | Halberd +3 | War Axe +3 | Runeblade +4 | Fireball +1" + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "        Loot [I] tiles and defeat monsters. Stronger enemies carry better gear." + ANSIColor.RESET);
        out.println(ANSIColor.CRIMSON + "  DRAGON'S LAIR: Level 3 at [3,1]. It awakens at +4 combat bonus;" + ANSIColor.RESET);
        out.println(ANSIColor.CRIMSON + "                 only Excalibur can slay it." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "  KEYS: Hidden guardians hold the Level 1 and Level 2 Dungeon Keys." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "        Defeat them to descend." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "  TOP BAR: Tracks the quest item for the current level." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "           Levels 1-2: KEY: [MISSING] -> [ACQUIRED]" + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "           Level 3: EXCALIBUR: [LOCKED] -> [DRAWN]" + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "  POTIONS: You start at 3/5 HP. [P] potions stay on the floor." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "           Walk onto one, then type 'drink' to restore 2 HP." + ANSIColor.RESET);
        out.println(ANSIColor.CRIMSON + "  TRAPS: [T] traps stay hidden until you explore nearby. Step on one:" + ANSIColor.RESET);
        out.println(ANSIColor.CRIMSON + "         lose 1 HP." + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "  ENEMIES: Active monsters stay on explored tiles as [M]; venomous" + ANSIColor.RESET);
        out.println(ANSIColor.GOLD + "           ones show [V]. Their strength may remain concealed." + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + "  MAP: [*] you  [S] start  [I] item  [P] potion  [E] stairs  [D] dragon" + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + "       [M] monster  [V] venom  [T] trap  [#] wall  [?] unexplored  [.] empty" + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + "       Your tile and the four chambers beside it are revealed as you move." + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + "  FALLEN HERO: Press 'r' on the defeat screen to restart with a new keep." + ANSIColor.RESET);

        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println(ANSIColor.SHADOW_GRAY + "  Press [ENTER] to return to the keep..." + ANSIColor.RESET);
        out.flush();
    }

    public void render(Player player, DungeonMap map, String statusMsg) {
        render(player, map, map.getGenerationLevel(), statusMsg);
    }

    public void render(Player player, DungeonMap map, int level, String statusMsg) {
        renderScreen(player, map, level, statusMsg, ANSIColor.BRIGHT_WHITE,
            buildSidePanel(player, map), true);
    }

    public void renderVictoryScreen(Player player, String message) {
        clearScreen();

        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("VICTORY - ASHGORATH IS SLAIN", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("EPILOGUE - THE FALLEN KEEP IS FREE", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println();

        String[] art = {
            "                 /\\                              .-.",
            "                /  \\                            /   \\",
            "               /    \\                          /_____\\",
            "              /  /\\  \\                         \\  o  /",
            "             /  /  \\  \\                         \\___/",
            "            /  /____\\  \\                       __|_|__",
            "           /__/      \\__\\                     /_______\\",
            "               ||||                            | | | |",
            "               ||||                            | | | |",
            "           ____||||____                      __|_|_|_|__",
            "          |  EXCALIBUR |                    /  CROWN     \\",
            "          |_____________|                  /   JEWELS     \\",
            "                                           |_______________|"
        };
        String[] colors = new String[art.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = ANSIColor.GOLD;
        }
        printArt(art, colors);

        out.println();
        out.println(ANSIColor.GOLD + centerPlain("EXCALIBUR AND THE CROWN JEWELS", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + "  Final strength bonus: +" +
                    player.getInventory().getCombatBonus() +
                    "   |   Items carried: " + player.getInventory().size() + ANSIColor.RESET);
        for (String line : wrapPlain(message, STORY_WIDTH)) {
            out.println(ANSIColor.BRIGHT_WHITE + "  " + line + ANSIColor.RESET);
        }
        out.println();
        out.println(ANSIColor.EMERALD + centerPlain("The legend of the Fallen Keep is complete.", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println();
        renderCredits();
        out.flush();
    }

    public void renderDefeatScreen(Player player, String message) {
        clearScreen();

        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("DEFEAT - THE FALLEN KEEP CLAIMS ANOTHER HERO", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(HEADER_BG + ANSIColor.BRIGHT_WHITE +
            centerPlain("ANOTHER HERO HAS FALLEN", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println();

        String[] art = {
            "                    .-.",
            "                   ( x )",
            "                    `-'",
            "           _____________________",
            "          |                     |",
            "          |        R.I.P.       |",
            "          |                     |",
            "          |_____________________|",
            "            /|/   /|/   /|/   /|/"
        };
        String[] colors = {
            ANSIColor.CRIMSON, ANSIColor.CRIMSON, ANSIColor.CRIMSON,
            ANSIColor.BONE, ANSIColor.BONE, ANSIColor.BONE,
            ANSIColor.BONE, ANSIColor.BONE, ANSIColor.SHADOW_GRAY
        };
        printArt(art, colors);

        out.println();
        out.println(ANSIColor.CRIMSON + centerPlain("YOU HAVE FALLEN", SCREEN_WIDTH) + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "  Final combat bonus: +" +
                    player.getInventory().getCombatBonus() +
                    "   |   Items carried: " + player.getInventory().size() + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + "  " + message + ANSIColor.RESET);
        out.println();
        out.println(ANSIColor.SKY_BLUE + "  [R] Restart the Keep     [Q] Quit the game" + ANSIColor.RESET);
        out.print(ANSIColor.SHADOW_GRAY + "> " + ANSIColor.RESET);
        out.flush();
    }

    public void renderRollEffect(Player player, DungeonMap map, int first, int second,
                                 RollOutcomeStyle style) {
        renderRollEffect(player, map, map.getGenerationLevel(), first, second, style);
    }

    public void renderRollEffect(Player player, DungeonMap map, int level, int first, int second,
                                 RollOutcomeStyle style) {
        if (!enableClearScreen) {
            return;
        }

        Random random = new Random();
        for (int frame = 0; frame < 12; frame++) {
            renderScreen(player, map, level, "The bones tumble...", ANSIColor.BRIGHT_WHITE,
                buildRollPanel(random.nextInt(6) + 1, random.nextInt(6) + 1, null, false), false);
            pause(frame < 8 ? 110 : 180);
        }

        renderScreen(player, map, level, "The fates hold their breath...", ANSIColor.SKY_BLUE,
            buildRollPanel(first, second, null, true), false);
        pause(ROLL_SETTLE_HOLD_MS);

        renderScreen(player, map, level, style.tagline, style.color,
            buildRollPanel(first, second, style, true), false);
        pause(ROLL_RESULT_HOLD_MS);
    }

    private void renderScreen(Player player, DungeonMap map, int level, String statusMsg,
                              String statusColor, SidePanel panel, boolean showPrompt) {
        clearScreen();
        renderHeader(player, level);
        out.println();

        List<String> mapLines = buildMapLines(player, map);
        List<String> panelLines = normalizePanel(panel.lines());
        out.println(mapLines.get(0) + PANE_SEPARATOR + topBorder(panel));
        for (int row = 1; row < PANEL_HEIGHT; row++) {
            String mapLine = row < mapLines.size() ? mapLines.get(row) : " ".repeat(MAP_WIDTH);
            String paneLine = row <= PANEL_CONTENT_ROWS
                ? framePanelLine(panelLines.get(row - 1))
                : bottomBorder();
            out.println(mapLine + PANE_SEPARATOR + paneLine);
        }

        out.println();
        renderStatusBar(statusMsg, statusColor);
        if (showPrompt) {
            out.println(ANSIColor.SHADOW_GRAY + "Type 'help' for instructions." + ANSIColor.RESET);
            out.print(ANSIColor.CYAN + "> " + ANSIColor.RESET);
        }
        out.flush();
    }

    private void renderHeader(Player player, int level) {
        boolean hasKey = player.getInventory().contains("key");
        boolean hasExcalibur = player.getInventory().contains("excalibur");
        String stats = " ARNOST :: LEVEL " + level + "/3 :: HP [" +
            player.getHp() + "/" + player.getMaxHp() + "] :: COMBAT +" +
            player.getInventory().getCombatBonus() + " :: ";
        String questIndicator = level == 3
            ? (hasExcalibur ? ANSIColor.EMERALD : ANSIColor.SHADOW_GRAY) +
                "EXCALIBUR: " + (hasExcalibur ? "[DRAWN]" : "[LOCKED]")
            : (hasKey ? ANSIColor.GOLD : ANSIColor.SHADOW_GRAY) +
                "KEY: " + (hasKey ? "[ACQUIRED]" : "[MISSING]");

        String line = HEADER_BG + ANSIColor.BRIGHT_WHITE + stats + questIndicator +
            HEADER_BG + ANSIColor.BRIGHT_WHITE;
        int padding = Math.max(0, SCREEN_WIDTH - visibleLength(line));

        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
        out.println(line + " ".repeat(padding) + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + SCREEN_RULE + ANSIColor.RESET);
    }

    private void renderStatusBar(String statusMsg, String statusColor) {
        List<String> lines = wrapStatus(statusMsg == null ? "" : statusMsg);
        out.println(STATUS_RULE);
        for (String line : lines) {
            out.println(STATUS_BG + statusColor + padVisible(line, SCREEN_WIDTH) + ANSIColor.RESET);
        }
        out.println(STATUS_RULE);
    }

    private static List<String> wrapStatus(String message) {
        String firstPrefix = " STATUS : ";
        String continuationPrefix = "          ";
        List<String> lines = new ArrayList<>();
        String prefix = firstPrefix;

        for (String paragraph : message.replace('\r', ' ').split("\\n", -1)) {
            String remaining = paragraph.trim();
            if (remaining.isEmpty()) {
                lines.add("");
                prefix = continuationPrefix;
                continue;
            }

            while (true) {
                int capacity = SCREEN_WIDTH - prefix.length();
                if (remaining.length() <= capacity) {
                    lines.add(prefix + remaining);
                    prefix = continuationPrefix;
                    break;
                }

                int breakAt = remaining.lastIndexOf(' ', capacity);
                if (breakAt <= 0) {
                    breakAt = capacity;
                }
                lines.add(prefix + remaining.substring(0, breakAt));
                remaining = remaining.substring(breakAt).trim();
                prefix = continuationPrefix;
            }
        }
        return lines;
    }

    private List<String> buildMapLines(Player player, DungeonMap map) {
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (x > 0) {
                    sb.append(' ');
                }
                Tile tile = map.getTile(x, y);
                boolean current = x == player.getX() && y == player.getY();
                boolean dragonSite = map.hasDragonLairPosition()
                    && x == map.getDragonLairX() && y == map.getDragonLairY();
                sb.append(markerFor(tile, current, dragonSite));
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private String markerFor(Tile tile, boolean current, boolean dragonSite) {
        if (current) {
            String color = dragonSite ? ANSIColor.CRIMSON : currentTileColor(tile);
            return color + "[*]" + ANSIColor.RESET;
        }
        if (dragonSite) {
            return ANSIColor.CRIMSON + "[D]" + ANSIColor.RESET;
        }
        if (tile.isExit()) {
            return ANSIColor.SKY_BLUE + "[E]" + ANSIColor.RESET;
        }
        if (tile.isWall()) {
            return ANSIColor.SHADOW_GRAY + "[#]" + ANSIColor.RESET;
        }
        if (!tile.isExplored()) {
            return ANSIColor.SKY_BLUE + "[?]" + ANSIColor.RESET;
        }
        if (tile.isTrapArmed()) {
            return ANSIColor.CRIMSON + "[T]" + ANSIColor.RESET;
        }
        if (tile.isStart()) {
            return ANSIColor.EMERALD + "[S]" + ANSIColor.RESET;
        }
        if (tile.isDragonLair()) {
            return ANSIColor.CRIMSON + "[D]" + ANSIColor.RESET;
        }
        if (tile.getMonsterId() != null && !tile.isCleared()) {
            MonsterType monster = MonsterCatalog.get(tile.getMonsterId());
            String color = monster == null ? ANSIColor.CRIMSON : monster.color();
            String marker = MonsterCatalog.isVenomous(tile.getMonsterId()) ? "[V]" : "[M]";
            return color + marker + ANSIColor.RESET;
        }
        if (tile.getItemId() != null && !tile.isCleared()) {
            ItemType item = ItemCatalog.get(tile.getItemId());
            String color = item == null ? ANSIColor.GOLD : item.color();
            return color + "[I]" + ANSIColor.RESET;
        }
        if (tile.hasPotion()) {
            return ANSIColor.GOLD + "[P]" + ANSIColor.RESET;
        }
        return ANSIColor.SHADOW_GRAY + "[.]" + ANSIColor.RESET;
    }

    private String currentTileColor(Tile tile) {
        if (tile == null) {
            return ANSIColor.CYAN;
        }
        if (tile.isExit()) {
            return ANSIColor.SKY_BLUE;
        }
        if (tile.hasPotion()) {
            return ANSIColor.GOLD;
        }
        if (tile.isTrapArmed()) {
            return ANSIColor.CRIMSON;
        }
        if (tile.isStart()) {
            return ANSIColor.EMERALD;
        }
        if (tile.isDragonLair()) {
            return ANSIColor.CRIMSON;
        }
        if (tile.isWall()) {
            return ANSIColor.SHADOW_GRAY;
        }
        if (tile.getMonsterId() != null && !tile.isCleared()) {
            MonsterType monster = MonsterCatalog.get(tile.getMonsterId());
            return monster == null ? ANSIColor.CRIMSON : monster.color();
        }
        if (tile.getItemId() != null && !tile.isCleared()) {
            ItemType item = ItemCatalog.get(tile.getItemId());
            return item == null ? ANSIColor.GOLD : item.color();
        }
        return ANSIColor.CYAN;
    }

    private SidePanel buildSidePanel(Player player, DungeonMap map) {
        Tile currentTile = map.getTile(player.getX(), player.getY());
        if (currentTile == null) {
            return buildLocationPanel(player, null);
        }

        if (currentTile.getMonsterId() != null && !currentTile.isCleared()) {
            MonsterType monster = MonsterCatalog.get(currentTile.getMonsterId());
            if (monster != null) {
                int baseStrength = currentTile.getMonsterStrength() > 0
                    ? currentTile.getMonsterStrength()
                    : monster.baseStrength();
                int strength = MonsterCatalog.effectiveStrength(
                    baseStrength, player.getInventory().getCombatBonus());
                return buildMonsterPanel(player, monster, strength,
                    currentTile.isMonsterStrengthHidden());
            }
        }

        if (currentTile.hasPotion()) {
            return buildPotionPanel();
        }

        if (currentTile.getItemId() != null && !currentTile.isCleared()) {
            ItemType item = ItemCatalog.get(currentTile.getItemId());
            if (item != null) {
                return buildItemPanel(player, item);
            }
        }

        if (currentTile.isTrapArmed()) {
            return buildTrapPanel();
        }

        if (currentTile.isExit()) {
            return buildExitPanel(player);
        }

        if (currentTile.isDragonLair()) {
            return buildDragonPanel(player);
        }

        return buildLocationPanel(player, currentTile);
    }

    private SidePanel buildMonsterPanel(Player player, MonsterType monster, int strength,
                                        boolean strengthHidden) {
        List<String> lines = new ArrayList<>();
        for (String artLine : monster.art()) {
            lines.add(centerVisible(monster.color() + artLine + ANSIColor.RESET, PANEL_CONTENT_WIDTH));
        }

        String strengthColor = strengthHidden ? ANSIColor.SHADOW_GRAY : ANSIColor.EMERALD;
        String strengthText = strengthHidden ? "???" : Integer.toString(strength);
        String venom = MonsterCatalog.isVenomous(monster.id())
            ? "  " + ANSIColor.LAVA + "VENOM" + ANSIColor.RESET
            : "";
        String nameAndStrength = ANSIColor.BRIGHT_WHITE + monster.name() + ANSIColor.RESET +
            "  " + strengthColor + "STR " + strengthText + ANSIColor.RESET + venom;
        lines.add(centerVisible(nameAndStrength, PANEL_CONTENT_WIDTH));

        String lore = MONSTER_LORE.get(monster.id());
        if (lore != null) {
            for (String loreLine : loreLines(lore)) {
                lines.add(centerVisible(ANSIColor.BONE + loreLine + ANSIColor.RESET,
                    PANEL_CONTENT_WIDTH));
            }
        }

        lines.add(centerVisible(ANSIColor.GOLD +
            "Drops: " + MonsterCatalog.lootHintOf(monster.id()) + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(strengthHidden
            ? " Type 'roll' to reveal and fight."
            : " Type 'roll' to fight.");

        return new SidePanel("ENCOUNTER: " + monster.name().toUpperCase(Locale.ROOT),
            ENCOUNTER_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private static List<String> loreLines(String lore) {
        String text = lore.trim();
        if (text.isEmpty()) {
            return List.of("");
        }

        if (text.length() <= PANEL_CONTENT_WIDTH * 2) {
            int midpoint = text.length() / 2;
            int splitAt = text.lastIndexOf(' ', midpoint);
            if (splitAt <= 0) {
                splitAt = text.indexOf(' ', midpoint);
            }
            if (splitAt > 0) {
                return List.of(
                    text.substring(0, splitAt).trim(),
                    text.substring(splitAt).trim()
                );
            }
            return List.of(text);
        }

        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > PANEL_CONTENT_WIDTH) {
            int breakAt = remaining.lastIndexOf(' ', PANEL_CONTENT_WIDTH);
            if (breakAt <= 0) {
                breakAt = PANEL_CONTENT_WIDTH;
            }
            lines.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    private SidePanel buildItemPanel(Player player, ItemType item) {
        List<String> lines = new ArrayList<>();
        String bonus = item.combatBonus() > 0
            ? "+" + item.combatBonus() + " STR"
            : "Trophy";
        lines.add(" " + item.name());
        for (String artLine : item.art()) {
            lines.add(centerVisible(item.color() + artLine + ANSIColor.RESET, PANEL_CONTENT_WIDTH));
        }
        lines.add(" Bonus    : " + bonus);
        lines.add(" Walk over it to collect.");

        return new SidePanel("GROUND LOOT", INVENTORY_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private SidePanel buildPotionPanel() {
        List<String> lines = new ArrayList<>();
        lines.add(centerVisible(ANSIColor.GOLD + "Health Potion" + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.EMERALD + "   .---.   " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.EMERALD + "  |  +  |  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.EMERALD + "   '---'   " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(" Type 'drink' to consume.");
        lines.add(" Restores 2 HP (max 5).");

        return new SidePanel("HEALTH POTION", INVENTORY_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private SidePanel buildTrapPanel() {
        List<String> lines = new ArrayList<>();
        lines.add(centerVisible(ANSIColor.CRIMSON + "  /\\  /\\  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.CRIMSON + " /  \\/  \\ " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.CRIMSON + " \\__/\\__/ " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(" A trap waits underfoot.");
        lines.add(" Careful where you step.");
        lines.add("");

        return new SidePanel("TRAP", ENCOUNTER_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private SidePanel buildExitPanel(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(centerVisible(ANSIColor.SKY_BLUE + "   ___   " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.SKY_BLUE + "  |_|_|  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.SKY_BLUE + "  |_|_|  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        if (player.getInventory().contains("key")) {
            lines.add(" The key turns. Type 'descend'");
            lines.add(" or 'go stairs' to continue.");
        } else {
            lines.add(" The stairs are locked.");
            lines.add(" A hidden guardian holds the key.");
        }

        return new SidePanel("EXIT STAIRCASE", INVENTORY_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private SidePanel buildDragonPanel(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(centerVisible(ANSIColor.CRIMSON + "  /\\___/\\  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.CRIMSON + " <  o o  > " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        lines.add(centerVisible(ANSIColor.CRIMSON + "  \\_^^^_/  " + ANSIColor.RESET,
            PANEL_CONTENT_WIDTH));
        if (player.getInventory().contains("excalibur")) {
            lines.add(" Excalibur burns in your hand.");
            lines.add(" Type 'roll' to strike.");
        } else if (player.getInventory().getCombatBonus() >= 4) {
            lines.add(" The dragon's eyes open.");
            lines.add(" Only Excalibur can pierce it.");
        } else {
            lines.add(" Dormant. Gather +4 and claim Excalibur.");
            lines.add(" The dragon sleeps.");
        }

        return new SidePanel("DRAGON'S LAIR", ENCOUNTER_BG, ANSIColor.BRIGHT_WHITE, lines);
    }

    private SidePanel buildLocationPanel(Player player, Tile currentTile) {
        boolean atStart = currentTile != null && currentTile.isStart();
        String location = atStart ? "START FOUNTAIN" : "EMPTY CHAMBER";

        List<String> lines = new ArrayList<>();
        lines.add("");
        if (atStart) {
            lines.add(centerVisible(ANSIColor.EMERALD + "        .        " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
            lines.add(centerVisible(ANSIColor.EMERALD + "      .   .      " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
            lines.add(centerVisible(ANSIColor.EMERALD + "    ~~~~~~~~~~~  " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
        } else {
            lines.add(centerVisible(ANSIColor.SHADOW_GRAY + "        .        " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
            lines.add(centerVisible(ANSIColor.SHADOW_GRAY + "      .   .      " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
            lines.add(centerVisible(ANSIColor.SHADOW_GRAY + "        .        " + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
        }

        String gear = player.getInventory().size() == 0 ? "None" : summarizeGear(player);
        lines.add(" Equipped : " + gear);
        lines.add(" Bonus    : +" + player.getInventory().getCombatBonus()
            + " STR  | Items " + player.getInventory().size());

        return new SidePanel("LOCATION: " + location, INVENTORY_BG,
            ANSIColor.BRIGHT_WHITE, lines);
    }

    private String topBorder(SidePanel panel) {
        String label = "[ " + panel.label() + " ]";
        if (label.length() > 38) {
            label = truncatePlain(label, 38);
        }
        int dashes = Math.max(1, PANEL_WIDTH - 7 - label.length());
        String coloredLabel = panel.background() + panel.foreground() + label + ANSIColor.RESET;
        return ANSIColor.SHADOW_GRAY + ".--- " + ANSIColor.RESET + coloredLabel +
            ANSIColor.SHADOW_GRAY + " " + "-".repeat(dashes) + "." + ANSIColor.RESET;
    }

    private String framePanelLine(String content) {
        String clipped = truncateVisible(content, PANEL_CONTENT_WIDTH);
        return ANSIColor.SHADOW_GRAY + "| " + ANSIColor.RESET +
            padVisible(clipped, PANEL_CONTENT_WIDTH) +
            ANSIColor.SHADOW_GRAY + " |" + ANSIColor.RESET;
    }

    private String bottomBorder() {
        return ANSIColor.SHADOW_GRAY + "'" + "-".repeat(PANEL_WIDTH - 2) + "'" + ANSIColor.RESET;
    }

    private SidePanel buildRollPanel(int first, int second, RollOutcomeStyle style, boolean settled) {
        boolean revealed = style != null;
        String label = revealed ? style.label : settled ? "SETTLED" : "ROLLING";
        String background = revealed ? style.background : ROLLING_BG;
        String foreground = revealed ? style.foreground : BLACK_TEXT;

        List<String> lines = new ArrayList<>();
        String[] left = dieFace(first);
        String[] right = dieFace(second);
        String diceColor = revealed
            ? style.color
            : settled ? ANSIColor.BRIGHT_WHITE : ANSIColor.GOLD;
        for (int row = 0; row < left.length; row++) {
            String dice = diceColor + left[row] + "   " + right[row] + ANSIColor.RESET;
            lines.add(centerVisible(dice, PANEL_CONTENT_WIDTH));
        }

        if (revealed) {
            lines.add(centerVisible(style.color + first + " + " + second + " = " +
                (first + second) + ANSIColor.RESET, PANEL_CONTENT_WIDTH));
        } else if (settled) {
            lines.add(centerVisible(ANSIColor.BRIGHT_WHITE + first + " + " + second + " = " +
                (first + second) + "   |   Fate decides..." + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
        } else {
            lines.add(centerVisible(ANSIColor.SHADOW_GRAY + "Await the result." + ANSIColor.RESET,
                PANEL_CONTENT_WIDTH));
        }
        return new SidePanel(label, background, foreground, lines);
    }

    private static String[] dieFace(int value) {
        String[] pips = switch (value) {
            case 1 -> new String[]{"       ", "   o   ", "       "};
            case 2 -> new String[]{" o     ", "       ", "     o "};
            case 3 -> new String[]{" o     ", "   o   ", "     o "};
            case 4 -> new String[]{" o   o ", "       ", " o   o "};
            case 5 -> new String[]{" o   o ", "   o   ", " o   o "};
            case 6 -> new String[]{" o   o ", " o   o ", " o   o "};
            default -> new String[]{"       ", "       ", "       "};
        };
        return new String[]{
            "+-------+",
            "|" + pips[0] + "|",
            "|" + pips[1] + "|",
            "|" + pips[2] + "|",
            "+-------+"
        };
    }

    private static String summarizeGear(Player player) {
        List<String> items = player.getInventory().getItems();
        if (items.isEmpty()) {
            return "None";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String itemId : items) {
            if ("key".equals(itemId)) {
                continue;
            }
            counts.merge(itemId, 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            return "None";
        }

        StringBuilder summary = new StringBuilder();
        String latest = player.getInventory().getLatestItem();
        if (latest != null && counts.containsKey(latest)) {
            appendGear(summary, latest, counts.remove(latest));
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            appendGear(summary, entry.getKey(), entry.getValue());
        }
        return summary.toString();
    }

    private static void appendGear(StringBuilder summary, String itemId, int count) {
        if (summary.length() > 0) {
            summary.append(", ");
        }
        summary.append(ItemCatalog.nameOf(itemId));
        if (count > 1) {
            summary.append(" x").append(count);
        }
    }

    private List<String> normalizePanel(List<String> lines) {
        List<String> normalized = new ArrayList<>(PANEL_CONTENT_ROWS);
        for (int i = 0; i < PANEL_CONTENT_ROWS; i++) {
            normalized.add(i < lines.size() ? lines.get(i) : "");
        }
        return normalized;
    }

    private void printArt(String[] lines, String[] colors) {
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, line.length());
        }
        int leftPad = Math.max(0, (SCREEN_WIDTH - maxWidth) / 2);
        for (int i = 0; i < lines.length; i++) {
            out.println(colors[i] + " ".repeat(leftPad) + lines[i] + ANSIColor.RESET);
        }
    }

    private static List<String> wrapPlain(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.replace('\r', ' ').split("\\n", -1)) {
            String remaining = paragraph.trim();
            if (remaining.isEmpty()) {
                continue;
            }
            while (remaining.length() > width) {
                int breakAt = remaining.lastIndexOf(' ', width);
                if (breakAt <= 0) {
                    breakAt = width;
                }
                lines.add(remaining.substring(0, breakAt).trim());
                remaining = remaining.substring(breakAt).trim();
            }
            if (!remaining.isEmpty()) {
                lines.add(remaining);
            }
        }
        return lines;
    }

    private static String centerPlain(String text, int width) {
        int padding = Math.max(0, width - text.length());
        int left = padding / 2;
        int right = padding - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    private static String centerVisible(String text, int width) {
        int padding = Math.max(0, width - visibleLength(text));
        int left = padding / 2;
        int right = padding - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    private static String padVisible(String text, int width) {
        int padding = Math.max(0, width - visibleLength(text));
        return text + " ".repeat(padding);
    }

    private static String truncatePlain(String text, int width) {
        if (width <= 0) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width);
    }

    private static String truncateVisible(String text, int width) {
        if (visibleLength(text) <= width) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int visible = 0;
        boolean escape = false;
        for (int i = 0; i < text.length() && visible < width; i++) {
            char c = text.charAt(i);
            if (c == '\u001B') {
                escape = true;
                result.append(c);
                continue;
            }
            if (escape) {
                result.append(c);
                if (c == 'm') {
                    escape = false;
                }
                continue;
            }
            result.append(c);
            visible++;
        }
        result.append(ANSIColor.RESET);
        return result.toString();
    }

    private static int visibleLength(String text) {
        int length = 0;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u001B') {
                escape = true;
                continue;
            }
            if (escape) {
                if (c == 'm') {
                    escape = false;
                }
                continue;
            }
            length++;
        }
        return length;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void clearScreen() {
        if (enableClearScreen) {
            out.print("\033[H\033[2J");
            out.flush();
        }
    }

    private record SidePanel(String label, String background, String foreground,
                             List<String> lines) {
    }
}
