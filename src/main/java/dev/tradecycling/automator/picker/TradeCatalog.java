package dev.tradecycling.automator.picker;

import dev.tradecycling.automator.price.VanillaBookPrices;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
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
 * Expands the vanilla listing table for one profession level into picker rows.
 */
public final class TradeCatalog {
    private TradeCatalog() {
    }

    public static List<TradeEntry> list(Level level, Villager villager, int villagerLevel) {
        if (level == null || villager == null) {
            return List.of();
        }
        ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        if (profession == null || profession.equals(VillagerProfession.NONE) || profession.equals(VillagerProfession.NITWIT)) {
            return List.of();
        }
        int tradeLevel = Math.max(1, Math.min(5, villagerLevel));
        boolean rebalance = level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
        Int2ObjectMap<VillagerTrades.ItemListing[]> table = null;
        if (rebalance) {
            table = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
        }
        if (table == null) {
            table = VillagerTrades.TRADES.get(profession);
        }
        if (table == null) {
            return List.of();
        }
        VillagerTrades.ItemListing[] listings = table.get(tradeLevel);
        if (listings == null || listings.length == 0) {
            return List.of();
        }
        ResourceKey<VillagerType> type = villager.getVillagerData().type().unwrapKey().orElse(null);
        HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<TradeEntry> entries = new ArrayList<>();
        for (VillagerTrades.ItemListing listing : listings) {
            expand(listing, type, enchantments, entries);
        }
        entries.sort(Comparator
                .comparing((TradeEntry entry) -> entry.id.startsWith("enchant:"))
                .thenComparing(entry -> entry.name));
        return List.copyOf(entries);
    }

    private static void expand(VillagerTrades.ItemListing listing, ResourceKey<VillagerType> type, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        if (listing == null || listing.getClass().getSimpleName().equals("FailureItemListing")) {
            return;
        }
        if (listing instanceof VillagerTrades.TypeSpecificTrade specific) {
            if (type == null) {
                return;
            }
            expand(specific.trades().get(type), type, enchantments, entries);
            return;
        }
        if (listing instanceof VillagerTrades.EnchantBookForEmeralds books) {
            expandBooks(books, enchantments, entries);
            return;
        }
        if (listing instanceof VillagerTrades.ItemsForEmeralds sale) {
            ItemStack stack = sale.itemStack.copy();
            int cost = sale.emeraldCost;
            String id = "buy:" + TradeEntry.itemId(stack.getItem()) + ":" + stack.getCount() + ":" + cost;
            entries.add(TradeEntry.buy(id, stack, stack.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(stack.getItem())
                            && offer.getResult().getCount() == stack.getCount()));
            return;
        }
        if (listing instanceof VillagerTrades.EmeraldForItems purchase) {
            ItemStack stack = purchase.itemStack.itemStack().copy();
            int payout = purchase.emeraldAmount;
            String id = "sell:" + TradeEntry.itemId(stack.getItem()) + ":" + stack.getCount() + ":" + payout;
            entries.add(TradeEntry.sell(id, stack, stack.getHoverName().getString(), payout, (offer, chosenMax) ->
                    offer.getResult().is(Items.EMERALD)
                            && offer.getResult().getCount() == payout
                            && offer.getCostA().is(stack.getItem())));
            return;
        }
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
        if (listing instanceof VillagerTrades.ItemsAndEmeraldsToItems conversion) {
            ItemStack result = conversion.toItem.copy();
            int cost = conversion.emeraldCost;
            Item extra = conversion.fromItem.item().value();
            String id = "convert:" + TradeEntry.itemId(result.getItem()) + ":" + cost + ":" + TradeEntry.itemId(extra);
            entries.add(TradeEntry.buy(id, result, result.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(result.getItem())
                            && offer.getResult().getCount() == result.getCount()));
            return;
        }
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
            icon.set(DataComponents.ITEM_NAME, Component.translatable(map.displayName));
            String id = "map:" + map.displayName + ":" + cost;
            entries.add(TradeEntry.buy(id, icon, icon.getHoverName().getString(), cost, cost, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax) && TradeEntry.named(offer.getResult(), map.displayName)));
            return;
        }
        if (listing instanceof VillagerTrades.SuspiciousStewForEmerald stew) {
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
            return;
        }
        if (listing instanceof VillagerTrades.EmeraldsForVillagerTypeItem typed) {
            if (type == null) {
                return;
            }
            Item item = typed.trades.get(type);
            if (item == null) {
                return;
            }
            ItemStack icon = new ItemStack(item, typed.cost);
            String id = "typed:" + type.identifier() + ":" + TradeEntry.itemId(item);
            entries.add(TradeEntry.sell(id, icon, icon.getHoverName().getString(), 1, (offer, chosenMax) ->
                    offer.getResult().is(Items.EMERALD)
                            && offer.getResult().getCount() == 1
                            && offer.getCostA().is(item)));
        }
    }

    private static void expandBooks(VillagerTrades.EnchantBookForEmeralds books, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
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
                ItemStack icon = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
                String name = enchantment.description().getString();
                if (level != 1 || enchantment.getMaxLevel() != 1) {
                    name = name + " " + Component.translatable("enchantment.level." + level).getString();
                }
                entries.add(TradeEntry.book(holder, level, icon, name, curse, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure)));
            }
        }
    }
}
