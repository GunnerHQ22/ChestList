package io.github.gunnerhq22.chestlistview.client;

enum ChestViewMode {
    GRID("grid"), LIST("list"), COMPACT("compact");

    final String id;

    ChestViewMode(String id) {
        this.id = id;
    }

    static ChestViewMode fromId(String id) {
        if (LIST.id.equals(id)) {
            return LIST;
        }
        return COMPACT.id.equals(id) ? COMPACT : GRID;
    }
}
