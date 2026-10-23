package dev.quarkos.villagerreroller.picker;

import net.minecraft.core.Holder;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?}
import net.minecraft.world.item.enchantment.Enchantment;
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

    //? if <1.21 {
    /*public static TradeEntry book(Enchantment enchantment, int level, ItemStack icon, String name, boolean curse, int emeraldMin, int emeraldMax) {
        Identifier id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
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
            return storedBookMatches(result, enchantment, level);
        });
    }
    *///?} else {
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
    //?}

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

    //? if <1.20.5 {
    /*public static boolean storedBookMatches(ItemStack result, Enchantment enchantment, int level) {
        net.minecraft.nbt.ListTag list = net.minecraft.world.item.EnchantedBookItem.getEnchantments(result);
        Identifier id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        String wanted = id == null ? "" : id.toString();
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag tag = list.getCompound(i);
            if (wanted.equals(tag.getString("id")) && tag.getInt("lvl") == level) {
                return true;
            }
        }
        return false;
    }
    *///?}
    //? if >=1.20.5 && <1.21 {
    /*public static boolean storedBookMatches(ItemStack result, Enchantment enchantment, int level) {
        ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
        return stored != null && stored.getLevel(enchantment) == level;
    }
    *///?}

    public static String stackKey(ItemStack stack) {
        //? if <1.20.5 {
        /*return String.valueOf(stack.getTag());
        *///?} else {
        return String.valueOf(stack.getComponents());
        //?}
    }

    //? if <1.20.2 {
    /*public static boolean stewHas(ItemStack stack, MobEffect effect) {
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Effects", 9)) {
            return false;
        }
        int wanted = MobEffect.getId(effect);
        net.minecraft.nbt.ListTag list = tag.getList("Effects", 10);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getInt("EffectId") == wanted) {
                return true;
            }
        }
        return false;
    }
    *///?}
    //? if >=1.20.2 && <1.20.5 {
    /*public static boolean stewHas(ItemStack stack, MobEffect effect) {
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("effects", 9)) {
            return false;
        }
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id == null) {
            return false;
        }
        String wanted = id.toString();
        net.minecraft.nbt.ListTag list = tag.getList("effects", 10);
        for (int i = 0; i < list.size(); i++) {
            if (wanted.equals(list.getCompound(i).getString("id"))) {
                return true;
            }
        }
        return false;
    }
    *///?}
    //? if >=1.20.5 {
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
    //?}

    public static boolean potionMatches(ItemStack stack, ItemStack expected) {
        //? if <1.20.5 {
        /*if (net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack) != net.minecraft.world.item.alchemy.PotionUtils.getPotion(expected)) {
            return false;
        }
        return net.minecraft.world.item.alchemy.PotionUtils.getCustomEffects(expected).equals(net.minecraft.world.item.alchemy.PotionUtils.getCustomEffects(stack));
        *///?} else {
        PotionContents wanted = expected.get(DataComponents.POTION_CONTENTS);
        PotionContents found = stack.get(DataComponents.POTION_CONTENTS);
        if (wanted == null || found == null) {
            return wanted == null && found == null;
        }
        return wanted.equals(found);
        //?}
    }

    public static boolean named(ItemStack stack, String translationKey) {
        //? if <1.20.5 {
        /*return stack.getHoverName().getString().equals(Component.translatable(translationKey).getString());
        *///?} else {
        Component custom = stack.get(DataComponents.ITEM_NAME);
        if (custom == null) {
            return false;
        }
        return custom.getString().equals(Component.translatable(translationKey).getString());
        //?}
    }
}
