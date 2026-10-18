package com.arnost.game.controller;
import com.arnost.game.model.entity.Player;

public class CombatResolver {
    public enum Outcome { VICTORY, DRAW, DEFEAT }
    public static Outcome resolveCombat(Player player, int monsterStrength, int rawRoll) {
        int totalAttack = rawRoll + player.getInventory().getCombatBonus();
        if (totalAttack > monsterStrength) return Outcome.VICTORY;
        else if (totalAttack == monsterStrength) { player.revertPosition(); return Outcome.DRAW; }
        else { player.takeDamage(1); player.revertPosition(); return Outcome.DEFEAT; }
    }
}