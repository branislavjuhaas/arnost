package com.arnost.game.view;
import java.io.PrintStream;

public class TUIRenderer {
    private final PrintStream out;
    private final boolean enableClearScreen;
    public TUIRenderer(PrintStream out, boolean enableClearScreen) {
        this.out = out;
        this.enableClearScreen = enableClearScreen;
    }
}