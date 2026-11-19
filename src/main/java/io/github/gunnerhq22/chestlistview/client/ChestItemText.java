package io.github.gunnerhq22.chestlistview.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

final class ChestItemText {
    private static final Pattern SHORTCUT = Pattern.compile(
            "(?:hold|press) .+? (?:to open (?:the )?guide|for (?:more |additional )?details"
                    + "|to (?:show|view) (?:more |additional )?details|to expand (?:the )?tooltip)[.!…]*");
    private static final Pattern SHORTCUT_CHORD = Pattern.compile(
            "(?:hold|press) (?:shift|ctrl|control|alt)(?:\\s*\\+\\s*[a-z0-9])+ (?:for|to) .+");
    private static final Pattern STATE = Pattern.compile(
            "^(?:capacity|stored|contents|mode|status|configuration|energy|fluid|charge|durability)\\b"
                    + "|\\b(?:bytes? used|of [\\d.,]+ (?:bytes?|fe|rf|mb))\\b");

    private ChestItemText() {}

    static List<Component> details(ItemStack stack, Item.TooltipContext context, Player player) {
        var tooltip = stack.getTooltipLines(context, player, TooltipFlag.NORMAL);
        if (tooltip.isEmpty()) {
            return List.of();
        }

        // Let vanilla generate the names and levels, including its show-in-tooltip flags.
        // Only promote lines still present after NeoForge and other mods have processed them.
        Set<String> enchantments = new HashSet<>();
        stack.addToTooltip(DataComponents.STORED_ENCHANTMENTS, context,
                line -> enchantments.add(line.getString()), TooltipFlag.NORMAL);
        stack.addToTooltip(DataComponents.ENCHANTMENTS, context,
                line -> enchantments.add(line.getString()), TooltipFlag.NORMAL);

        Set<String> effects = new HashSet<>();
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            potion.forEachEffect(effect -> effects.add(effect.getEffect().value().getDescriptionId()));
        }

        List<Detail> details = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(tooltip.getFirst().getString().strip());
        for (int i = 1; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            String text = line.getString().strip();
            if (isPreviewDetail(line, text) && seen.add(text)) {
                details.add(new Detail(line, priority(line, text, enchantments, effects)));
            }
        }
        if (stack.isDamageableItem() && details.stream().noneMatch(detail ->
                containsKey(detail.line(), key -> key.equals("item.durability")
                        || key.equals("chestlistview.item.durability")))) {
            Component durability = Component.translatable("chestlistview.item.durability",
                    stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage());
            details.add(new Detail(durability, Priority.STATE));
        }
        details.sort(Comparator.comparing(Detail::priority));
        return details.stream().map(Detail::line).toList();
    }

    static boolean isPreviewDetail(Component line, String text) {
        if (text.isBlank() || (line.getContents() instanceof TranslatableContents translated
                && (translated.getKey().startsWith("item.modifiers.")
                || translated.getKey().equals("potion.whenDrank")))) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        // A keybind by itself is not enough evidence to hide an item description.
        boolean keyPrompt = (normalized.startsWith("hold ") || normalized.startsWith("press "))
                && (normalized.contains(" for ") || normalized.contains(" to "))
                && containsKeybind(line);
        return !keyPrompt && !SHORTCUT.matcher(normalized).matches()
                && !SHORTCUT_CHORD.matcher(normalized).matches();
    }

    private static Priority priority(Component line, String text, Set<String> enchantments, Set<String> effects) {
        if (enchantments.contains(text)) {
            return Priority.ENCHANTMENT;
        }
        if (containsKey(line, key -> effects.contains(key) || key.equals("potion.withDuration")
                || key.equals("potion.withAmplifier"))) {
            return Priority.POTION;
        }
        if (containsKey(line, key -> key.startsWith("attribute.modifier.")
                || key.startsWith("neoforge.attribute."))) {
            return Priority.ATTRIBUTE;
        }
        return STATE.matcher(text.toLowerCase(Locale.ROOT)).find() ? Priority.STATE : Priority.OTHER;
    }

    private static boolean containsKey(Component line, Predicate<String> matches) {
        return anyComponent(line, part -> part.getContents() instanceof TranslatableContents translated
                && matches.test(translated.getKey()));
    }

    private static boolean containsKeybind(Component line) {
        return anyComponent(line, part -> part.getContents() instanceof KeybindContents);
    }

    private static boolean anyComponent(Component line, Predicate<Component> matches) {
        if (matches.test(line)) {
            return true;
        }
        if (line.getContents() instanceof TranslatableContents translated) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component component && anyComponent(component, matches)) {
                    return true;
                }
            }
        }
        return line.getSiblings().stream().anyMatch(sibling -> anyComponent(sibling, matches));
    }

    private enum Priority { ENCHANTMENT, STATE, POTION, ATTRIBUTE, OTHER }

    private record Detail(Component line, Priority priority) {}
}
