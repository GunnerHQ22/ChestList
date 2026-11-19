package io.github.gunnerhq22.chestlistview.client;

enum ChestViewMode {
    GRID("grid"), LIST("list"), COMPACT("compact");

    final String id;

    ChestViewMode(String id) {
        this.id = id;
    }

    ChestViewMode next() {
        return switch (this) {
            case GRID -> LIST;
            case LIST -> COMPACT;
            case COMPACT -> GRID;
        };
    }

    static ChestViewMode fromId(String id) {
        if (LIST.id.equals(id)) {
            return LIST;
        }
        return COMPACT.id.equals(id) ? COMPACT : GRID;
    }
}
