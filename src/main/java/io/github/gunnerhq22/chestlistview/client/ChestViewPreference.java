package io.github.gunnerhq22.chestlistview.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
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
