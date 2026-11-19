package io.github.gunnerhq22.chestlistview.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_VISIBLE_ROWS = 7;
    private static final int FRAME_SIDE_WIDTH = 7;
    private static final int ROW_LEFT = FRAME_SIDE_WIDTH;
    private static final int ROW_RIGHT = 160;
    private static final int ICON_X = 9;
    private static final int TEXT_X = 29;
    private static final int BUTTON_WIDTH = 35;
    private static final float ROW_TEXT_SCALE = 0.8F;
    private static final int ROW_TEXT_RIGHT = ROW_RIGHT - 3;
    private static final int TOOLTIP_DEFAULT_COLOR = 0xFFFFFF;

    private final ChestViewport viewport;
    private final Map<Integer, CachedPreview> previews = new HashMap<>();
    private int previewModifiers;
    private final ChestViewPreference preference;
    private final String preferenceKey;
    private ChestViewMode preferredMode;
    private boolean listView;
    private boolean suppressToggleRelease;
    private Button toggle;

    ChestListScreen(ChestMenu menu, Inventory inventory, Component title, ChestViewPreference preference, String preferenceKey) {
        super(menu, inventory, title);
        viewport = new ChestViewport(menu.getRowCount() * 9);
        this.preference = preference;
        this.preferenceKey = preferenceKey;
        preferredMode = preference == null ? ChestViewMode.GRID : preference.getMode(preferenceKey);
    }

    @Override
    protected void init() {
        int availableRows = (height - FRAME_HEIGHT) / ROW_HEIGHT;
        boolean listAvailable = availableRows > 0;
        listView = listAvailable && preferredMode != ChestViewMode.GRID;
        viewport.setRowLimit(Math.clamp(availableRows, 1, MAX_VISIBLE_ROWS));
        boolean compact = listView && preferredMode == ChestViewMode.COMPACT;
        viewport.refresh(slot -> !compact || menu.getSlot(slot).hasItem());
        imageHeight = FRAME_HEIGHT + (listView ? listHeight() : gridHeight());
        inventoryLabelY = imageHeight - 94;
        super.init();
        toggle = addRenderableWidget(Button.builder(
                        Component.translatable("chestlistview.view." + preferredMode.next().id),
                        button -> toggleView())
                .bounds(leftPos + imageWidth - BUTTON_WIDTH - 6, topPos + 3, BUTTON_WIDTH, 13)
                .build());
        if (preferredMode.next() == ChestViewMode.COMPACT) {
            toggle.setTooltip(Tooltip.create(Component.translatable("chestlistview.view.compact_hint")));
        }
        toggle.active = listAvailable;
    }

    private void toggleView() {
        if (isQuickCrafting) {
            return;
        }
        preferredMode = preferredMode.next();
        if (preference != null) {
            preference.setMode(preferenceKey, preferredMode);
        }
        hoveredSlot = null;
        rebuildWidgets();
        setInitialFocus(toggle);
    }

    private int listHeight() {
        return viewport.rowLimit() * ROW_HEIGHT;
    }

    private int gridHeight() {
        return menu.getRowCount() * GRID_SLOT_PITCH;
    }

    private int inventoryOffset() {
        return listHeight() - gridHeight();
    }

    private int rowY(int row) {
        return HEADER_HEIGHT + row * ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (listView && preferredMode == ChestViewMode.COMPACT && !isQuickCrafting) {
            long window = minecraft.getWindow().getWindow();
            // Keep the displayed slot mapping stable between press and release.
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE
                    && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_RELEASE) {
                viewport.refresh(slot -> menu.getSlot(slot).hasItem());
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
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
            super.renderBg(graphics, partialTick, mouseX, mouseY);
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
        for (int row = 0; row < viewport.rowCount(); row++) {
            int y = topPos + rowY(row);
            boolean hovered = mouseX >= leftPos + ROW_LEFT && mouseX < leftPos + ROW_RIGHT
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(leftPos + ROW_LEFT, y, leftPos + ROW_RIGHT, y + ROW_HEIGHT - 1,
                    hovered ? 0xFF38343F : 0xFF29262E);
            graphics.fill(leftPos + ICON_X - 1, y + 3, leftPos + ICON_X + 17, y + 21, 0xFF8B8B8B);
        }

        if (viewport.maxOffset() == 0) {
            return;
        }
        int trackX = leftPos + ROW_RIGHT + 3;
        int trackY = topPos + HEADER_HEIGHT;
        int thumbHeight = Math.max(8, listHeight() * viewport.rowCount() / viewport.entryCount());
        int thumbY = trackY + (listHeight() - thumbHeight) * viewport.firstEntry() / viewport.maxOffset();
        graphics.fill(trackX, trackY, trackX + 6, trackY + listHeight(), 0xFF8B8B8B);
        graphics.fill(trackX + 1, thumbY, trackX + 5, thumbY + thumbHeight, 0xFFE2E2E2);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!listView) {
            super.renderSlot(graphics, slot);
            return;
        }

        int row = viewport.visibleRow(slot.index);
        if (viewport.contains(slot.index) && row < 0) {
            return;
        }

        int dx = row >= 0 ? ICON_X - slot.x : 0;
        int dy = row >= 0 ? rowY(row) + 4 - slot.y : inventoryOffset();
        graphics.pose().pushPose();
        graphics.pose().translate(dx, dy, 0);
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
        if (toggle == null) {
            super.renderLabels(graphics, mouseX, mouseY);
            return;
        }
        int titleRight = toggle.getX() - leftPos - 4;
        if (listView) {
            Component range = viewport.entryCount() == 0 ? Component.translatable("chestlistview.view.empty_range")
                    : Component.translatable("chestlistview.view.slot_range",
                    viewport.firstEntry() + 1, viewport.firstEntry() + viewport.rowCount(), viewport.entryCount());
            float rangeX = titleRight - font.width(range) * ROW_TEXT_SCALE;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(rangeX, titleLabelY + (1 - ROW_TEXT_SCALE) * font.lineHeight / 2, 0);
            pose.scale(ROW_TEXT_SCALE, ROW_TEXT_SCALE, 1);
            graphics.drawString(font, range, 0, 0, 0x404040, false);
            pose.popPose();
            titleRight = (int) rangeX - 5;
        }
        int titleWidth = titleRight - titleLabelX;
        if (font.width(title) <= titleWidth) {
            graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        } else if (titleWidth >= font.width("\u2026")) {
            graphics.drawString(font, ellipsize(title.getString(), titleWidth),
                    titleLabelX, titleLabelY, 0x404040, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        if (!listView) {
            return;
        }

        if (viewport.entryCount() == 0) {
            graphics.drawCenteredString(font, Component.translatable("chestlistview.view.empty"),
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
        for (int row = 0; row < viewport.rowCount(); row++) {
            Slot slot = menu.getSlot(viewport.slotAt(row));
            var stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            CachedPreview preview = previews.get(slot.index);
            if (preview == null || !ItemStack.matches(preview.stack(), stack)) {
                preview = new CachedPreview(stack.copy(), ChestItemText.details(stack, tooltipContext, player));
                previews.put(slot.index, preview);
            }
            var details = preview.details();
            boolean hasDetails = !details.isEmpty();
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(TEXT_X, rowY(row) + (hasDetails ? 2 : (ROW_HEIGHT - font.lineHeight * ROW_TEXT_SCALE) / 2), 0);
            pose.scale(ROW_TEXT_SCALE, ROW_TEXT_SCALE, 1);

            int textWidth = (int) ((ROW_TEXT_RIGHT - TEXT_X) / ROW_TEXT_SCALE);
            graphics.drawString(font, ellipsize(stack.getHoverName().getString(), textWidth),
                    0, 0, 0xFFFFFF, false);

            for (int line = 0; line < Math.min(2, details.size()); line++) {
                Component detail = details.get(line);
                if (font.width(detail) > textWidth || detail.getString().contains("\n")) {
                    String ellipsis = "\u2026";
                    var wrapped = font.split(detail, textWidth - font.width(ellipsis));
                    graphics.drawString(font, wrapped.getFirst(), 0, 9 + line * 9, TOOLTIP_DEFAULT_COLOR, false);
                    graphics.drawString(font, ellipsis, font.width(wrapped.getFirst()), 9 + line * 9,
                            TOOLTIP_DEFAULT_COLOR, false);
                } else {
                    graphics.drawString(font, Language.getInstance().getVisualOrder(detail),
                            0, 9 + line * 9, TOOLTIP_DEFAULT_COLOR, false);
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
        if (listView && scrollY != 0 && mouseX >= leftPos + ROW_LEFT && mouseX < leftPos + imageWidth
                && mouseY >= topPos + HEADER_HEIGHT && mouseY < topPos + HEADER_HEIGHT + listHeight()) {
            if (viewport.maxOffset() > 0 && !isQuickCrafting && !suppressToggleRelease) {
                viewport.scroll(scrollY > 0 ? -1 : 1);
                updateHoveredSlot(mouseX, mouseY);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && toggle != null && toggle.isMouseOver(mouseX, mouseY)) {
            suppressToggleRelease = true;
            if (!isQuickCrafting) {
                toggleView();
            }
            return true;
        }
        updateHoveredSlot(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && suppressToggleRelease) {
            suppressToggleRelease = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
