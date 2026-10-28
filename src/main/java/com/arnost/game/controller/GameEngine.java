package com.arnost.game.controller;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.item.ItemCatalog;
import com.arnost.game.model.item.ItemType;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.map.Tile;
import com.arnost.game.model.monster.MonsterCatalog;
import com.arnost.game.model.monster.MonsterType;
import com.arnost.game.model.state.GameSave;
import com.arnost.game.model.state.GameState;
import com.arnost.game.view.TUIRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

public class GameEngine {
    private static final int START_X = 3;
    private static final int START_Y = 6;
    private static final int LEVEL_COUNT = GameState.MAX_LEVEL;

    private static final String NO_MONSTER_MESSAGE = "There is no monster here to fight.";
    private static final String INVALID_DIRECTION_MESSAGE =
        "Invalid direction. Use n/s/e/w, go n/s/e/w, or north/south/east/west.";
    private static final String WALL_MESSAGE =
        "Rubble blocks the way. Choose another direction.";
    private static final String POTION_MESSAGE =
        "You see a Health Potion on the ground. Type 'drink' to consume (+2 HP).";
    private static final String NO_POTION_MESSAGE =
        "There is no Health Potion here to drink.";
    private static final String STAIRS_REQUIRED_MESSAGE =
        "You must stand on an Exit Staircase [E] to descend.";
    private static final String DRAGON_SEALED_MESSAGE =
        "The Dragon's Lair is sealed. Gather +4 combat bonus and claim Excalibur.";
    private static final String EXIT_LOCKED_MESSAGE =
        "The Exit Staircase is locked. A hidden guardian still holds the Dungeon Key.";
    private static final String DRAGON_WEAPON_MESSAGE =
        "The dragon's scales turn your blade. Only Excalibur can slay it.";
    private static final String VICTORY_EPILOGUE =
        "With a final, holy strike of Excalibur, Ashgorath collapses into ashes. "
            + "Sir Arnost emerges from the catacombs bearing the lost Crown Jewels. "
            + "The Keep is fallen no more!";

    private Player player;
    private GameState gameState;
    private DungeonMap[] levels;
    private long seed;
    private Random lootRandom;

    private final Scanner scanner;
    private final TUIRenderer renderer;
    private final DiceRoller diceRoller;
    private final SaveManager saveManager;

    public GameEngine(Player player, DungeonMap map, GameState gameState,
                      InputStream in, TUIRenderer renderer, DiceRoller diceRoller) {
        this(player, levelsAround(map, gameState), gameState, in, renderer, diceRoller, null);
    }

    public GameEngine(Player player, DungeonMap map, GameState gameState,
                      InputStream in, TUIRenderer renderer, DiceRoller diceRoller,
                      SaveManager saveManager) {
        this(player, levelsAround(map, gameState), gameState, in, renderer, diceRoller, saveManager);
    }

    public GameEngine(Player player, long seed, GameState gameState,
                      InputStream in, TUIRenderer renderer, DiceRoller diceRoller) {
        this(player, generateLevels(seed), gameState, in, renderer, diceRoller, null);
    }

    public GameEngine(Player player, long seed, GameState gameState,
                      InputStream in, TUIRenderer renderer, DiceRoller diceRoller,
                      SaveManager saveManager) {
        this(player, generateLevels(seed), gameState, in, renderer, diceRoller, saveManager);
    }

    private GameEngine(Player player, DungeonMap[] levels, GameState gameState,
                       InputStream in, TUIRenderer renderer, DiceRoller diceRoller,
                       SaveManager saveManager) {
        this.player = player;
        this.gameState = gameState;
        this.levels = levels;
        this.seed = levels[0].getGenerationSeed();
        this.lootRandom = new Random(lootSeed(this.seed));
        this.scanner = new Scanner(in);
        this.renderer = renderer;
        this.diceRoller = diceRoller;
        this.saveManager = saveManager;
        activateDragonLairIfEligible();
    }

    public Player getPlayer() { return player; }
    public GameState getGameState() { return gameState; }
    public GameState getState() { return gameState; }
    public int getCurrentLevel() { return gameState.getCurrentLevel(); }
    public long getSeed() { return seed; }
    public SaveManager getSaveManager() { return saveManager; }

    public DungeonMap getCurrentMap() {
        int index = gameState.getCurrentLevel() - 1;
        return levels[index];
    }

    public DungeonMap getMap() {
        return getCurrentMap();
    }

    public DungeonMap[] getLevels() {
        return levels.clone();
    }

    public void startLoop() {
        boolean saveAvailable = saveManager != null && saveManager.exists();
        renderer.renderTitleScreen(saveAvailable);

        boolean attemptedLoad = false;
        boolean loaded = false;
        if (scanner.hasNextLine()) {
            String choice = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            attemptedLoad = choice.equals("l") || choice.equals("load");
            if (attemptedLoad && saveAvailable) {
                loaded = loadSavedGame();
            }
        }

        if (loaded) {
            saveAvailable = true;
        } else {
            saveIfPossible();
            renderPrologue();
        }

        String lastMsg;
        if (loaded) {
            lastMsg = "The saved keep remembers you, Sir Arnost.";
        } else if (attemptedLoad && saveAvailable) {
            lastMsg = "The save could not be read. A new keep rises from the ashes.";
        } else {
            lastMsg = "Welcome to Fallen Keep, Sir Arnost! Hunt the guardian who holds the key.";
        }

        while (true) {
            markVisibleArea();
            renderer.render(player, getCurrentMap(), gameState.getCurrentLevel(), lastMsg);

            while (!gameState.isGameWon() && !gameState.isPlayerDead() && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                if (line.equals("quit")) {
                    saveIfPossible();
                    return;
                }

                lastMsg = executeCommand(line);
                if (!gameState.isGameWon() && !gameState.isPlayerDead()) {
                    saveIfPossible();
                }

                if (!gameState.isGameWon()) {
                    renderer.render(player, getCurrentMap(), gameState.getCurrentLevel(), lastMsg);
                }
            }

            if (gameState.isGameWon()) {
                deleteSave();
                renderer.renderVictoryScreen(player, VICTORY_EPILOGUE);
                return;
            }

            if (!gameState.isPlayerDead()) {
                saveIfPossible();
                return;
            }

            deleteSave();
            renderer.renderDefeatScreen(player, "The Fallen Keep claims another hero.");
            if (!promptRestart()) {
                return;
            }

            resetForNewRun();
            lastMsg = "A new keep rises from the ashes. Welcome back, Sir Arnost!";
        }
    }

    private void renderPrologue() {
        renderer.renderStoryScreen(
            "PROLOGUE",
            "THE GATE OF THE FALLEN KEEP",
            "Press [ENTER] to enter the keep...",
            "The Fallen Keep has stood silent for a hundred years, its gates sealed beneath ash and ivy.",
            "Sir Arnost, last knight of the broken order, rides through the mist with a worn blade and a vow he cannot break.",
            "Two Dungeon Keys wait behind hidden guardians. On the third level, the holy blade Excalibur lies hidden, and at the bottom of the Dragon's Abyss, Ashgorath guards the lost Crown Jewels.",
            "Descend. Claim the keys. Draw Excalibur. End the curse of the Fallen Keep.");
        waitForStoryContinue();
    }

    private void waitForStoryContinue() {
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    private boolean promptRestart() {
        while (scanner.hasNextLine()) {
            String choice = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (choice.equals("r") || choice.equals("restart")) {
                return true;
            }
            if (choice.equals("q") || choice.equals("quit")) {
                return false;
            }
            renderer.renderDefeatScreen(player, "Unknown choice. Type 'r' to restart or 'q' to quit.");
        }
        return false;
    }

    private void resetForNewRun() {
        seed = System.nanoTime();
        levels = generateLevels(seed);
        lootRandom = new Random(lootSeed(seed));
        player.resetForNewGame();
        gameState.reset();
        saveIfPossible();
    }

    private String executeCommand(String line) {
        if (line.equals("help")) {
            renderer.renderHelp();
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            return "Back in the keep. Type 'help' whenever you need guidance.";
        }

        if (line.equals("save")) {
            return saveGameCommand();
        }

        if (line.equals("descend") || line.equals("go stairs")
            || line.equals("go stair") || line.equals("stairs") || line.equals("staircase")) {
            return handleDescend();
        }

        if (line.equals("drink") || line.equals("drink potion")
            || line.equals("take potion") || line.equals("use potion")) {
            return handleDrink();
        }

        if (line.equals("roll")) {
            return handleRoll();
        }

        if (isMovementCommand(line)) {
            String direction = directionFromCommand(line);
            if (direction == null) {
                return INVALID_DIRECTION_MESSAGE;
            }
            return handleMovement(direction);
        }

        if (line.isEmpty()) {
            return "Enter a command. Type 'help' for instructions.";
        }

        return "Unknown command. Type 'help' for instructions.";
    }

    private String handleMovement(String direction) {
        int nx = player.getX();
        int ny = player.getY();
        switch (direction) {
            case "north" -> ny--;
            case "south" -> ny++;
            case "east" -> nx++;
            case "west" -> nx--;
        }

        Tile target = getCurrentMap().getTile(nx, ny);
        if (target == null) {
            return "Blocked by dungeon boundary.";
        }
        if (isWall(target)) {
            return WALL_MESSAGE;
        }

        player.setPosition(nx, ny);
        markVisibleArea();
        activateDragonLairIfEligible();

        if (hasActiveMonster(target)) {
            return encounterMessage(target);
        }

        if (target.isTrapArmed()) {
            target.setTrapTriggered(true);
            player.takeDamage(1);
            if (player.getHp() <= 0) {
                gameState.setPlayerDead(true);
                return "A trap springs! You lose 1 HP and fall.";
            }
            return "A trap springs! You lose 1 HP. HP ["
                + player.getHp() + "/" + player.getMaxHp() + "].";
        }

        if (target.hasPotion()) {
            return POTION_MESSAGE;
        }

        String foundItem = collectItem(target);
        if (foundItem != null) {
            activateDragonLairIfEligible();
            if (hasActiveMonster(target)) {
                return encounterMessage(target);
            }
            if ("key".equals(target.getItemId())) {
                return "Moved " + direction + " to [" + nx + "," + ny + "]. "
                    + "Found the Dungeon Key! The exit stairs can now be unlocked.";
            }
            if ("excalibur".equals(target.getItemId())) {
                return "Moved " + direction + " to [" + nx + "," + ny + "]. "
                    + "Found Excalibur! Only its edge can slay the dragon.";
            }
            return "Moved " + direction + " to [" + nx + "," + ny + "]. Found " + foundItem + "!";
        }

        if (target.isExit()) {
            String stairsState = player.getInventory().contains("key")
                ? "The key turns in the lock. Type 'descend' or 'go stairs' to continue."
                : "The stairs are locked. A hidden guardian holds the Dungeon Key.";
            return "Moved " + direction + " to [" + nx + "," + ny + "]. "
                + "You stand on an Exit Staircase [E]. " + stairsState;
        }

        if (target.isDragonLair()) {
            return DRAGON_SEALED_MESSAGE;
        }

        return "Moved " + direction + " to [" + nx + "," + ny + "]";
    }

    private String handleDrink() {
        Tile currentTile = getCurrentMap().getTile(player.getX(), player.getY());
        if (currentTile == null || !currentTile.hasPotion()) {
            return NO_POTION_MESSAGE;
        }

        currentTile.setHasPotion(false);
        int restored = player.heal(2);
        return "You drink the Health Potion. Restored " + restored + " HP. HP ["
            + player.getHp() + "/" + player.getMaxHp() + "].";
    }

    private String handleDescend() {
        Tile currentTile = getCurrentMap().getTile(player.getX(), player.getY());
        if (currentTile == null || !currentTile.isExit()) {
            return STAIRS_REQUIRED_MESSAGE;
        }
        if (!player.getInventory().contains("key")) {
            return EXIT_LOCKED_MESSAGE;
        }

        int currentLevel = gameState.getCurrentLevel();
        if (currentLevel >= LEVEL_COUNT) {
            return "There is no deeper level in the Fallen Keep.";
        }

        player.getInventory().removeItem("key");
        int nextLevel = currentLevel + 1;
        gameState.setCurrentLevel(nextLevel);
        player.teleportTo(START_X, START_Y);

        markVisibleArea();
        activateDragonLairIfEligible();

        renderDescentCutscene(nextLevel);
        return levelArrivalMessage(nextLevel);
    }

    private void renderDescentCutscene(int level) {
        switch (level) {
            case 2 -> renderer.renderStoryScreen(
                "LEVEL 2",
                "THE SUNKEN CRYPTS",
                "Press [ENTER] to enter the Sunken Crypts...",
                "You unlock the heavy iron grate and descend into the Sunken Crypts. The air grows thick with ancient venom...",
                "Water drips through the vaulted dark. The Mummy Lord's servants stir, and a hidden guardian carries the next Dungeon Key.",
                "Find the key. Survive the venom. Descend deeper.");
            case 3 -> renderer.renderStoryScreen(
                "LEVEL 3",
                "THE DRAGON'S ABYSS",
                "Press [ENTER] to enter the Dragon's Abyss...",
                "Unlocking the vault with the Mummy Lord's Key, you descend into the Dragon's Abyss. Volcanic heat radiates from below...",
                "The Knight Guardian's blade waits in the dark. Ashgorath's shadow coils around the lost Crown Jewels.",
                "Claim Excalibur. Slay the dragon. End the curse.");
            default -> renderer.renderStoryScreen(
                "LEVEL " + level,
                "THE FALLEN KEEP",
                "Press [ENTER] to continue...",
                "You descend deeper into the Fallen Keep.");
        }
        waitForStoryContinue();
    }

    private String levelArrivalMessage(int level) {
        return switch (level) {
            case 2 -> "A hidden guardian holds Level 2's Dungeon Key. "
                + "Venom Spiders and hidden traps guard the way.";
            case 3 -> "A hidden champion guards Excalibur. Plague Mummies stalk the halls. "
                + "The Dragon's Lair waits at [3,1].";
            default -> "The stairwell closes behind you.";
        };
    }

    private void activateDragonLairIfEligible() {
        if (gameState.getCurrentLevel() != 3
            || player.getInventory().getCombatBonus() < 4) {
            return;
        }

        DungeonMap map = getCurrentMap();
        Tile lair = map.getDragonLairTile();
        if (lair == null || lair.isCleared()) {
            return;
        }
        if (!"dragon".equals(lair.getMonsterId())) {
            MapGenerator.activateDragonLair(map);
        }
    }

    private String collectItem(Tile tile) {
        if (tile.getItemId() == null || tile.isCleared()) {
            return null;
        }

        String itemId = tile.getItemId();
        tile.setCleared(true);
        player.getInventory().addItem(itemId);

        ItemType item = ItemCatalog.get(itemId);
        if (item == null) {
            return itemId;
        }

        String bonus = item.combatBonus() > 0
            ? " (+" + item.combatBonus() + " Strength)"
            : "";
        return item.name() + bonus;
    }

    private String handleRoll() {
        Tile currentTile = getCurrentMap().getTile(player.getX(), player.getY());
        if (currentTile == null || !hasActiveMonster(currentTile)) {
            return NO_MONSTER_MESSAGE;
        }

        String monsterId = currentTile.getMonsterId();
        boolean strengthWasHidden = currentTile.isMonsterStrengthHidden();
        if (strengthWasHidden) {
            currentTile.setMonsterStrengthHidden(false);
        }

        if ("dragon".equals(monsterId)) {
            if (!player.getInventory().contains("excalibur")) {
                int strength = effectiveMonsterStrength(currentTile, monsterId);
                String reveal = strengthWasHidden
                    ? "Revealed STR " + strength + ". "
                    : "";
                return reveal + DRAGON_WEAPON_MESSAGE;
            }
            return handleDragonRoll(currentTile, "");
        }

        int monsterStrength = effectiveMonsterStrength(currentTile, monsterId);
        String reveal = strengthWasHidden
            ? "Revealed STR " + monsterStrength + ". "
            : "";

        DiceRoller.DiceRoll roll = diceRoller.rollDetailed();

        CombatResolver.Outcome outcome =
            CombatResolver.evaluateCombat(player, monsterStrength, roll.total());
        TUIRenderer.RollOutcomeStyle style = switch (outcome) {
            case VICTORY -> TUIRenderer.RollOutcomeStyle.WIN;
            case DRAW -> TUIRenderer.RollOutcomeStyle.DRAW;
            case DEFEAT -> TUIRenderer.RollOutcomeStyle.LOSS;
        };
        renderer.renderRollEffect(player, getCurrentMap(), gameState.getCurrentLevel(),
            roll.first(), roll.second(), style);
        CombatResolver.applyOutcome(player, outcome);

        if (outcome == CombatResolver.Outcome.VICTORY) {
            currentTile.setCleared(true);
            String itemId = MonsterCatalog.rollLoot(monsterId, lootRandom);
            if (itemId != null) {
                currentTile.setItemId(itemId);
                player.getInventory().addItem(itemId);
            }

            activateDragonLairIfEligible();

            MonsterType monster = MonsterCatalog.get(monsterId);
            if (itemId == null) {
                return reveal + (monster == null
                    ? "Victory. The monster is slain."
                    : "Defeated " + monster.name() + "! It dropped nothing.");
            }

            if ("key".equals(itemId) && monster != null) {
                return reveal + "Defeated " + monster.name()
                    + "! It carried the Dungeon Key. The exit stairs can now be unlocked.";
            }
            if ("excalibur".equals(itemId) && monster != null) {
                return reveal + "Defeated " + monster.name()
                    + "! It yielded Excalibur (+4 Strength). Only its edge can slay the dragon.";
            }

            ItemType item = ItemCatalog.get(itemId);
            if (monster != null && item != null) {
                String bonus = item.combatBonus() > 0
                    ? " (+" + item.combatBonus() + " Strength)"
                    : "";
                return reveal + "Defeated " + monster.name() + "! Dropped " + item.name()
                    + bonus + ".";
            }
            return reveal + "Victory. The monster is slain.";
        }

        if (outcome == CombatResolver.Outcome.DRAW) {
            return reveal + "Draw. Neither side yields; you are pushed back.";
        }

        String venomMessage = "";
        int poisonDamage = MonsterCatalog.poisonDamageOf(monsterId);
        if (poisonDamage > 0 && player.getHp() > 0) {
            player.takeDamage(poisonDamage);
            venomMessage = " Venom burns for " + poisonDamage + " more HP!";
        }
        if (player.getHp() <= 0) {
            gameState.setPlayerDead(true);
        }
        return reveal + "Defeat. Lost 1 HP and pushed back." + venomMessage;
    }

    private String handleDragonRoll(Tile dragonTile, String reveal) {
        int lives = dragonLives(dragonTile);
        DiceRoller.DiceRoll roll = diceRoller.rollDetailed();

        if (roll.total() < MonsterCatalog.DRAGON_MIN_ROLL) {
            CombatResolver.Outcome outcome = roll.total() == MonsterCatalog.DRAGON_MIN_ROLL - 1
                ? CombatResolver.Outcome.DRAW
                : CombatResolver.Outcome.DEFEAT;
            TUIRenderer.RollOutcomeStyle style = outcome == CombatResolver.Outcome.DRAW
                ? TUIRenderer.RollOutcomeStyle.DRAW
                : TUIRenderer.RollOutcomeStyle.LOSS;
            renderer.renderRollEffect(player, getCurrentMap(), gameState.getCurrentLevel(),
                roll.first(), roll.second(), style);
            CombatResolver.applyOutcome(player, outcome);

            String threshold = "A strike needs a 2d6 sum of "
                + MonsterCatalog.DRAGON_MIN_ROLL + "+.";
            if (outcome == CombatResolver.Outcome.DRAW) {
                return reveal + "Draw. Ashgorath turns the blade aside; you are pushed back. "
                    + threshold;
            }
            if (player.getHp() <= 0) {
                gameState.setPlayerDead(true);
                return reveal + "Ashgorath's claws find you. You fall. " + threshold;
            }
            return reveal + "Defeat. Ashgorath's claws cost you 1 HP; you are pushed back. "
                + threshold;
        }

        int remaining = lives - 1;
        dragonTile.setMonsterLives(remaining);

        if (remaining > 0) {
            renderer.renderRollEffect(player, getCurrentMap(), gameState.getCurrentLevel(),
                roll.first(), roll.second(), TUIRenderer.RollOutcomeStyle.STRIKE);
            String lifeWord = remaining == 1 ? "life" : "lives";
            return reveal + "Excalibur bites deep! Ashgorath has "
                + remaining + " " + lifeWord + " left.";
        }

        renderer.renderRollEffect(player, getCurrentMap(), gameState.getCurrentLevel(),
            roll.first(), roll.second(), TUIRenderer.RollOutcomeStyle.WIN);
        dragonTile.setCleared(true);
        String itemId = MonsterCatalog.rollLoot("dragon", lootRandom);
        if (itemId != null) {
            dragonTile.setItemId(itemId);
            player.getInventory().addItem(itemId);
        }
        gameState.setGameWon(true);
        return reveal + "Victory. Ashgorath is slain and the Crown Jewels are yours.";
    }

    private static int dragonLives(Tile dragonTile) {
        int lives = dragonTile.getMonsterLives();
        if (lives <= 0) {
            lives = MonsterCatalog.DRAGON_LIVES;
            dragonTile.setMonsterLives(lives);
        }
        return lives;
    }

    private String saveGameCommand() {
        if (saveManager == null) {
            return "Saving is not available in this session.";
        }
        try {
            saveManager.save(snapshot());
            return "Game saved.";
        } catch (IOException e) {
            return "The keep could not be saved.";
        }
    }

    private void saveIfPossible() {
        if (saveManager == null) {
            return;
        }
        if (gameState.isGameWon() || gameState.isPlayerDead()) {
            deleteSave();
            return;
        }
        try {
            saveManager.save(snapshot());
        } catch (IOException ignored) {
            // Saving must never break the command loop.
        }
    }

    private void deleteSave() {
        if (saveManager != null) {
            saveManager.delete();
        }
    }

    private GameSave snapshot() {
        return new GameSave(seed, player, levels, gameState);
    }

    private boolean loadSavedGame() {
        if (saveManager == null) {
            return false;
        }

        Optional<GameSave> loaded = saveManager.load();
        if (loaded.isEmpty()) {
            return false;
        }

        GameSave save = loaded.get();
        if (save.player() == null || save.gameState() == null
            || save.levels() == null || save.levels().length < LEVEL_COUNT) {
            return false;
        }
        for (int i = 0; i < LEVEL_COUNT; i++) {
            if (save.levels()[i] == null) {
                return false;
            }
        }
        if (save.gameState().getCurrentLevel() < GameState.MIN_LEVEL
            || save.gameState().getCurrentLevel() > GameState.MAX_LEVEL) {
            return false;
        }
        if (save.gameState().isGameWon() || save.gameState().isPlayerDead()) {
            return false;
        }

        player.restoreFrom(save.player());
        gameState.restoreFrom(save.gameState());
        levels = save.levels();
        seed = save.seed();
        lootRandom = new Random(lootSeed(seed));

        markVisibleArea();
        activateDragonLairIfEligible();
        return true;
    }

    private void markVisibleArea() {
        DungeonMap map = getCurrentMap();
        int[][] offsets = {
            {0, 0},
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
        };
        for (int[] offset : offsets) {
            Tile tile = map.getTile(player.getX() + offset[0], player.getY() + offset[1]);
            if (tile != null) {
                tile.setExplored(true);
            }
        }
    }

    private static DungeonMap[] levelsAround(DungeonMap map, GameState gameState) {
        long mapSeed = map != null && map.getGenerationSeed() != 0L
            ? map.getGenerationSeed()
            : System.nanoTime();
        DungeonMap[] generated = generateLevels(mapSeed);
        if (map == null) {
            return generated;
        }

        int requestedLevel = gameState != null ? gameState.getCurrentLevel() : 1;
        int index = Math.max(0, Math.min(LEVEL_COUNT - 1, requestedLevel - 1));
        generated[index] = map;
        return generated;
    }

    private static DungeonMap[] generateLevels(long seed) {
        DungeonMap[] generated = new DungeonMap[LEVEL_COUNT];
        for (int i = 0; i < LEVEL_COUNT; i++) {
            generated[i] = MapGenerator.generateSeededMap(seed, i + 1);
        }
        return generated;
    }

    private static boolean isMovementCommand(String line) {
        if (line.equals("go") || line.startsWith("go ")) {
            return true;
        }
        if (line.length() == 3 && line.startsWith("go")) {
            return resolveDirection(line.substring(2)) != null;
        }
        return resolveDirection(line) != null;
    }

    private static String directionFromCommand(String line) {
        if (line.startsWith("go")) {
            return resolveDirection(line.substring(2).trim());
        }
        return resolveDirection(line);
    }

    private static String resolveDirection(String token) {
        return switch (token) {
            case "n", "no", "nor", "north", "u", "up" -> "north";
            case "s", "so", "sou", "south", "d", "down" -> "south";
            case "e", "ea", "eas", "east", "r", "right" -> "east";
            case "w", "we", "wes", "west", "l", "left" -> "west";
            default -> null;
        };
    }

    private static boolean isWall(Tile tile) {
        return tile != null && tile.isWall();
    }

    private static boolean hasActiveMonster(Tile tile) {
        return tile.getMonsterId() != null && !tile.isCleared();
    }

    private static String encounterMessage(Tile tile) {
        String venom = MonsterCatalog.isVenomous(tile.getMonsterId())
            ? " Venom drips from its fangs: defeat costs +1 HP."
            : "";
        return tile.isMonsterStrengthHidden()
            ? "Encountered a hidden MONSTER!" + venom + " Its strength is concealed until you roll."
            : "Encountered a hidden MONSTER!" + venom + " Type 'roll' to fight.";
    }

    private int effectiveMonsterStrength(Tile tile, String monsterId) {
        int baseStrength = tile.getMonsterStrength() > 0
            ? tile.getMonsterStrength()
            : MonsterCatalog.baseStrengthOf(monsterId);
        return MonsterCatalog.effectiveStrength(
            baseStrength, player.getInventory().getCombatBonus());
    }

    private static long lootSeed(long seed) {
        return seed ^ 0x9E3779B97F4A7C15L;
    }
}
