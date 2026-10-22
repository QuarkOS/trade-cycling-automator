package dev.quarkos.villagerreroller.picker;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.function.BiPredicate;

/**
 * One row in the trade picker. {@code emeraldMin} and {@code emeraldMax} are the
 * vanilla roll before reputation discounts. A chosen ceiling of {@link Integer#MAX_VALUE}
 * means any price.
 */
public final class TradeEntry {
    public final String id;
    public final ItemStack icon;
    public final String name;
    public final boolean curse;
    public final int emeraldMin;
    public final int emeraldMax;
    /** True when the villager is paid in emeralds. False when the villager pays emeralds. */
    public final boolean paysEmeralds;
    private final BiPredicate<MerchantOffer, Integer> matcher;

    private TradeEntry(String id, ItemStack icon, String name, boolean curse, int emeraldMin, int emeraldMax, boolean paysEmeralds, BiPredicate<MerchantOffer, Integer> matcher) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.curse = curse;
        this.emeraldMin = emeraldMin;
        this.emeraldMax = emeraldMax;
        this.paysEmeralds = paysEmeralds;
        this.matcher = matcher;
    }

    public boolean matches(MerchantOffer offer, int chosenMax) {
        if (offer == null || offer.isOutOfStock()) {
            return false;
        }
        return matcher.test(offer, chosenMax);
    }

    public String priceLabel() {
        if (!paysEmeralds) {
            return "\u2192 " + emeraldMin;
        }
        if (emeraldMin == emeraldMax) {
            return Integer.toString(emeraldMin);
        }
        return emeraldMin + "\u2013" + emeraldMax;
    }

    public static TradeEntry book(Holder<Enchantment> enchantment, int level, ItemStack icon, String name, boolean curse, int emeraldMin, int emeraldMax) {
        Identifier id = enchantment.unwrapKey().orElseThrow().identifier();
        String entryId = "enchant:" + id + ":" + level;
        return new TradeEntry(entryId, icon, name, curse, emeraldMin, emeraldMax, true, (offer, chosenMax) -> {
            ItemStack cost = offer.getCostA();
            if (!cost.is(Items.EMERALD) || cost.getCount() <= 0 || cost.getCount() > chosenMax) {
                return false;
            }
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                return false;
            }
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            return stored != null && stored.getLevel(enchantment) == level;
        });
    }

    public static TradeEntry buy(String id, ItemStack icon, String name, int emeraldMin, int emeraldMax, BiPredicate<MerchantOffer, Integer> matcher) {
        return new TradeEntry(id, icon, name, false, emeraldMin, emeraldMax, true, matcher);
    }

    public static TradeEntry sell(String id, ItemStack icon, String name, int emeraldsPaidByVillager, BiPredicate<MerchantOffer, Integer> matcher) {
        return new TradeEntry(id, icon, name, false, emeraldsPaidByVillager, emeraldsPaidByVillager, false, matcher);
    }

    public static boolean emeraldCostAtMost(MerchantOffer offer, int chosenMax) {
        ItemStack cost = offer.getCostA();
        return cost.is(Items.EMERALD) && cost.getCount() > 0 && cost.getCount() <= chosenMax;
    }

    public static boolean sameItem(ItemStack stack, Item item) {
        return stack != null && !stack.isEmpty() && stack.is(item);
    }

    public static String itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "minecraft:air" : id.toString();
    }

    public static boolean stewHas(ItemStack stack, Holder<MobEffect> effect) {
        var effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects == null) {
            return false;
        }
        for (var entry : effects.effects()) {
            if (entry.effect().equals(effect)) {
                return true;
            }
        }
        return false;
    }

    public static boolean potionMatches(ItemStack stack, ItemStack expected) {
        PotionContents wanted = expected.get(DataComponents.POTION_CONTENTS);
        PotionContents found = stack.get(DataComponents.POTION_CONTENTS);
        if (wanted == null || found == null) {
            return wanted == null && found == null;
        }
        return wanted.equals(found);
    }

    public static boolean named(ItemStack stack, String translationKey) {
        Component custom = stack.get(DataComponents.ITEM_NAME);
        if (custom == null) {
            return false;
        }
        return custom.getString().equals(Component.translatable(translationKey).getString());
    }
}
