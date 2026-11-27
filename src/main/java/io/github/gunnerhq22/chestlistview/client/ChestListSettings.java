package io.github.gunnerhq22.chestlistview.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

record ChestListSettings(int rows, boolean showNames, Set<DetailType> hiddenDetails, TextSize textSize) {
    static final int MIN_ROWS = 4;
    static final int MAX_ROWS = 10;
    private static final int DEFAULT_ROWS = 7;
    private static final String FILE_NAME = "chestlistview-layout.properties";

    ChestListSettings {
        hiddenDetails = Set.copyOf(hiddenDetails);
    }

    ChestListSettings(int rows, boolean showNames) {
        this(rows, showNames, Set.of(), TextSize.DEFAULT);
    }

    ChestListSettings toggle(DetailType type) {
        var hidden = EnumSet.noneOf(DetailType.class);
        hidden.addAll(hiddenDetails);
        if (!hidden.remove(type)) {
            hidden.add(type);
        }
        return new ChestListSettings(rows, showNames, hidden, textSize);
    }

    static ChestListSettings load(Path configDirectory) {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(configDirectory.resolve(FILE_NAME))) {
            properties.load(reader);
        } catch (NoSuchFileException ignored) {
            return new ChestListSettings(DEFAULT_ROWS, true);
        } catch (IOException | IllegalArgumentException exception) {
            LogUtils.getLogger().warn("Could not read Chest List View layout settings", exception);
            return new ChestListSettings(DEFAULT_ROWS, true);
        }
        int rows = DEFAULT_ROWS;
        try {
            rows = Math.clamp(Integer.parseInt(properties.getProperty("rows", "7")), MIN_ROWS, MAX_ROWS);
        } catch (NumberFormatException ignored) {
            // Keep the default for an invalid row count.
        }
        var hidden = EnumSet.noneOf(DetailType.class);
        for (DetailType type : DetailType.values()) {
            if ("false".equalsIgnoreCase(properties.getProperty(type.propertyKey()))) {
                hidden.add(type);
            }
        }
        return new ChestListSettings(rows, !"false".equalsIgnoreCase(properties.getProperty("showNames")), hidden,
                TextSize.fromId(properties.getProperty("textSize")));
    }

    void save(Path configDirectory) {
        var properties = new Properties();
        properties.setProperty("rows", Integer.toString(rows));
        properties.setProperty("showNames", Boolean.toString(showNames));
        properties.setProperty("textSize", textSize.id);
        for (DetailType type : DetailType.values()) {
            properties.setProperty(type.propertyKey(), Boolean.toString(!hiddenDetails.contains(type)));
        }
        try {
            Files.createDirectories(configDirectory);
            Path temporary = Files.createTempFile(configDirectory, "chestlistview-layout-", ".tmp");
            Path target = configDirectory.resolve(FILE_NAME);
            try {
                try (var writer = Files.newBufferedWriter(temporary)) {
                    properties.store(writer, "Chest List View layout settings");
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not save Chest List View layout settings", exception);
        }
    }

    enum TextSize {
        SMALLEST("minus_25", 0.60F),
        SMALL("minus_10", 0.72F),
        DEFAULT("default", 0.80F),
        LARGE("plus_10", 0.88F),
        LARGEST("plus_20", 0.96F);

        final String id;
        final float scale;

        TextSize(String id, float scale) {
            this.id = id;
            this.scale = scale;
        }

        private static TextSize fromId(String id) {
            for (TextSize size : values()) {
                if (size.id.equals(id)) {
                    return size;
                }
            }
            return DEFAULT;
        }
    }

    enum DetailType {
        ENCHANTMENT("enchantments"),
        DURABILITY("durability"),
        STATE("storage_status"),
        POTION("potion_effects"),
        ATTRIBUTE("attributes"),
        OTHER("other");

        final String id;

        DetailType(String id) {
            this.id = id;
        }

        private String propertyKey() {
            return "details." + id;
        }
    }
}
