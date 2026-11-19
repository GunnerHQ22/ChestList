package io.github.gunnerhq22.chestlistview.client;

import java.util.Arrays;
import java.util.function.IntPredicate;

/** Maps displayed entries to their original menu slots in physical slot order. */
final class ChestViewport {
    private final int[] displayedSlots;
    private int entryCount;
    private int firstEntry;
    private int rowLimit = 7;

    ChestViewport(int slotCount) {
        displayedSlots = new int[slotCount];
        refresh(slot -> true);
    }

    void refresh(IntPredicate shouldDisplay) {
        int anchor = entryCount == 0 ? 0 : displayedSlots[firstEntry];
        entryCount = 0;
        for (int slot = 0; slot < displayedSlots.length; slot++) {
            if (shouldDisplay.test(slot)) {
                displayedSlots[entryCount++] = slot;
            }
        }
        int entry = Arrays.binarySearch(displayedSlots, 0, entryCount, anchor);
        firstEntry = Math.min(entry >= 0 ? entry : -entry - 1, maxOffset());
    }

    boolean contains(int slotId) {
        return slotId < displayedSlots.length;
    }

    int slotAt(int row) {
        return displayedSlots[firstEntry + row];
    }

    int visibleRow(int slotId) {
        int entry = Arrays.binarySearch(displayedSlots, firstEntry, firstEntry + rowCount(), slotId);
        return entry >= 0 ? entry - firstEntry : -1;
    }

    void setRowLimit(int rows) {
        rowLimit = rows;
        scroll(0);
    }

    void scroll(int delta) {
        firstEntry = Math.clamp(firstEntry + delta, 0, maxOffset());
    }

    int rowLimit() {
        return rowLimit;
    }

    int rowCount() {
        return Math.min(rowLimit, entryCount);
    }

    int firstEntry() {
        return firstEntry;
    }

    int entryCount() {
        return entryCount;
    }

    int maxOffset() {
        return Math.max(0, entryCount - rowLimit);
    }
}
