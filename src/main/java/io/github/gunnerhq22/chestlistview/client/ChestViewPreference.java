package io.github.gunnerhq22.chestlistview.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

final class ChestViewPreference {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path file;
    private final Properties savedModes = new Properties();

    ChestViewPreference(Path gameDirectory) {
        file = gameDirectory.resolve("config").resolve("chestlistview-chests.properties");
        try (var reader = Files.newBufferedReader(file)) {
            savedModes.load(reader);
            savedModes.entrySet().removeIf(entry -> ChestViewMode.fromId(entry.getValue().toString()) == ChestViewMode.GRID);
        } catch (NoSuchFileException ignored) {
            // Unseen chests use Grid.
        } catch (IOException | IllegalArgumentException exception) {
            savedModes.clear();
            LOGGER.warn("Could not read chest view preferences from {}", file, exception);
        }
    }

    ChestViewMode getMode(String key) {
        return ChestViewMode.fromId(savedModes.getProperty(key));
    }

    void setMode(String key, ChestViewMode mode) {
        if (getMode(key) == mode) {
            return;
        }
        if (mode == ChestViewMode.GRID) {
            savedModes.remove(key);
        } else {
            savedModes.setProperty(key, mode.id);
        }
        save();
    }

    void migrateChestKey(String key) {
        int separator = key.lastIndexOf('|');
        switch (key.substring(separator + 1)) {
            case "chest", "trapped_chest", "double_chest", "double_trapped_chest" -> {}
            default -> { return; }
        }
        String legacy = key.substring(0, separator);
        Object previous = savedModes.remove(legacy);
        if (previous != null) {
            savedModes.putIfAbsent(key, previous);
            save();
        }
    }

    void prune(String scope, BiPredicate<BlockPos, String> keep) {
        if (savedModes.keySet().removeIf(entry -> entry instanceof String key && key.startsWith(scope)
                && !keepLocation(key.substring(scope.length()), keep))) {
            save();
        }
    }

    private static boolean keepLocation(String location, BiPredicate<BlockPos, String> keep) {
        String[] parts = location.split("\\|", 2);
        String[] coordinates = parts[0].split(",", -1);
        if (coordinates.length != 3) {
            return true;
        }
        BlockPos pos;
        try {
            pos = new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]),
                    Integer.parseInt(coordinates[2]));
        } catch (NumberFormatException ignored) {
            return true;
        }
        return keep.test(pos, parts.length == 2 ? parts[1] : "");
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "chestlistview-", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temporary)) {
                    savedModes.store(writer, "Chest List View preferences. Unlisted chests use Grid.");
                }
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save chest view preferences to {}", file, exception);
        }
    }
}
