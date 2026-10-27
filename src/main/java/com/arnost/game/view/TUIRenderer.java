package com.arnost.game.view;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import java.io.PrintStream;

public class TUIRenderer {
    private final PrintStream out;
    private final boolean enableClearScreen;

    public TUIRenderer(PrintStream out, boolean enableClearScreen) {
        this.out = out;
        this.enableClearScreen = enableClearScreen;
    }

    public void renderTitleScreen() {
        if (enableClearScreen) {
            out.print("\033[H\033[2J");
            out.flush();
        }

        out.println(ANSIColor.GOLD + """
        ================================================================================
         █████╗ ██████╗ ███╗   ██╗██████╗ ███████╗████████╗
        ██╔══██╗██╔══██╗████╗  ██║██╔══██╗██╔════╝╚══██╔══╝
        ███████║██████╔╝██╔██╗ ██║██║  ██║███████╗   ██║   
        ██╔══██║██╔══██╗██║╚██╗██║██║  ██║╚════██║   ██║   
        ██║  ██║██║  ██║██║ ╚████║██████╔╝███████║   ██║   
        ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═════╝ ╚══════╝   ╚═╝   
                    LEGEND OF THE FALLEN KEEP
        ================================================================================
        """ + ANSIColor.RESET);

        out.println(ANSIColor.CYAN + "         A 2d6 Procedural CLI Dungeon Crawler in Java 27\n" + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "  [S] Start Fountain  |  [D] Dragon's Lair  |  [?] Unexplored Tile" + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + "  Commands: 'go north/south/east/west', 'roll', 'quit'\n" + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + "  Press [ENTER] to enter the Keep..." + ANSIColor.RESET);
        out.flush();
    }

    public void render(Player player, DungeonMap map, String statusMsg) {
        if (enableClearScreen) {
            out.print("\033[H\033[2J");
            out.flush();
        }

        out.println(ANSIColor.GOLD + "=== ARNOST: LEGEND OF THE FALLEN KEEP ===" + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "HP: " + player.getHp() + "/" + player.getMaxHp() +
                    " | Combat Bonus: +" + player.getInventory().getCombatBonus() + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + "----------------------------------------" + ANSIColor.RESET);

        for (int y = 0; y < DungeonMap.SIZE; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                Tile tile = map.getTile(x, y);
                if (x == player.getX() && y == player.getY()) {
                    sb.append(ANSIColor.CYAN + "[*]" + ANSIColor.RESET);
                } else if (!tile.isExplored()) {
                    sb.append(ANSIColor.SHADOW_GRAY + "[?]" + ANSIColor.RESET);
                } else if (tile.getType().equals("START")) {
                    sb.append(ANSIColor.EMERALD + "[S]" + ANSIColor.RESET);
                } else if (tile.getType().equals("DRAGON_LAIR")) {
                    sb.append(ANSIColor.CRIMSON + "[D]" + ANSIColor.RESET);
                } else {
                    sb.append(ANSIColor.BRIGHT_WHITE + "[.]" + ANSIColor.RESET);
                }
            }
            out.println(sb.toString());
        }

        out.println(ANSIColor.SKY_BLUE + "----------------------------------------" + ANSIColor.RESET);
        out.println(ANSIColor.BRIGHT_WHITE + "Status: " + statusMsg + ANSIColor.RESET);
        out.print(ANSIColor.CYAN + "> " + ANSIColor.RESET);
    }
}