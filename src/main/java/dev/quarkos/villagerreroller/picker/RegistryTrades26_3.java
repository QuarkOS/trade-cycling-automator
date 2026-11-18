//? if >=26.3 {
/*package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.TradeOdds;
import dev.quarkos.villagerreroller.odds.VanillaRoll;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RegistryTrades26_3 {
    private RegistryTrades26_3() {
    }

    public static List<TradeEntry> list(Level level, Villager villager, int villagerLevel) {
        if (level == null || villager == null) {
            return List.of();
        }
        return list(level, villager.getVillagerData().profession().unwrapKey().orElse(null), villagerLevel, villager);
    }

    static List<TradeEntry> list(Level level, ResourceKey<VillagerProfession> professionKey,
                                 int villagerLevel, Villager villager) {
        if (professionKey == null || professionKey.equals(VillagerProfession.NONE) || professionKey.equals(VillagerProfession.NITWIT)) {
            return List.of();
        }
        int tradeLevel = Math.max(1, Math.min(5, villagerLevel));
        RegistryAccess registries = ClientTradeRegistries.get(level);
        var professions = registries.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        VillagerProfession profession = professions.get(professionKey).map(Holder::value).orElse(null);
        if (profession == null) {
            return List.of();
        }
        ResourceKey<TradeSet> setKey = profession.getTrades(tradeLevel);
        if (setKey == null) {
            return List.of();
        }
        TradeSet set = registries.lookup(Registries.TRADE_SET).flatMap(lookup -> lookup.get(setKey)).map(Holder::value).orElse(null);
        if (set == null) {
            return List.of();
        }
        List<TradeEntry> entries = new ArrayList<>();
        int[] index = {0};
        set.trades().stream().forEach(holder -> expand(holder.value(), registries, villager, entries, "trade-" + index[0]++));
        entries.sort(Comparator
                .comparing((TradeEntry entry) -> entry.id.startsWith("enchant:"))
                .thenComparing(entry -> entry.name));
        return List.copyOf(entries);
    }

    static int[] offerCounts(Level level, ResourceKey<VillagerProfession> professionKey, int villagerLevel) {
        TradeSet set = tradeSet(level, professionKey, villagerLevel);
        if (set == null) {
            return new int[]{VanillaRoll.OFFERS_PER_LEVEL};
        }
        return counts(set.amount().value());
    }

    static boolean allowDuplicates(Level level, ResourceKey<VillagerProfession> professionKey, int villagerLevel) {
        TradeSet set = tradeSet(level, professionKey, villagerLevel);
        return set != null && set.allowDuplicates();
    }

    private static TradeSet tradeSet(Level level, ResourceKey<VillagerProfession> professionKey, int villagerLevel) {
        if (professionKey == null || professionKey.equals(VillagerProfession.NONE) || professionKey.equals(VillagerProfession.NITWIT)) {
            return null;
        }
        int tradeLevel = Math.max(1, Math.min(5, villagerLevel));
        RegistryAccess registries = ClientTradeRegistries.get(level);
        var professions = registries.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        VillagerProfession profession = professions.get(professionKey).map(Holder::value).orElse(null);
        if (profession == null) {
            return null;
        }
        ResourceKey<TradeSet> setKey = profession.getTrades(tradeLevel);
        if (setKey == null) {
            return null;
        }
        return registries.lookup(Registries.TRADE_SET).flatMap(lookup -> lookup.get(setKey)).map(Holder::value).orElse(null);
    }

    private static int[] counts(ContextIntProvider provider) {
        if (provider instanceof ConstantValue constant) {
            return new int[]{Math.max(0, constant.value())};
        }
        if (provider instanceof UniformGenerator uniform) {
            int min = uniform.min().value() instanceof ConstantValue constant ? Math.max(0, constant.value()) : VanillaRoll.OFFERS_PER_LEVEL;
            int max = uniform.max().value() instanceof ConstantValue constant ? Math.max(min, constant.value()) : min;
            return VanillaRoll.inclusive(min, max);
        }
        return new int[]{VanillaRoll.OFFERS_PER_LEVEL};
    }

    private static void expand(VillagerTrade trade, RegistryAccess registries, Villager villager,
                               List<TradeEntry> entries, String group) {
        if (!RegistryTradeDetails.fitsVariant(trade, registries, villager == null ? null : villager.getVillagerData().type())) {
            return;
        }
        ItemStack gives = trade.gives.create();
        int[] price = bounds(trade.wants.count().value());
        if (gives.is(Items.ENCHANTED_BOOK)) {
            addBooks(trade, registries, entries, group, price);
            return;
        }
        if (gives.is(Items.EMERALD)) {
            ItemStack cost = new ItemStack(trade.wants.item().value(), Math.max(1, price[0]));
            int payout = Math.max(1, gives.getCount());
            String id = "sell:" + TradeEntry.itemId(cost.getItem()) + ":" + cost.getCount() + ":" + payout;
            entries.add(TradeEntry.sell(id, cost, cost.getHoverName().getString(), payout, (offer, chosenMax) ->
                    offer.getResult().is(Items.EMERALD)
                            && offer.getResult().getCount() == payout
                            && offer.getCostA().is(cost.getItem()), TradeOdds.Roll.sell(group)));
            return;
        }
        if (trade.wants.item().value() == Items.EMERALD) {
            int min = Math.max(1, price[0]);
            int max = Math.max(min, price[1]);
            String id = "buy:" + TradeEntry.itemId(gives.getItem()) + ":" + gives.getCount() + ":" + min + ":" + max;
            entries.add(TradeEntry.buy(id, gives, gives.getHoverName().getString(), min, max, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(gives.getItem())
                            && offer.getResult().getCount() == gives.getCount(), TradeOdds.Roll.fixed(group)));
        }
    }

    private static void addBooks(VillagerTrade trade, RegistryAccess registries, List<TradeEntry> entries,
                                 String group, int[] price) {
        trade.givenItemModifier.ifPresent(holder -> walk(holder.value(), registries, entries, group, price));
    }

    private static void walk(LootItemFunction function, RegistryAccess registries, List<TradeEntry> entries,
                             String group, int[] price) {
        if (function instanceof EnchantRandomlyFunction random) {
            HolderSet<Enchantment> options = random.options.orElseGet(() ->
                    registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.TRADEABLE));
            options.stream().forEach(holder -> RegistryTradeDetails.addRandomBooks(holder, entries, group));
        } else if (function instanceof SetEnchantmentsFunction) {
            RegistryTradeDetails.addFixedBooks(function, registries, entries, group, price);
        } else if (function instanceof SequenceFunction sequence) {
            sequence.functions.stream().forEach(holder -> walk(holder.value(), registries, entries, group, price));
        }
    }

    private static int[] bounds(ContextIntProvider provider) {
        if (provider instanceof ConstantValue constant) {
            int value = Math.max(0, constant.value());
            return new int[]{value, value};
        }
        if (provider instanceof UniformGenerator uniform) {
            int min = uniform.min().value() instanceof ConstantValue constant ? Math.max(0, constant.value()) : 1;
            int max = uniform.max().value() instanceof ConstantValue constant ? Math.max(min, constant.value()) : min;
            return new int[]{min, max};
        }
        return new int[]{1, 1};
    }
}
*///?}
