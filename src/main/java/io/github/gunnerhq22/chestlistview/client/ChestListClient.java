package io.github.gunnerhq22.chestlistview.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ChestListClient {
    private static final long OPEN_TIMEOUT_NANOS = 2_000_000_000L;
    private static final int PREFERENCE_CHECK_INTERVAL_TICKS = 30 * 20;

    private static ChestViewPreference preferences;
    private static ChestInteraction pendingInteraction;
    private static ClientLevel checkedLevel;
    private static int ticksUntilCheck;

    private ChestListClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ChestListClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ChestListClient::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ChestListClient::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ChestListClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ChestListClient::onClientTick);
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
        var state = level.getBlockState(pos);
        if (StationaryStorage.isCandidate(level, pos, state)) {
            pendingInteraction = new ChestInteraction(level, pos.immutable(), state.getBlock(), System.nanoTime());
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        clearPendingOnEntityInteraction(event);
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        clearPendingOnEntityInteraction(event);
    }

    private static void clearPendingOnEntityInteraction(PlayerInteractEvent event) {
        if (event.getLevel() instanceof ClientLevel && Minecraft.getInstance().player == event.getEntity()) {
            pendingInteraction = null;
        }
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        ChestInteraction interaction = pendingInteraction;
        pendingInteraction = null;
        if (event.isCanceled() || !(event.getNewScreen() instanceof ContainerScreen screen)
                || screen.getClass() != ContainerScreen.class
                || !isStandardChestMenu(screen.getMenu())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.touchscreen().get()) {
            return;
        }
        String key = preferenceKey(minecraft, interaction, screen);
        if (key != null) {
            preferences(minecraft).migrateChestKey(key);
        }
        event.setNewScreen(new ChestListScreen(screen.getMenu(), minecraft.player.getInventory(),
                screen.getTitle(), key == null ? null : preferences, key));
    }

    private static boolean isStandardChestMenu(ChestMenu menu) {
        if (menu.getClass() != ChestMenu.class) {
            return false;
        }
        var type = menu.getType();
        return switch (menu.getRowCount()) {
            case 1 -> type == MenuType.GENERIC_9x1;
            case 2 -> type == MenuType.GENERIC_9x2;
            case 3 -> type == MenuType.GENERIC_9x3;
            case 4 -> type == MenuType.GENERIC_9x4;
            case 5 -> type == MenuType.GENERIC_9x5;
            case 6 -> type == MenuType.GENERIC_9x6;
            default -> false;
        };
    }

    private static ChestViewPreference preferences(Minecraft minecraft) {
        if (preferences == null) {
            preferences = new ChestViewPreference(minecraft.gameDirectory.toPath());
        }
        return preferences;
    }

    private static String preferenceKey(Minecraft minecraft, ChestInteraction interaction, ContainerScreen screen) {
        if (interaction == null || interaction.level() != minecraft.level
                || System.nanoTime() - interaction.clickedAt() > OPEN_TIMEOUT_NANOS
                || minecraft.hitResult instanceof EntityHitResult) {
            return null;
        }
        ClientLevel level = interaction.level();
        BlockState state = level.getBlockState(interaction.pos());
        if (!state.is(interaction.block())) {
            return null;
        }
        var location = StationaryStorage.resolve(level, interaction.pos(), state,
                screen.getMenu().getRowCount() * 9, screen.getTitle());
        if (location == null) {
            return null;
        }

        BlockPos pos = location.pos();
        String scope = preferenceScope(minecraft, level);
        return scope == null ? null : scope + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                + "|" + location.type();
    }

    private static String preferenceScope(Minecraft minecraft, ClientLevel level) {
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
        return scope + "|" + level.dimension().location() + "|";
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != checkedLevel) {
            checkedLevel = level;
            ticksUntilCheck = 0;
        }
        if (level == null || ticksUntilCheck-- > 0) {
            return;
        }
        ticksUntilCheck = PREFERENCE_CHECK_INTERVAL_TICKS - 1;
        String scope = preferenceScope(minecraft, level);
        if (scope != null) {
            preferences(minecraft).prune(scope, (pos, type) -> StationaryStorage.keepPreference(level, pos, type));
        }
    }

    private record ChestInteraction(ClientLevel level, BlockPos pos, Block block, long clickedAt) {}
}
