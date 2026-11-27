package io.github.gunnerhq22.chestlistview.client;

import io.github.gunnerhq22.chestlistview.client.ChestListSettings.DetailType;
import io.github.gunnerhq22.chestlistview.client.ChestListSettings.TextSize;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

final class ChestListScreen extends ContainerScreen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int HEADER_HEIGHT = 17;
    private static final int INVENTORY_HEIGHT = 96;
    private static final int GRID_SLOT_PITCH = 18;
    private static final int FRAME_HEIGHT = HEADER_HEIGHT + INVENTORY_HEIGHT + 1;
    private static final int BASE_ROW_HEIGHT = 24;
    private static final int MIN_ROW_HEIGHT = 16;
    private static final int ITEM_SIZE = 16;
    private static final int MAX_VISIBLE_ROWS = 7;
    private static final int FRAME_SIDE_WIDTH = 7;
    private static final int ROW_LEFT = FRAME_SIDE_WIDTH;
    private static final int ROW_RIGHT = 160;
    private static final int ICON_X = 9;
    private static final int TEXT_X = 29;
    private static final int CONTROL_SIZE = 18;
    private static final int CONTROL_PITCH = 19;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final float HEADER_RANGE_SCALE = 0.8F;
    private static final int ROW_TEXT_RIGHT = ROW_RIGHT - 3;
    private static final int TOOLTIP_DEFAULT_COLOR = 0xFFFFFF;
    private static final int SETTINGS_BUTTON_HEIGHT = 16;
    private static final int SETTINGS_ROW_PITCH = 18;

    private final ChestViewport viewport;
    private final Map<Integer, CachedPreview> previews = new HashMap<>();
    private int previewModifiers;
    private final ChestViewPreference preference;
    private final String preferenceKey;
    private ChestViewMode preferredMode;
    private boolean listView;
    private boolean suppressToggleRelease;
    private final Button[] controls = new Button[5];
    private final Map<Integer, SearchEntry> searchEntries = new HashMap<>();
    private EditBox searchBox;
    private boolean searchOpen;
    private String query = "";
    private String[] searchTerms = {};
    private boolean draggingScrollbar;
    private double thumbGrabOffset;
    private int gridVerticalOffset;
    private int listHeight;
    private ChestListSettings settings;
    private boolean settingsOpen;
    private int settingsPage;
    private Button settingsPageButton;
    private int textSizeLabelY;
    private int rowCountLabelY;

    ChestListScreen(ChestMenu menu, Inventory inventory, Component title, ChestViewPreference preference, String preferenceKey) {
        super(menu, inventory, title);
        viewport = new ChestViewport(menu.getRowCount() * 9);
        this.preference = preference;
        this.preferenceKey = preferenceKey;
        preferredMode = preference == null ? ChestViewMode.GRID : preference.getMode(preferenceKey);
    }

    @Override
    protected void init() {
        int leftMargin = (width - imageWidth) / 2;
        boolean sideControls = leftMargin >= CONTROL_SIZE + 2;
        int toolbarInset = sideControls ? 0 : CONTROL_PITCH;
        int availableRows = (height - toolbarInset - FRAME_HEIGHT) / BASE_ROW_HEIGHT;
        boolean listAvailable = availableRows > 0;
        if (settings == null) {
            settings = ChestListSettings.load(minecraft.gameDirectory.toPath().resolve("config"));
        }
        listView = listAvailable && (settingsOpen || searchOpen || preferredMode != ChestViewMode.GRID);
        listHeight = Math.clamp(availableRows, 1, MAX_VISIBLE_ROWS) * BASE_ROW_HEIGHT;
        viewport.setRowLimit(Math.min(settings.rows(), listHeight / MIN_ROW_HEIGHT));
        refreshViewport();
        imageHeight = FRAME_HEIGHT + (listView ? listHeight() : gridHeight());
        inventoryLabelY = imageHeight - 94;
        super.init();
        gridVerticalOffset = Math.max(0, toolbarInset - topPos);
        topPos += gridVerticalOffset;

        // Anchor the controls to the tallest available view so changing modes cannot move them.
        int tallestHeight = FRAME_HEIGHT + Math.max(gridHeight(), listAvailable ? listHeight() : 0);
        int controlX = sideControls ? leftPos - CONTROL_SIZE - 2 : (width - controls.length * CONTROL_PITCH + 1) / 2;
        int lastControlOffset = (controls.length - 1) * CONTROL_PITCH;
        int anchorY = Math.max(1, (height - tallestHeight) / 2 + 1);
        int lowestControlY = Math.max(0, height - lastControlOffset - CONTROL_SIZE);
        int controlY = sideControls ? Math.min(anchorY + lastControlOffset, lowestControlY) : 0;
        ChestViewMode[] modes = ChestViewMode.values();
        for (int i = 0; i < modes.length; i++) {
            ChestViewMode mode = modes[i];
            controls[i] = addControl(controlX, controlY, sideControls, i, mode.id,
                    button -> selectMode(mode));
            controls[i].active = mode != preferredMode || (searchOpen && mode == ChestViewMode.GRID);
            controls[i].active &= mode == ChestViewMode.GRID || listAvailable;
        }
        controls[3] = addControl(controlX, controlY, sideControls, 3, "search", button -> toggleSearch());
        controls[3].active = listAvailable;

        controls[4] = addControl(controlX, controlY, sideControls, 4, "settings", button -> toggleSettings());
        controls[4].active = listAvailable;

        searchBox = null;
        settingsPageButton = null;
        textSizeLabelY = -1;
        rowCountLabelY = -1;
        if (settingsOpen && listView) {
            addSettingsControls();
        }
        if (!settingsOpen && searchOpen && listView) {
            var label = Component.translatable("chestlistview.view.search");
            int searchWidth = 108;
            searchBox = addRenderableWidget(new EditBox(font, leftPos + 7, topPos + 2, searchWidth, 13, label));
            searchBox.setMaxLength(120);
            searchBox.setHint(label);
            searchBox.setValue(query);
            searchBox.setResponder(this::updateSearch);
        }
    }

    private Button addControl(int x, int y, boolean side, int index, String id, Button.OnPress action) {
        return addRenderableWidget(Button.builder(Component.translatable("chestlistview.control." + id), action)
                .tooltip(Tooltip.create(Component.translatable("chestlistview.view." + id)))
                .bounds(x + (side ? 0 : index * CONTROL_PITCH), y + (side ? index * CONTROL_PITCH : 0),
                        CONTROL_SIZE, side ? CONTROL_SIZE : 15)
                .build());
    }

    private Button addSettingsButton(int x, int y, int width, Component label, Runnable action) {
        return addRenderableWidget(Button.builder(label, button -> action.run())
                .bounds(x, y, width, SETTINGS_BUTTON_HEIGHT).build());
    }

    private void addSettingsControls() {
        var types = DetailType.values();
        int totalRows = 1 + types.length;
        int rowsPerPage = listHeight() / SETTINGS_ROW_PITCH;
        int textSizeRows = Math.min(2, rowsPerPage);
        int pages = Math.ceilDiv(totalRows + textSizeRows, rowsPerPage);
        settingsPage = Math.min(settingsPage, pages - 1);
        if (pages > 1) {
            var page = Component.translatable("chestlistview.settings.page", settingsPage + 1, pages);
            settingsPageButton = addRenderableWidget(Button.builder(page, button -> {
                settingsPage = (settingsPage + 1) % pages;
                rebuildWidgets();
            }).bounds(leftPos + imageWidth - 52, topPos + 2, 44, 13).build());
        }

        int first = Math.max(0, settingsPage * rowsPerPage - textSizeRows);
        int end = Math.min(totalRows, (settingsPage + 1) * rowsPerPage - textSizeRows);
        int x = leftPos + 8;
        int y = topPos + settingsTop(end - first + (settingsPage == 0 ? textSizeRows : 0));
        if (settingsPage == 0) {
            addTextSizeControls(x, y, textSizeRows == 1);
            y += textSizeRows * SETTINGS_ROW_PITCH;
        }
        for (int row = first; row < end; row++, y += SETTINGS_ROW_PITCH) {
            if (row == 0) {
                rowCountLabelY = y - topPos + (SETTINGS_BUTTON_HEIGHT - font.lineHeight) / 2;
                addSettingsButton(x, y, 16, Component.literal("-"),
                        () -> changeLayout(settings.rows() - 1, settings.showNames())).active =
                        settings.rows() > ChestListSettings.MIN_ROWS;
                addSettingsButton(x + 62, y, 16, Component.literal("+"),
                        () -> changeLayout(settings.rows() + 1, settings.showNames())).active =
                        settings.rows() < ChestListSettings.MAX_ROWS;
                addSettingsButton(x + 83, y, imageWidth - 99, Component.translatable(settings.showNames()
                                ? "chestlistview.settings.names_on" : "chestlistview.settings.names_off"),
                        () -> changeLayout(settings.rows(), !settings.showNames()));
            } else {
                DetailType type = types[row - 1];
                var label = Component.translatable("chestlistview.settings.detail." + type.id);
                var message = Component.translatable(settings.hiddenDetails().contains(type)
                        ? "chestlistview.settings.detail_off" : "chestlistview.settings.detail_on", label);
                addSettingsButton(x, y, imageWidth - 16, message, () -> changeSettings(settings.toggle(type)));
            }
        }
    }

    private void addTextSizeControls(int x, int y, boolean compact) {
        int buttonHeight = compact ? 13 : SETTINGS_BUTTON_HEIGHT;
        int buttonY = y + (compact ? 7 : SETTINGS_ROW_PITCH);
        textSizeLabelY = buttonY - topPos - font.lineHeight - 1;
        TextSize[] sizes = TextSize.values();
        // Give the Default label extra room and keep it centered between the percentage buttons.
        int buttonWidth = (imageWidth - 16 - (sizes.length - 1) - 11) / sizes.length;
        for (TextSize size : sizes) {
            int width = buttonWidth + (size == TextSize.DEFAULT ? 11 : 0);
            Component label = Component.translatable("chestlistview.settings.text_size." + size.id);
            addRenderableWidget(Button.builder(label, button ->
                            changeSettings(new ChestListSettings(settings.rows(), settings.showNames(),
                                    settings.hiddenDetails(), size)))
                    .bounds(x, buttonY, width, buttonHeight).build()).active = size != settings.textSize();
            x += width + 1;
        }
    }

    private int settingsTop(int rows) {
        int controlsHeight = (rows - 1) * SETTINGS_ROW_PITCH + SETTINGS_BUTTON_HEIGHT;
        return HEADER_HEIGHT + (listHeight() - controlsHeight) / 2;
    }

    private void toggleSettings() {
        if (isQuickCrafting || draggingScrollbar) {
            return;
        }
        settingsOpen = !settingsOpen;
        settingsPage = 0;
        hoveredSlot = null;
        rebuildWidgets();
    }

    private void changeLayout(int rows, boolean showNames) {
        changeSettings(new ChestListSettings(rows, showNames, settings.hiddenDetails(), settings.textSize()));
    }

    private void changeSettings(ChestListSettings updated) {
        if (!settings.hiddenDetails().equals(updated.hiddenDetails())) {
            previews.clear();
        }
        settings = updated;
        settings.save(minecraft.gameDirectory.toPath().resolve("config"));
        rebuildWidgets();
    }

    private void selectMode(ChestViewMode mode) {
        if (isQuickCrafting || draggingScrollbar) {
            return;
        }
        settingsOpen = false;
        preferredMode = mode;
        if (mode == ChestViewMode.GRID) {
            searchOpen = false;
            updateSearch("");
        }
        if (preference != null) {
            preference.setMode(preferenceKey, mode);
        }
        hoveredSlot = null;
        rebuildWidgets();
    }

    private void toggleSearch() {
        if (isQuickCrafting || draggingScrollbar) {
            return;
        }
        settingsOpen = false;
        searchOpen = !searchOpen;
        if (!searchOpen) {
            updateSearch("");
        }
        hoveredSlot = null;
        rebuildWidgets();
        if (searchBox != null) {
            setInitialFocus(searchBox);
        }
    }

    private void updateSearch(String value) {
        query = value;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        searchTerms = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        refreshViewport();
        viewport.scroll(-viewport.firstEntry());
        hoveredSlot = null;
    }

    private void refreshViewport() {
        boolean compact = listView && preferredMode == ChestViewMode.COMPACT;
        boolean filtering = listView && searchTerms.length > 0 && minecraft != null && minecraft.player != null;
        viewport.refresh(index -> {
            ItemStack stack = menu.getSlot(index).getItem();
            if (!filtering) {
                return !compact || !stack.isEmpty();
            }
            if (stack.isEmpty()) {
                return false;
            }
            SearchEntry entry = searchEntries.get(index);
            if (entry == null || !ItemStack.matches(entry.stack(), stack)) {
                entry = new SearchEntry(stack.copy(), ChestSearch.searchableText(stack, minecraft.player));
                searchEntries.put(index, entry);
            }
            return ChestSearch.matches(entry.text(), searchTerms);
        });
    }

    private int listHeight() {
        return listHeight;
    }

    private int gridHeight() {
        return menu.getRowCount() * GRID_SLOT_PITCH;
    }

    private int inventoryOffset() {
        return listHeight() - gridHeight();
    }

    private int rowY(int row) {
        return HEADER_HEIGHT + row * listHeight() / viewport.rowLimit();
    }

    private int rowHeight(int row) {
        return rowY(row + 1) - rowY(row);
    }

    private int iconSize(int row) {
        return Math.min(ITEM_SIZE, rowHeight(row) - 3);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (listView && (preferredMode == ChestViewMode.COMPACT || searchTerms.length > 0)
                && !isQuickCrafting && !draggingScrollbar) {
            long window = minecraft.getWindow().getWindow();
            // Keep the displayed slot mapping stable between press and release.
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE
                    && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_RELEASE) {
                refreshViewport();
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (settingsOpen && listView) {
            return false;
        }
        if (!listView || width != 16 || height != 16) {
            return super.isHovering(x, y, width, height, mouseX, mouseY);
        }

        // Vanilla's private slot lookup calls this with each slot's original coordinates.
        int gridX = x - 8;
        int gridY = y - 18;
        if (gridX >= 0 && gridX <= 144 && gridX % GRID_SLOT_PITCH == 0
                && gridY >= 0 && gridY < gridHeight() && gridY % GRID_SLOT_PITCH == 0) {
            int row = viewport.visibleRow(gridY / GRID_SLOT_PITCH * 9 + gridX / GRID_SLOT_PITCH);
            return row >= 0 && mouseX >= leftPos + ROW_LEFT && mouseX < leftPos + ROW_RIGHT
                    && mouseY >= topPos + rowY(row) && mouseY < topPos + rowY(row + 1);
        }
        return super.isHovering(x, y + inventoryOffset(), width, height, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (!listView) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, gridVerticalOffset, 0);
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            pose.popPose();
            return;
        }

        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, HEADER_HEIGHT);
        graphics.blit(BACKGROUND, leftPos, topPos + HEADER_HEIGHT + listHeight(),
                0, 126, imageWidth, INVENTORY_HEIGHT);
        int listTop = topPos + HEADER_HEIGHT;
        int rightFrameX = imageWidth - FRAME_SIDE_WIDTH;
        graphics.fill(leftPos + FRAME_SIDE_WIDTH, listTop, leftPos + rightFrameX,
                listTop + listHeight(), 0xFF47434C);
        for (int offset = 0; offset < listHeight(); offset += gridHeight()) {
            int stripHeight = Math.min(gridHeight(), listHeight() - offset);
            graphics.blit(BACKGROUND, leftPos, listTop + offset,
                    0, HEADER_HEIGHT, FRAME_SIDE_WIDTH, stripHeight);
            graphics.blit(BACKGROUND, leftPos + rightFrameX, listTop + offset,
                    rightFrameX, HEADER_HEIGHT, FRAME_SIDE_WIDTH, stripHeight);
        }
        if (settingsOpen) {
            return;
        }
        for (int row = 0; row < viewport.rowCount(); row++) {
            int y = topPos + rowY(row);
            boolean hovered = mouseX >= leftPos + ROW_LEFT && mouseX < leftPos + ROW_RIGHT
                    && mouseY >= y && mouseY < y + rowHeight(row);
            graphics.fill(leftPos + ROW_LEFT, y, leftPos + ROW_RIGHT, y + rowHeight(row) - 1,
                    hovered ? 0xFF38343F : 0xFF29262E);
            int size = iconSize(row);
            int iconTop = y + (rowHeight(row) - size) / 2;
            graphics.fill(leftPos + ICON_X - 1, iconTop - 1, leftPos + ICON_X + size + 1,
                    iconTop + size + 1, 0xFF8B8B8B);
        }

        if (viewport.maxOffset() == 0) {
            return;
        }
        int trackX = scrollbarX();
        int trackY = topPos + HEADER_HEIGHT;
        int thumbHeight = thumbHeight();
        int thumbY = thumbY(thumbHeight);
        boolean hovered = isOverScrollbar(mouseX, mouseY) && !isQuickCrafting;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + listHeight(),
                hovered || draggingScrollbar ? 0xFFA5A5A5 : 0xFF8B8B8B);
        graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                hovered || draggingScrollbar ? 0xFFFFFFFF : 0xFFE2E2E2);
    }

    private int scrollbarX() {
        return leftPos + ROW_RIGHT + 3;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return listView && !settingsOpen && viewport.maxOffset() > 0
                && mouseX >= leftPos + ROW_RIGHT && mouseX < leftPos + imageWidth
                && mouseY >= topPos + HEADER_HEIGHT && mouseY < topPos + HEADER_HEIGHT + listHeight();
    }

    private int thumbHeight() {
        return Math.max(8, listHeight() * viewport.rowCount() / viewport.entryCount());
    }

    private int thumbY(int thumbHeight) {
        return topPos + HEADER_HEIGHT + (int) Math.round(
                (listHeight() - thumbHeight) * (double) viewport.firstEntry() / viewport.maxOffset());
    }

    private void dragScrollbar(double mouseY) {
        int travel = listHeight() - thumbHeight();
        double fraction = (mouseY - topPos - HEADER_HEIGHT - thumbGrabOffset) / travel;
        int entry = (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * viewport.maxOffset());
        viewport.scroll(entry - viewport.firstEntry());
        hoveredSlot = null;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!listView) {
            super.renderSlot(graphics, slot);
            return;
        }

        int row = viewport.visibleRow(slot.index);
        if (viewport.contains(slot.index) && (row < 0 || settingsOpen)) {
            return;
        }

        var pose = graphics.pose();
        pose.pushPose();
        if (row >= 0) {
            int size = iconSize(row);
            float scale = (float) size / ITEM_SIZE;
            pose.translate(ICON_X, rowY(row) + (rowHeight(row) - size) / 2, 0);
            pose.scale(scale, scale, 1);
            pose.translate(-slot.x, -slot.y, 0);
        } else {
            pose.translate(0, inventoryOffset(), 0);
        }
        super.renderSlot(graphics, slot);
        graphics.pose().popPose();
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (!listView) {
            super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
        } else if (!viewport.contains(slot.index) && slot.isHighlightable()) {
            renderSlotHighlight(graphics, slot.x, slot.y + inventoryOffset(), 0, getSlotColor(slot.index));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleRight = imageWidth - 7;
        if (listView && !settingsOpen) {
            Component range = viewport.entryCount() == 0 ? Component.translatable("chestlistview.view.empty_range")
                    : Component.translatable("chestlistview.view.slot_range",
                    viewport.firstEntry() + 1, viewport.firstEntry() + viewport.rowCount(), viewport.entryCount());
            float rangeX = titleRight - font.width(range) * HEADER_RANGE_SCALE;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(rangeX, titleLabelY + (1 - HEADER_RANGE_SCALE) * font.lineHeight / 2, 0);
            pose.scale(HEADER_RANGE_SCALE, HEADER_RANGE_SCALE, 1);
            if (searchBox == null || rangeX >= searchBox.getX() - leftPos + searchBox.getWidth() + 3) {
                graphics.drawString(font, range, 0, 0, 0x404040, false);
            }
            pose.popPose();
            titleRight = (int) rangeX - 5;
        }
        if (settingsPageButton != null) {
            titleRight = settingsPageButton.getX() - leftPos - 5;
        }
        int titleWidth = titleRight - titleLabelX;
        Component displayedTitle = settingsOpen && listView ? Component.translatable("chestlistview.view.settings") : title;
        if (searchBox == null && font.width(displayedTitle) <= titleWidth) {
            graphics.drawString(font, displayedTitle, titleLabelX, titleLabelY, 0x404040, false);
        } else if (searchBox == null && titleWidth >= font.width("\u2026")) {
            graphics.drawString(font, ellipsize(displayedTitle.getString(), titleWidth),
                    titleLabelX, titleLabelY, 0x404040, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        if (!listView) {
            return;
        }

        if (settingsOpen) {
            if (textSizeLabelY >= 0) {
                graphics.drawCenteredString(font, Component.translatable("chestlistview.settings.text_size"),
                        imageWidth / 2, textSizeLabelY, TOOLTIP_DEFAULT_COLOR);
            }
            if (rowCountLabelY >= 0) {
                Component rows = Component.translatable("chestlistview.settings.rows", settings.rows());
                graphics.drawCenteredString(font, rows, 47, rowCountLabelY, TOOLTIP_DEFAULT_COLOR);
            }
            return;
        }

        if (viewport.entryCount() == 0) {
            graphics.drawCenteredString(font, Component.translatable(searchTerms.length == 0 ? "chestlistview.view.empty" : "chestlistview.view.no_results"),
                    imageWidth / 2, HEADER_HEIGHT + (listHeight() - font.lineHeight) / 2, 0xFFFFFF);
            return;
        }

        var player = minecraft.player;
        if (player == null) {
            return;
        }
        var tooltipContext = Item.TooltipContext.of(player.level());
        int modifiers = (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0) | (hasAltDown() ? 4 : 0);
        if (previewModifiers != modifiers) {
            previews.clear();
            previewModifiers = modifiers;
        }
        float textScale = settings.textSize().scale;
        float lineHeight = font.lineHeight * textScale;
        int availableLines = Math.max(1, (int) ((listHeight() / viewport.rowLimit() - 1) / lineHeight));
        int textWidth = (int) ((ROW_TEXT_RIGHT - TEXT_X) / textScale);
        for (int row = 0; row < viewport.rowCount(); row++) {
            Slot slot = menu.getSlot(viewport.slotAt(row));
            var stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            CachedPreview preview = previews.get(slot.index);
            if (preview == null || !ItemStack.matches(preview.stack(), stack)) {
                preview = new CachedPreview(stack.copy(),
                        ChestItemText.details(stack, tooltipContext, player, settings.hiddenDetails()));
                previews.put(slot.index, preview);
            }
            var details = preview.details();
            boolean showName = settings.showNames() || details.isEmpty();
            int nameLines = showName ? 1 : 0;
            int detailLines = Math.min(details.size(), availableLines - nameLines);
            float textY = rowY(row) + (rowHeight(row) - (nameLines + detailLines) * lineHeight) / 2;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(TEXT_X, textY, 0);
            pose.scale(textScale, textScale, 1);

            if (showName) {
                graphics.drawString(font, ellipsize(stack.getHoverName().getString(), textWidth),
                        0, 0, 0xFFFFFF, false);
            }

            for (int line = 0; line < detailLines; line++) {
                int lineY = (line + nameLines) * font.lineHeight;
                Component detail = details.get(line);
                if (font.width(detail) > textWidth || detail.getString().contains("\n")) {
                    String ellipsis = "\u2026";
                    var wrapped = font.split(detail, textWidth - font.width(ellipsis));
                    graphics.drawString(font, wrapped.getFirst(), 0, lineY, TOOLTIP_DEFAULT_COLOR, false);
                    graphics.drawString(font, ellipsis, font.width(wrapped.getFirst()), lineY,
                            TOOLTIP_DEFAULT_COLOR, false);
                } else {
                    graphics.drawString(font, Language.getInstance().getVisualOrder(detail),
                            0, lineY, TOOLTIP_DEFAULT_COLOR, false);
                }
            }
            pose.popPose();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        previews.clear();
    }

    private record SearchEntry(ItemStack stack, String text) {}

    private record CachedPreview(ItemStack stack, List<Component> details) {}

    private String ellipsize(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "\u2026";
        return font.plainSubstrByWidth(text, maxWidth - font.width(ellipsis)) + ellipsis;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (settingsOpen && listView) {
            return true;
        }
        if (listView && scrollY != 0 && mouseX >= leftPos + ROW_LEFT && mouseX < leftPos + imageWidth
                && mouseY >= topPos + HEADER_HEIGHT && mouseY < topPos + HEADER_HEIGHT + listHeight()) {
            if (viewport.maxOffset() > 0 && !isQuickCrafting && !suppressToggleRelease && !draggingScrollbar) {
                viewport.scroll(scrollY > 0 ? -1 : 1);
                updateHoveredSlot(mouseX, mouseY);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (var child : children()) {
                if (child instanceof Button control && control.isMouseOver(mouseX, mouseY)) {
                    suppressToggleRelease = true;
                    control.onPress();
                    return true;
                }
            }
            if (searchBox != null && searchBox.isMouseOver(mouseX, mouseY)) {
                if (!isQuickCrafting) {
                    setInitialFocus(searchBox);
                    searchBox.mouseClicked(mouseX, mouseY, button);
                    suppressToggleRelease = true;
                }
                return true;
            }
            if (isOverScrollbar(mouseX, mouseY)) {
                if (!isQuickCrafting) {
                    int thumbHeight = thumbHeight();
                    int thumbY = thumbY(thumbHeight);
                    draggingScrollbar = true;
                    thumbGrabOffset = mouseY >= thumbY && mouseY < thumbY + thumbHeight
                            ? mouseY - thumbY : thumbHeight / 2.0;
                    dragScrollbar(mouseY);
                }
                return true;
            }
        }
        if (draggingScrollbar || (settingsOpen && listView)) {
            return true;
        }
        if (searchBox != null && searchBox.isFocused()) {
            setFocused(null);
        }
        updateHoveredSlot(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar || suppressToggleRelease || (settingsOpen && listView)) {
            if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                dragScrollbar(mouseY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar || (settingsOpen && listView)
                || (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && suppressToggleRelease)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                draggingScrollbar = false;
                suppressToggleRelease = false;
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        for (Button control : controls) {
            if (control != null && mouseX >= control.getX() && mouseX < control.getX() + control.getWidth()
                    && mouseY >= control.getY() && mouseY < control.getY() + control.getHeight()) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsOpen && listView) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                toggleSettings();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                toggleSearch();
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_TAB) {
                setFocused(null);
            } else {
                searchBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (draggingScrollbar) {
            return true;
        }
        var window = minecraft.getWindow();
        updateHoveredSlot(minecraft.mouseHandler.xpos() * width / window.getScreenWidth(),
                minecraft.mouseHandler.ypos() * height / window.getScreenHeight());
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateHoveredSlot(double mouseX, double mouseY) {
        hoveredSlot = null;
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                hoveredSlot = slot;
                return;
            }
        }
    }
}
