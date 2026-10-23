//? if <26.1 {
package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.price.VanillaBookPrices;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
//? if >=1.21 {
import net.minecraft.tags.EnchantmentTags;
//?}
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Expands the 1.21.x villager listing table. 26.x reads trade sets from the registry instead.
 */
public final class ListingTrades {
    private ListingTrades() {
    }

    public static List<TradeEntry> list(Level level, Villager villager, int villagerLevel) {
        if (level == null || villager == null) {
            return List.of();
        }
        int tradeLevel = Math.max(1, Math.min(5, villagerLevel));
        //? if >=1.20.2 {
        boolean rebalance = level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
        //?}
        Int2ObjectMap<VillagerTrades.ItemListing[]> table = null;
        Object villagerType;
        //? if <1.20.2 {
        /*VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            return List.of();
        }
        table = VillagerTrades.TRADES.get(profession);
        villagerType = villager.getVillagerData().getType();
        *///?}
        //? if >=1.20.2 && <1.21.5 {
        /*VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            return List.of();
        }
        if (rebalance) {
            table = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
        }
        if (table == null) {
            table = VillagerTrades.TRADES.get(profession);
        }
        villagerType = villager.getVillagerData().getType();
        *///?}
        //? if >=1.21.5 {
        ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        if (profession == null || profession.equals(VillagerProfession.NONE) || profession.equals(VillagerProfession.NITWIT)) {
            return List.of();
        }
        if (rebalance) {
            table = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
        }
        if (table == null) {
            table = VillagerTrades.TRADES.get(profession);
        }
        villagerType = villager.getVillagerData().type().unwrapKey().orElse(null);
        //?}
        if (table == null) {
            return List.of();
        }
        VillagerTrades.ItemListing[] listings = table.get(tradeLevel);
        if (listings == null || listings.length == 0) {
            return List.of();
        }
        HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<TradeEntry> entries = new ArrayList<>();
        for (VillagerTrades.ItemListing listing : listings) {
            expand(listing, villagerType, enchantments, entries);
        }
        entries.sort(Comparator
                .comparing((TradeEntry entry) -> entry.id.startsWith("enchant:"))
                .thenComparing(entry -> entry.name));
        return List.copyOf(entries);
    }

    private static void expand(VillagerTrades.ItemListing listing, Object type, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        if (listing == null || listing.getClass().getSimpleName().equals("FailureItemListing")) {
            return;
        }
        //? if >=1.20.2 {
        if (listing instanceof VillagerTrades.TypeSpecificTrade specific) {
            if (type == null) {
                return;
            }
            //? if <1.21.5 {
            /*expand(specific.trades().get((VillagerType) type), type, enchantments, entries);
            *///?} else {
            expand(specific.trades().get((ResourceKey<VillagerType>) type), type, enchantments, entries);
            //?}
            return;
        }
        //?}
        if (listing instanceof VillagerTrades.EnchantBookForEmeralds books) {
            expandBooks(books, enchantments, entries);
            return;
        }
        //? if <1.20.2 {
        /*if (listing instanceof VillagerTrades.ItemsForEmeralds sale) {
            ItemStack stack = sale.itemStack.copy();
            stack.setCount(sale.numberOfItems);
            addBuy(entries, stack, sale.emeraldCost);
            return;
        }
        *///?}
        //? if >=1.20.2 {
        if (listing instanceof VillagerTrades.ItemsForEmeralds sale) {
            addBuy(entries, sale.itemStack.copy(), sale.emeraldCost);
            return;
        }
        //?}
        //? if <1.20.2 {
        /*if (listing instanceof VillagerTrades.EmeraldForItems purchase) {
            addSell(entries, new ItemStack(purchase.item, purchase.cost), 1);
            return;
        }
        *///?}
        //? if >=1.20.2 {
        if (listing instanceof VillagerTrades.EmeraldForItems purchase) {
            //? if <1.20.5 {
            /*ItemStack stack = purchase.itemStack.copy();
            *///?} else {
            ItemStack stack = purchase.itemStack.itemStack().copy();
            //?}
            addSell(entries, stack, purchase.emeraldAmount);
            return;
        }
        //?}
        if (listing instanceof VillagerTrades.EnchantedItemForEmeralds gear) {
            Item item = gear.itemStack.getItem();
            int min = VanillaBookPrices.enchantedGearMin(gear.baseEmeraldCost);
            int max = VanillaBookPrices.enchantedGearMax(gear.baseEmeraldCost);
            ItemStack icon = gear.itemStack.copy();
            String id = "gear:" + TradeEntry.itemId(item) + ":" + gear.baseEmeraldCost;
            entries.add(TradeEntry.buy(id, icon, "Enchanted " + icon.getHoverName().getString(), min, max, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(item)
                            && offer.getResult().isEnchanted()));
            return;
        }
        //? if <1.20.2 {
        /*if (listing instanceof VillagerTrades.ItemsAndEmeraldsToItems conversion) {
            ItemStack result = new ItemStack(conversion.toItem.getItem(), conversion.toCount);
            addBuy(entries, result, conversion.emeraldCost);
            return;
        }
        *///?}
        //? if >=1.20.2 {
        if (listing instanceof VillagerTrades.ItemsAndEmeraldsToItems conversion) {
            ItemStack result = conversion.toItem.copy();
            int cost = conversion.emeraldCost;
            //? if <1.20.5 {
            /*Item extra = conversion.fromItem.getItem();
            *///?} else {
            Item extra = conversion.fromItem.item().value();
            //?}
            String id = "convert:" + TradeEntry.itemId(result.getItem()) + ":" + cost + ":" + TradeEntry.itemId(extra);
            entries.add(TradeEntry.buy(id, result, result.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(result.getItem())
                            && offer.getResult().getCount() == result.getCount()));
            return;
        }
        //?}
        if (listing instanceof VillagerTrades.DyedArmorForEmeralds dyed) {
            Item item = dyed.item;
            int cost = dyed.value;
            ItemStack icon = new ItemStack(item);
            String id = "dye:" + TradeEntry.itemId(item) + ":" + cost;
            entries.add(TradeEntry.buy(id, icon, icon.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax) && offer.getResult().is(item)));
            return;
        }
        if (listing instanceof VillagerTrades.TippedArrowForItemsAndEmeralds arrows) {
            ItemStack icon = new ItemStack(arrows.toItem.getItem(), arrows.toCount);
            int cost = arrows.emeraldCost;
            String id = "arrows:" + cost + ":" + arrows.toCount;
            entries.add(TradeEntry.buy(id, icon, icon.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(arrows.toItem.getItem())
                            && offer.getResult().getCount() == arrows.toCount));
            return;
        }
        if (listing instanceof VillagerTrades.TreasureMapForEmeralds map) {
            int cost = map.emeraldCost;
            ItemStack icon = new ItemStack(Items.FILLED_MAP);
            //? if <1.20.5 {
            /*icon.setHoverName(Component.translatable(map.displayName));
            *///?} else {
            icon.set(DataComponents.ITEM_NAME, Component.translatable(map.displayName));
            //?}
            String id = "map:" + map.displayName + ":" + cost;
            entries.add(TradeEntry.buy(id, icon, icon.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax) && TradeEntry.named(offer.getResult(), map.displayName)));
            return;
        }
        //? if <1.20.2 {
        /*if (listing instanceof VillagerTrades.SuspiciousStewForEmerald stew) {
            MobEffect mobEffect = stew.effect;
            ItemStack icon = new ItemStack(Items.SUSPICIOUS_STEW);
            net.minecraft.world.item.SuspiciousStewItem.saveMobEffect(icon, mobEffect, stew.duration);
            net.minecraft.resources.Identifier effectKey = BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
            String effectId = effectKey == null ? "unknown" : effectKey.toString();
            String name = icon.getHoverName().getString() + " (" + mobEffect.getDisplayName().getString() + ")";
            String id = "stew:" + effectId;
            entries.add(TradeEntry.buy(id, icon, name, 1, 1, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(Items.SUSPICIOUS_STEW)
                            && TradeEntry.stewHas(offer.getResult(), mobEffect)));
            return;
        }
        *///?}
        //? if >=1.20.2 {
        if (listing instanceof VillagerTrades.SuspiciousStewForEmerald stew) {
            //? if <1.20.5 {
            /*for (Object raw : stew.effects) {
                if (!(raw instanceof net.minecraft.world.level.block.SuspiciousEffectHolder.EffectEntry entry)) {
                    continue;
                }
                MobEffect mobEffect = entry.effect();
                ItemStack icon = new ItemStack(Items.SUSPICIOUS_STEW);
                net.minecraft.world.item.SuspiciousStewItem.saveMobEffects(icon, java.util.List.of(entry));
                net.minecraft.resources.Identifier effectKey = BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
                String effectId = effectKey == null ? "unknown" : effectKey.toString();
                String name = icon.getHoverName().getString() + " (" + mobEffect.getDisplayName().getString() + ")";
                String id = "stew:" + effectId;
                entries.add(TradeEntry.buy(id, icon, name, 1, 1, (offer, chosenMax) ->
                        TradeEntry.emeraldCostAtMost(offer, chosenMax)
                                && offer.getResult().is(Items.SUSPICIOUS_STEW)
                                && TradeEntry.stewHas(offer.getResult(), mobEffect)));
            }
            *///?} else {
            for (var effect : stew.effects.effects()) {
                Holder<MobEffect> mobEffect = effect.effect();
                ItemStack icon = new ItemStack(Items.SUSPICIOUS_STEW);
                icon.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, stew.effects);
                String effectId = mobEffect.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
                String name = icon.getHoverName().getString() + " (" + mobEffect.value().getDisplayName().getString() + ")";
                String id = "stew:" + effectId;
                entries.add(TradeEntry.buy(id, icon, name, 1, 1, (offer, chosenMax) ->
                        TradeEntry.emeraldCostAtMost(offer, chosenMax)
                                && offer.getResult().is(Items.SUSPICIOUS_STEW)
                                && TradeEntry.stewHas(offer.getResult(), mobEffect)));
            }
            //?}
            return;
        }
        //?}
        if (listing instanceof VillagerTrades.EmeraldsForVillagerTypeItem typed) {
            if (type == null) {
                return;
            }
            //? if <1.21.5 {
            /*Item item = (Item) typed.trades.get(type);
            *///?} else {
            Item item = typed.trades.get((ResourceKey<VillagerType>) type);
            //?}
            if (item == null) {
                return;
            }
            ItemStack icon = new ItemStack(item, typed.cost);
            //? if <1.21.5 {
            /*String typeId = String.valueOf(BuiltInRegistries.VILLAGER_TYPE.getKey((VillagerType) type));
            *///?} else {
            String typeId = ((ResourceKey<VillagerType>) type).identifier().toString();
            //?}
            String id = "typed:" + typeId + ":" + TradeEntry.itemId(item);
            entries.add(TradeEntry.sell(id, icon, icon.getHoverName().getString(), 1, (offer, chosenMax) ->
                    offer.getResult().is(Items.EMERALD)
                            && offer.getResult().getCount() == 1
                            && offer.getCostA().is(item)));
        }
    }

    private static void addBuy(List<TradeEntry> entries, ItemStack stack, int cost) {
        String id = "buy:" + TradeEntry.itemId(stack.getItem()) + ":" + stack.getCount() + ":" + cost;
        entries.add(TradeEntry.buy(id, stack, stack.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                TradeEntry.emeraldCostAtMost(offer, chosenMax)
                        && offer.getResult().is(stack.getItem())
                        && offer.getResult().getCount() == stack.getCount()));
    }

    private static void addSell(List<TradeEntry> entries, ItemStack stack, int payout) {
        String id = "sell:" + TradeEntry.itemId(stack.getItem()) + ":" + stack.getCount() + ":" + payout;
        entries.add(TradeEntry.sell(id, stack, stack.getHoverName().getString(), payout, (offer, chosenMax) ->
                offer.getResult().is(Items.EMERALD)
                        && offer.getResult().getCount() == payout
                        && offer.getCostA().is(stack.getItem())));
    }

    //? if <1.20.2 {
    /*private static void expandBooks(VillagerTrades.EnchantBookForEmeralds books, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            if (!enchantment.isTradeable()) {
                continue;
            }
            int low = enchantment.getMinLevel();
            int high = enchantment.getMaxLevel();
            if (low > high) {
                continue;
            }
            boolean treasure = enchantment.isTreasureOnly();
            boolean curse = enchantment.isCurse();
            for (int level = low; level <= high; level++) {
                ItemStack icon = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
                String name = Component.translatable(enchantment.getDescriptionId()).getString();
                if (level != 1 || enchantment.getMaxLevel() != 1) {
                    name = name + " " + Component.translatable("enchantment.level." + level).getString();
                }
                entries.add(TradeEntry.book(enchantment, level, icon, name, curse, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure)));
            }
        }
    }
    *///?}
    //? if >=1.20.2 {
    private static void expandBooks(VillagerTrades.EnchantBookForEmeralds books, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        //? if >=1.20.2 && <1.21 {
        /*for (Object raw : books.tradeableEnchantments) {
            Enchantment enchantment = (Enchantment) raw;
            int low = Math.max(enchantment.getMinLevel(), books.minLevel);
            int high = Math.min(enchantment.getMaxLevel(), books.maxLevel);
            if (low > high) {
                continue;
            }
            boolean treasure = enchantment.isTreasureOnly();
            boolean curse = enchantment.isCurse();
            for (int level = low; level <= high; level++) {
                ItemStack icon = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
                String name = Component.translatable(enchantment.getDescriptionId()).getString();
                if (level != 1 || enchantment.getMaxLevel() != 1) {
                    name = name + " " + Component.translatable("enchantment.level." + level).getString();
                }
                entries.add(TradeEntry.book(enchantment, level, icon, name, curse, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure)));
            }
        }
        *///?} else {
        TagKey<Enchantment> tag = books.tradeableEnchantments;
        for (Holder<Enchantment> holder : enchantments.getOrThrow(tag)) {
            Enchantment enchantment = holder.value();
            int low = Math.max(enchantment.getMinLevel(), books.minLevel);
            int high = Math.min(enchantment.getMaxLevel(), books.maxLevel);
            if (low > high) {
                continue;
            }
            boolean treasure = holder.is(EnchantmentTags.DOUBLE_TRADE_PRICE);
            boolean curse = holder.is(EnchantmentTags.CURSE);
            for (int level = low; level <= high; level++) {
                //? if <1.21.2 {
                /*ItemStack icon = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level));
                *///?} else {
                ItemStack icon = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
                //?}
                String name = enchantment.description().getString();
                if (level != 1 || enchantment.getMaxLevel() != 1) {
                    name = name + " " + Component.translatable("enchantment.level." + level).getString();
                }
                entries.add(TradeEntry.book(holder, level, icon, name, curse, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure)));
            }
        }
        //?}
    }
    //?}
}
//?}
