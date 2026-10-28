package com.arnost.game.view;

public class ANSIColor {
    public static final String RESET = "\u001B[0m";
    public static final String GOLD = "\u001B[38;5;220m";
    public static final String CRIMSON = "\u001B[38;5;196m";
    public static final String EMERALD = "\u001B[38;5;46m";
    public static final String SKY_BLUE = "\u001B[38;5;39m";
    public static final String CYAN = "\u001B[38;5;51m";
    public static final String SHADOW_GRAY = "\u001B[38;5;240m";
    public static final String BRIGHT_WHITE = "\u001B[38;5;255m";
    public static final String PURPLE = "\u001B[38;5;141m";
    public static final String BONE = "\u001B[38;5;252m";
    public static final String LAVA = "\u001B[38;5;208m";

    /**
     * Resolves a symbolic color name used by the bundled JSON data.
     * Keeping names in the data files avoids embedding raw escape sequences.
     */
    public static String named(String name) {
        if (name == null) {
            throw new IllegalArgumentException("ANSI color name must not be null.");
        }
        return switch (name) {
            case "GOLD" -> GOLD;
            case "CRIMSON" -> CRIMSON;
            case "EMERALD" -> EMERALD;
            case "SKY_BLUE" -> SKY_BLUE;
            case "CYAN" -> CYAN;
            case "SHADOW_GRAY" -> SHADOW_GRAY;
            case "BRIGHT_WHITE" -> BRIGHT_WHITE;
            case "PURPLE" -> PURPLE;
            case "BONE" -> BONE;
            case "LAVA" -> LAVA;
            default -> throw new IllegalArgumentException("Unknown ANSI color: " + name);
        };
    }
}