//? if >=26.1 {
/*package dev.quarkos.villagerreroller.picker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.quarkos.villagerreroller.odds.TradeOdds;
import dev.quarkos.villagerreroller.odds.VanillaRoll;
import dev.quarkos.villagerreroller.price.VanillaBookPrices;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

import java.util.List;

final class RegistryTradeDetails {
    private RegistryTradeDetails() {
    }

    // Vanilla uses a villager variant condition for regional trades and Trade Rebalance books.
    static boolean fitsVariant(VillagerTrade trade, RegistryAccess registries, Holder<VillagerType> type) {
        if (type == null) {
            return true;
        }
        var encoded = VillagerTrade.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), trade).result();
        if (encoded.orElse(null) instanceof JsonObject data
                && data.get("merchant_predicate") instanceof JsonObject condition
                && condition.get("predicate") instanceof JsonObject entity) {
            JsonElement predicates = entity.has("minecraft:predicates") ? entity.get("minecraft:predicates") : entity.get("predicates");
            if (predicates instanceof JsonObject properties && properties.has("minecraft:villager/variant")) {
                return matchesVariant(properties.get("minecraft:villager/variant"), type);
            }
        }
        return true;
    }

    private static boolean matchesVariant(JsonElement variants, Holder<VillagerType> type) {
        if (variants.isJsonArray()) {
            return variants.getAsJsonArray().asList().stream().anyMatch(variant -> matchesVariant(variant, type));
        }
        if (!variants.isJsonPrimitive() || !variants.getAsJsonPrimitive().isString()) {
            return true;
        }
        String value = variants.getAsString();
        if (value.startsWith("#")) {
            Identifier id = Identifier.tryParse(value.substring(1));
            return id != null && type.is(TagKey.create(Registries.VILLAGER_TYPE, id));
        }
        Identifier id = Identifier.tryParse(value);
        return id != null && type.is(ResourceKey.create(Registries.VILLAGER_TYPE, id));
    }

    static void addFixedBooks(LootItemFunction function, RegistryAccess registries, List<TradeEntry> entries,
                              String group, int[] price) {
        var encoded = LootItemFunctions.TYPED_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), function).result();
        if (!(encoded.orElse(null) instanceof JsonObject data) || !(data.get("enchantments") instanceof JsonObject levels)) {
            return;
        }
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (var entry : levels.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            JsonElement count = entry.getValue();
            if (id != null && count.isJsonPrimitive() && count.getAsJsonPrimitive().isNumber() && count.getAsInt() > 0) {
                enchantments.get(id).ifPresent(holder -> entries.add(book(holder, count.getAsInt(),
                        Math.max(1, price[0]), Math.max(1, price[1]), TradeOdds.Roll.fixed(group))));
            }
        }
    }

    static void addRandomBooks(Holder<Enchantment> holder, List<TradeEntry> entries, String group) {
        Enchantment enchantment = holder.value();
        boolean treasure = holder.is(EnchantmentTags.DOUBLE_TRADE_PRICE);
        String id = holder.unwrapKey().orElseThrow().identifier().toString();
        for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
            entries.add(book(holder, level, VanillaBookPrices.min(level, treasure), VanillaBookPrices.max(level, treasure),
                    TradeOdds.Roll.book(group, id, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT, level, treasure)));
        }
    }

    private static TradeEntry book(Holder<Enchantment> holder, int level, int min, int max, TradeOdds.Roll roll) {
        Enchantment enchantment = holder.value();
        var icon = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
        String name = enchantment.description().getString();
        if (level != 1 || enchantment.getMaxLevel() != 1) {
            name += " " + Component.translatable("enchantment.level." + level).getString();
        }
        return TradeEntry.book(holder, level, icon, name, holder.is(EnchantmentTags.CURSE), min, max, roll);
    }
}
*///?}
