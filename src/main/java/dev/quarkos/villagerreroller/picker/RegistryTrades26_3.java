//? if >=26.3 {
/*package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.price.VanillaBookPrices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
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
        ResourceKey<VillagerProfession> professionKey = villager.getVillagerData().profession().unwrapKey().orElse(null);
        if (professionKey == null || professionKey.equals(VillagerProfession.NONE) || professionKey.equals(VillagerProfession.NITWIT)) {
            return List.of();
        }
        int tradeLevel = Math.max(1, Math.min(5, villagerLevel));
        RegistryAccess registries = registries(level);
        HolderLookup.RegistryLookup<VillagerProfession> professions = registries.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        VillagerProfession profession = professions.get(professionKey).map(Holder::value).orElse(null);
        if (profession == null) {
            return List.of();
        }
        ResourceKey<TradeSet> setKey = profession.getTrades(tradeLevel);
        if (setKey == null) {
            return List.of();
        }
        HolderLookup.RegistryLookup<TradeSet> sets = registries.lookupOrThrow(Registries.TRADE_SET);
        TradeSet set = sets.get(setKey).map(Holder::value).orElse(null);
        if (set == null) {
            return List.of();
        }
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        List<TradeEntry> entries = new ArrayList<>();
        set.trades().stream().forEach(holder -> expand(holder.value(), enchantments, entries));
        entries.sort(Comparator
                .comparing((TradeEntry entry) -> entry.id.startsWith("enchant:"))
                .thenComparing(entry -> entry.name));
        return List.copyOf(entries);
    }

    private static void expand(VillagerTrade trade, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        if (trade == null) {
            return;
        }
        if (addBooks(trade, enchantments, entries)) {
            return;
        }
        ItemStack gives = trade.gives.create();
        int[] price = bounds(trade.wants.count().value());
        if (gives.is(Items.EMERALD)) {
            ItemStack cost = new ItemStack(trade.wants.item().value(), Math.max(1, price[0]));
            int payout = Math.max(1, gives.getCount());
            String id = "sell:" + TradeEntry.itemId(cost.getItem()) + ":" + cost.getCount() + ":" + payout;
            entries.add(TradeEntry.sell(id, cost, cost.getHoverName().getString(), payout, (offer, chosenMax) ->
                    offer.getResult().is(Items.EMERALD)
                            && offer.getResult().getCount() == payout
                            && offer.getCostA().is(cost.getItem())));
            return;
        }
        if (trade.wants.item().value() == Items.EMERALD) {
            int min = Math.max(1, price[0]);
            int max = Math.max(min, price[1]);
            String id = "buy:" + TradeEntry.itemId(gives.getItem()) + ":" + gives.getCount() + ":" + min + ":" + max;
            entries.add(TradeEntry.buy(id, gives, gives.getHoverName().getString(), min, max, (offer, chosenMax) ->
                    TradeEntry.emeraldCostAtMost(offer, chosenMax)
                            && offer.getResult().is(gives.getItem())
                            && offer.getResult().getCount() == gives.getCount()));
        }
    }

    private static boolean addBooks(VillagerTrade trade, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        int before = entries.size();
        trade.givenItemModifier.ifPresent(holder -> walk(holder.value(), enchantments, entries));
        return entries.size() != before;
    }

    private static void walk(LootItemFunction function, HolderLookup.RegistryLookup<Enchantment> enchantments, List<TradeEntry> entries) {
        if (function instanceof EnchantRandomlyFunction random) {
            HolderSet<Enchantment> options = random.options.orElseGet(() -> enchantments.getOrThrow(EnchantmentTags.TRADEABLE));
            options.stream().forEach(holder -> addBook(holder, entries));
            return;
        }
        if (function instanceof SequenceFunction sequence) {
            sequence.functions.stream().forEach(holder -> walk(holder.value(), enchantments, entries));
        }
    }

    private static void addBook(Holder<Enchantment> holder, List<TradeEntry> entries) {
        Enchantment enchantment = holder.value();
        int low = enchantment.getMinLevel();
        int high = enchantment.getMaxLevel();
        if (low > high) {
            return;
        }
        boolean treasure = holder.is(EnchantmentTags.DOUBLE_TRADE_PRICE);
        boolean curse = holder.is(EnchantmentTags.CURSE);
        for (int level = low; level <= high; level++) {
            ItemStack icon = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
            String name = enchantment.description().getString();
            if (level != 1 || enchantment.getMaxLevel() != 1) {
                name = name + " " + net.minecraft.network.chat.Component.translatable("enchantment.level." + level).getString();
            }
            entries.add(TradeEntry.book(holder, level, icon, name, curse, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure)));
        }
    }

    private static RegistryAccess registries(Level level) {
        // trade_set is a world registry. The client connection only has synchronized registries.
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            return server.registryAccess();
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.registryAccess().lookup(Registries.TRADE_SET).isPresent()) {
            return connection.registryAccess();
        }
        return level.registryAccess();
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
