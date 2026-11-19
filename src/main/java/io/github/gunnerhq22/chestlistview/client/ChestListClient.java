package io.github.gunnerhq22.chestlistview.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ChestListClient {
    private static final long OPEN_TIMEOUT_NANOS = 2_000_000_000L;

    private static ChestViewPreference preferences;
    private static ChestInteraction pendingInteraction;

    private ChestListClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ChestListClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ChestListClient::onScreenOpening);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        pendingInteraction = null;
        if (event.isCanceled() || Minecraft.getInstance().player != event.getEntity()
                || event.getEntity().isSecondaryUseActive()) {
            return;
        }

        BlockPos pos = event.getPos();
        if (chestSize(level, pos) != 0) {
            pendingInteraction = new ChestInteraction(level, pos.immutable(), System.nanoTime());
        }
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        ChestInteraction interaction = pendingInteraction;
        pendingInteraction = null;
        if (event.isCanceled() || !(event.getNewScreen() instanceof ContainerScreen screen)
                || screen.getClass() != ContainerScreen.class) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.touchscreen().get()) {
            return;
        }
        var type = screen.getMenu().getType();
        int clickedChestSize = interaction != null && interaction.level() == minecraft.level
                && System.nanoTime() - interaction.clickedAt() <= OPEN_TIMEOUT_NANOS
                ? chestSize(interaction.level(), interaction.pos()) : 0;
        boolean single = type == MenuType.GENERIC_9x3 && screen.getMenu().getRowCount() == 3;
        boolean doubleChest = type == MenuType.GENERIC_9x6 && screen.getMenu().getRowCount() == 6
                && clickedChestSize == 54;
        if (!single && !doubleChest) {
            return;
        }

        String key = null;
        if (clickedChestSize == (single ? 27 : 54)) {
            key = preferenceKey(minecraft, interaction, screen);
        }
        if (key != null && preferences == null) {
            preferences = new ChestViewPreference(minecraft.gameDirectory.toPath());
        }
        event.setNewScreen(new ChestListScreen(screen.getMenu(), minecraft.player.getInventory(),
                screen.getTitle(), key == null ? null : preferences, key));
    }

    private static int chestSize(ClientLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST)) {
            return 0;
        }
        return state.getValue(ChestBlock.TYPE) == ChestType.SINGLE ? 27 : 54;
    }

    private static String preferenceKey(Minecraft minecraft, ChestInteraction interaction, ContainerScreen screen) {
        ClientLevel level = interaction.level();
        BlockPos pos = interaction.pos();
        var state = level.getBlockState(pos);
        ChestType type = state.getValue(ChestBlock.TYPE);
        String defaultTitle = type == ChestType.SINGLE ? "container.chest" : "container.chestDouble";
        if (!screen.getTitle().equals(net.minecraft.network.chat.Component.translatable(defaultTitle))) {
            // A custom title can belong to a server GUI, so leave its preference on the open screen.
            return null;
        }
        if (type != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            var otherState = level.getBlockState(other);
            if (!otherState.is(state.getBlock())
                    || otherState.getValue(ChestBlock.TYPE) != type.getOpposite()
                    || otherState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
                    || !other.relative(ChestBlock.getConnectedDirection(otherState)).equals(pos)) {
                return null;
            }
            if (other.compareTo(pos) < 0) {
                pos = other;
            }
        }

        String scope;
        if (minecraft.getSingleplayerServer() != null) {
            var saves = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
            var world = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize();
            scope = "world:" + (world.startsWith(saves) ? saves.relativize(world) : world);
        } else if (minecraft.getCurrentServer() != null) {
            scope = "server:" + minecraft.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT);
        } else {
            return null;
        }
        return scope + "|" + level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record ChestInteraction(ClientLevel level, BlockPos pos, long clickedAt) {}
}
