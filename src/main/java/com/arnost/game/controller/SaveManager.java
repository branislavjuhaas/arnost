package com.arnost.game.controller;

import com.arnost.game.model.entity.Player;
import com.arnost.game.model.map.DungeonMap;
import com.arnost.game.model.state.GameSave;
import com.arnost.game.model.state.GameState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class SaveManager {
    public static final String DEFAULT_SAVE_FILE = "arnost-save.dat";

    private final Path savePath;

    public SaveManager() {
        this(Path.of(DEFAULT_SAVE_FILE));
    }

    public SaveManager(Path savePath) {
        if (savePath == null) {
            throw new IllegalArgumentException("Save path must not be null.");
        }
        this.savePath = savePath;
    }

    public Path getSavePath() {
        return savePath;
    }

    public boolean exists() {
        return Files.isRegularFile(savePath);
    }

    public boolean hasSaveFile() {
        return exists();
    }

    public void save(GameSave save) throws IOException {
        if (save == null) {
            throw new IllegalArgumentException("Save data must not be null.");
        }

        Path parent = savePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temp = savePath.resolveSibling(savePath.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
            out.writeObject(save);
        }

        try {
            Files.move(temp, savePath, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailed) {
            Files.move(temp, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void save(Player player, DungeonMap[] levels, GameState gameState, long seed)
        throws IOException {
        save(new GameSave(seed, player, levels, gameState));
    }

    public Optional<GameSave> load() {
        try {
            return Optional.of(read());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return Optional.empty();
        }
    }

    public GameSave read() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(savePath))) {
            Object value = in.readObject();
            if (value instanceof GameSave save) {
                return save;
            }
            throw new IOException("Save file does not contain an Arnost game.");
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(savePath);
        } catch (IOException ignored) {
            // A failed cleanup must not interrupt the game.
        }
    }
}
