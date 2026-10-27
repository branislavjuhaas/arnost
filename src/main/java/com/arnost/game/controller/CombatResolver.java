package com.arnost.game.controller;
import com.arnost.game.model.entity.Player;

public class CombatResolver {
    public enum Outcome { VICTORY, DRAW, DEFEAT }

    public static Outcome evaluateCombat(Player player, int monsterStrength, int rawRoll) {
        int totalAttack = rawRoll + player.getInventory().getCombatBonus();
        if (totalAttack > monsterStrength) return Outcome.VICTORY;
        if (totalAttack == monsterStrength) return Outcome.DRAW;
        return Outcome.DEFEAT;
    }

    public static void applyOutcome(Player player, Outcome outcome) {
        if (outcome == Outcome.DRAW) {
            player.revertPosition();
        } else if (outcome == Outcome.DEFEAT) {
            player.takeDamage(1);
            player.revertPosition();
        }
    }

    public static Outcome resolveCombat(Player player, int monsterStrength, int rawRoll) {
        Outcome outcome = evaluateCombat(player, monsterStrength, rawRoll);
        applyOutcome(player, outcome);
        return outcome;
    }
}
