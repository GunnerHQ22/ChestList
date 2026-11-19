package io.github.gunnerhq22.chestlistview.client;

import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/** Maps between viewport rows and the original chest menu slots. */
final class ChestViewport {
    private final ChestMenu menu;
    private final int chestSlots;
    private int firstSlot;
    private int rowCount = 7;

    ChestViewport(ChestMenu menu) {
        this.menu = menu;
        this.chestSlots = menu.getRowCount() * 9;
    }

    boolean contains(Slot slot) {
        return slot.index < chestSlots;
    }

    Slot slotAt(int row) {
        return menu.getSlot(firstSlot + row);
    }

    int visibleRow(int slotId) {
        int row = slotId - firstSlot;
        return slotId < chestSlots && row >= 0 && row < rowCount ? row : -1;
    }

    void setRowCount(int rows) {
        rowCount = rows;
        scroll(0);
    }

    void scroll(int delta) {
        firstSlot = Math.clamp(firstSlot + delta, 0, maxOffset());
    }

    int rowCount() {
        return rowCount;
    }

    int firstSlot() {
        return firstSlot;
    }

    int slotCount() {
        return chestSlots;
    }

    int maxOffset() {
        return chestSlots - rowCount;
    }
}
