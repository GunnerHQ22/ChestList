package io.github.gunnerhq22.chestlistview.client;

import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;

final class ChestSearch {
    private ChestSearch() {}

    static String searchableText(ItemStack stack, Player player) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder text = new StringBuilder(stack.getHoverName().getString())
                .append('\n').append(id);
        if (id.getNamespace().equals("minecraft")) {
            text.append("\nminecraft");
        } else {
            ModList.get().getModContainerById(id.getNamespace()).ifPresent(mod ->
                    text.append('\n').append(mod.getModInfo().getDisplayName()));
        }
        var context = Item.TooltipContext.of(player.level());
        for (Component line : stack.getTooltipLines(context, player, TooltipFlag.NORMAL)) {
            text.append('\n').append(line.getString());
        }
        if (stack.isDamageableItem()) {
            text.append('\n').append(Component.translatable("chestlistview.item.durability",
                    stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).getString())
                    .append("\ndamage ").append(stack.getDamageValue());
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    static boolean matches(String text, String[] terms) {
        for (String term : terms) {
            if (!text.contains(term)) {
                return false;
            }
        }
        return true;
    }
}
