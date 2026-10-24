package com.arnost.game.view;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import java.io.PrintStream;

public class TUIRenderer {
    private final PrintStream out;
    private final boolean enableClearScreen;

    public TUIRenderer(PrintStream out, boolean enableClearScreen) {
        this.out = out;
        this.enableClearScreen = enableClearScreen;
    }

    public void render(Player player, DungeonMap map, String statusMsg) {
        if (enableClearScreen) { out.print("\033[H\033[2J"); out.flush(); }
        out.println(ANSIColor.GOLD + "=== ARNOST: LEGEND OF THE FALLEN KEEP ===" + ANSIColor.RESET);
        out.println(ANSIColor.EMERALD + "HP: " + player.getHp() + "/" + player.getMaxHp() + " | Bonus: +" + player.getInventory().getCombatBonus() + ANSIColor.RESET);
        out.println(ANSIColor.SKY_BLUE + "----------------------------------------" + ANSIColor.RESET);
        for (int y = 0; y < DungeonMap.SIZE; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < DungeonMap.SIZE; x++) {
                if (x == player.getX() && y == player.getY()) sb.append(ANSIColor.CYAN + "[*]" + ANSIColor.RESET);
                else sb.append(ANSIColor.SHADOW_GRAY + "[.]" + ANSIColor.RESET);
            }
            out.println(sb.toString());
        }
        out.println(ANSIColor.BRIGHT_WHITE + "Status: " + statusMsg + ANSIColor.RESET);
    }
}