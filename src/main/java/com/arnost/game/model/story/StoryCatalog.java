package com.arnost.game.model.story;

import com.arnost.game.data.Json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Narrative content loaded from {@code data/story.json}.
 *
 * <p>Story screens and the victory epilogue are content, not engine logic.
 * Keeping them in JSON makes copy review separate from command and combat
 * changes while preserving the exact strings used by the renderer and tests.</p>
 */
public final class StoryCatalog {
    public record Screen(String chapter, String title, String prompt,
                         List<String> paragraphs) {
    }

    private static final Map<String, Object> DATA =
        Json.readObject("/data/story.json");

    private static final Screen PROLOGUE =
        readScreen(Json.object(DATA, "prologue"), "story.prologue");
    private static final Map<Integer, Screen> DESCENTS = readDescents();
    private static final Screen DEFAULT_DESCENT =
        readScreen(Json.object(Json.object(DATA, "descents"), "default"),
            "story.descents.default");
    private static final Map<String, String> ARRIVAL_MESSAGES =
        Collections.unmodifiableMap(Json.stringMap(DATA.get("arrivalMessages"),
            "story.arrivalMessages"));
    private static final String VICTORY_EPILOGUE =
        Json.string(DATA, "victoryEpilogue");

    private StoryCatalog() {
    }

    private static Map<Integer, Screen> readDescents() {
        Map<String, Object> raw = Json.object(DATA.get("descents"), "story.descents");
        Map<Integer, Screen> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if ("default".equals(entry.getKey())) {
                continue;
            }
            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "Invalid level key in story.descents: " + entry.getKey(), e);
            }
            result.put(level, readScreen(Json.object(entry.getValue(),
                "story.descents." + entry.getKey()), "story.descents." + entry.getKey()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Screen readScreen(Map<String, Object> definition, String context) {
        return new Screen(
            Json.string(definition, "chapter"),
            Json.string(definition, "title"),
            Json.string(definition, "prompt"),
            List.copyOf(Json.stringList(definition.get("paragraphs"),
                context + ".paragraphs"))
        );
    }

    public static Screen prologue() {
        return PROLOGUE;
    }

    public static Screen descent(int level) {
        Screen screen = DESCENTS.get(level);
        if (screen != null) {
            return screen;
        }
        return new Screen(
            "LEVEL " + level,
            DEFAULT_DESCENT.title(),
            DEFAULT_DESCENT.prompt(),
            DEFAULT_DESCENT.paragraphs()
        );
    }

    public static String arrivalMessage(int level) {
        String message = ARRIVAL_MESSAGES.get(Integer.toString(level));
        if (message == null) {
            message = ARRIVAL_MESSAGES.get("default");
        }
        if (message == null) {
            throw new IllegalStateException("Missing default arrival message in story.json");
        }
        return message;
    }

    public static String victoryEpilogue() {
        return VICTORY_EPILOGUE;
    }
}
