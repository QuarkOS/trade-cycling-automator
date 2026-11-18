package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.VanillaRoll;
import net.minecraft.resources.Identifier;
//? if >=26.1 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expands the vanilla trades for one profession level into picker rows.
 * 1.21.x reads the listing table. 26.x reads the trade-set registry.
 */
public final class TradeCatalog {
    private TradeCatalog() {
    }

    public static List<TradeEntry> list(Level level, Villager villager, int villagerLevel) {
        //? if >=26.3 {
        /*return RegistryTrades26_3.list(level, villager, villagerLevel);
        *///?} else {
        //? if >=26.1 {
        /*return RegistryTrades.list(level, villager, villagerLevel);
        *///?} else {
        return ListingTrades.list(level, villager, villagerLevel);
        //?}
        //?}
    }

    /** All distinct offers for this profession, across its five villager tiers. */
    static List<TradeEntry> browse(Level level, Identifier profession) {
        if (level == null || profession == null) {
            return List.of();
        }
        Map<String, TradeEntry> entries = new LinkedHashMap<>();
        //? if >=26.1 {
        /*var key = ResourceKey.create(Registries.VILLAGER_PROFESSION, profession);
        *///?}
        for (int tier = 1; tier <= 5; tier++) {
            //? if >=26.3 {
            /*List<TradeEntry> tierEntries = RegistryTrades26_3.list(level, key, tier);
            *///?} elif >=26.1 {
            /*List<TradeEntry> tierEntries = RegistryTrades.browse(level, key, tier);
            *///?} else {
            List<TradeEntry> tierEntries = ListingTrades.browse(level, profession, tier);
            //?}
            for (TradeEntry entry : tierEntries) {
                entries.putIfAbsent(entry.id, entry);
            }
        }
        return List.copyOf(entries.values());
    }

    /**
     * How many offers one level-up rolls. Classic villagers always roll
     * {@link VanillaRoll#OFFERS_PER_LEVEL}. 26.1+ reads the trade set amount.
     * Trade Rebalance is not applied here: the offer list already follows the
     * world's feature flag, and the odds use that same list.
     */
    public static int[] offerCounts(Level level, Villager villager, int villagerLevel) {
        //? if >=26.3 {
        /*return RegistryTrades26_3.offerCounts(level, villager, villagerLevel);
        *///?} else {
        //? if >=26.1 {
        /*return RegistryTrades.offerCounts(level, villager, villagerLevel);
        *///?} else {
        return new int[]{VanillaRoll.OFFERS_PER_LEVEL};
        //?}
        //?}
    }

    public static boolean allowDuplicates(Level level, Villager villager, int villagerLevel) {
        //? if >=26.3 {
        /*return RegistryTrades26_3.allowDuplicates(level, villager, villagerLevel);
        *///?} else {
        //? if >=26.1 {
        /*return RegistryTrades.allowDuplicates(level, villager, villagerLevel);
        *///?} else {
        return false;
        //?}
        //?}
    }
}
