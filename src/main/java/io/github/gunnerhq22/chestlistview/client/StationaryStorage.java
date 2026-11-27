package io.github.gunnerhq22.chestlistview.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Identifies a world block for saved views after the screen has passed the menu compatibility checks. */
final class StationaryStorage {
    private StationaryStorage() {}

    static boolean isCandidate(ClientLevel level, BlockPos pos, BlockState state) {
        return isVanillaChest(state) || state.is(Blocks.BARREL) || level.getBlockEntity(pos) instanceof Container;
    }

    static Location resolve(ClientLevel level, BlockPos pos, BlockState state, int slots, Component title) {
        ChestType type = chestType(state);
        boolean paired = type != ChestType.SINGLE;
        var blockEntity = level.getBlockEntity(pos);
        if (isVanillaChest(state) || state.is(Blocks.BARREL)) {
            String titleKey = state.is(Blocks.BARREL) ? "container.barrel"
                    : paired ? "container.chestDouble" : "container.chest";
            if (slots != (paired ? 54 : 27) || !title.equals(Component.translatable(titleKey))) {
                return null;
            }
        } else {
            if (!(blockEntity instanceof Container container)
                    || slots != container.getContainerSize() * (paired ? 2 : 1)
                    || !matchesTitle(level, pos, state, blockEntity, title, paired)) {
                return null;
            }
        }

        if (paired) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (!isLoaded(level, other)) {
                return null;
            }
            BlockState otherState = level.getBlockState(other);
            if (!otherState.is(state.getBlock()) || chestType(otherState) != type.getOpposite()
                    || otherState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
                    || !other.relative(ChestBlock.getConnectedDirection(otherState)).equals(pos)) {
                return null;
            }
            if (other.compareTo(pos) < 0) {
                pos = other;
            }
        }
        return new Location(pos, storageType(state, paired));
    }

    private static boolean matchesTitle(ClientLevel level, BlockPos pos, BlockState state,
            BlockEntity blockEntity, Component title, boolean paired) {
        if (!paired && blockEntity instanceof MenuProvider blockProvider && title.equals(blockProvider.getDisplayName())) {
            return true;
        }
        if (paired && title.equals(Component.translatable("container.chestDouble"))) {
            return true;
        }
        MenuProvider provider = state.getMenuProvider(level, pos);
        return provider != null && title.equals(provider.getDisplayName());
    }

    static boolean keepPreference(ClientLevel level, BlockPos pos, String type) {
        if (!isLoaded(level, pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (type.isEmpty()) {
            return isVanillaChest(state);
        }
        return storageType(state, chestType(state) != ChestType.SINGLE).equals(type);
    }

    private static boolean isLoaded(ClientLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static ChestType chestType(BlockState state) {
        return state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING)
                ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
    }

    private static boolean isVanillaChest(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
    }

    private static String storageType(BlockState state, boolean paired) {
        if (state.is(Blocks.BARREL)) {
            return "barrel";
        }
        if (isVanillaChest(state)) {
            return (paired ? "double_" : "") + (state.is(Blocks.TRAPPED_CHEST) ? "trapped_chest" : "chest");
        }
        return "block:" + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + (paired ? "/double" : "/single");
    }

    record Location(BlockPos pos, String type) {}
}
