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
    private final Properties listChests = new Properties();

    ChestViewPreference(Path gameDirectory) {
        file = gameDirectory.resolve("config").resolve("chestlistview-chests.properties");
        try (var reader = Files.newBufferedReader(file)) {
            listChests.load(reader);
            listChests.entrySet().removeIf(entry -> !"list".equals(entry.getValue()));
        } catch (NoSuchFileException ignored) {
            // Unseen chests use Grid.
        } catch (IOException | IllegalArgumentException exception) {
            listChests.clear();
            LOGGER.warn("Could not read chest view preferences from {}", file, exception);
        }
    }

    boolean isListPreferred(String key) {
        return listChests.containsKey(key);
    }

    void setListPreferred(String key, boolean listPreferred) {
        if (isListPreferred(key) == listPreferred) {
            return;
        }
        if (listPreferred) {
            listChests.setProperty(key, "list");
        } else {
            listChests.remove(key);
        }
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "chestlistview-", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temporary)) {
                    listChests.store(writer, "Chest List View preferences. Unlisted chests use Grid.");
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
