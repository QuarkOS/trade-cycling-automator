package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.VanillaRoll;
import net.minecraft.resources.Identifier;
//? if >=26.1 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** The first appearance of each trade, plus the actual listings used for novice odds. */
    static BrowserCatalog browse(Level level, Identifier profession, Villager villager) {
        if (level == null || profession == null) {
            return new BrowserCatalog(Map.of(), List.of(), new int[0], false);
        }
        Map<String, Offer> entries = new LinkedHashMap<>();
        List<TradeEntry> novice = List.of();
        //? if >=26.1 {
        /*var key = ResourceKey.create(Registries.VILLAGER_PROFESSION, profession);
        *///?}
        for (int tier = 1; tier <= 5; tier++) {
            //? if >=26.3 {
            /*List<TradeEntry> tierEntries = RegistryTrades26_3.list(level, key, tier, villager);
            *///?} elif >=26.1 {
            /*List<TradeEntry> tierEntries = RegistryTrades.list(level, key, tier, villager);
            *///?} else {
            List<TradeEntry> tierEntries = villager == null ? ListingTrades.browse(level, profession, tier)
                    : ListingTrades.list(level, villager, tier);
            //?}
            if (tier == 1) {
                novice = tierEntries;
            }
            for (TradeEntry entry : tierEntries) {
                entries.putIfAbsent(entry.id, new Offer(entry, new RerollTargets.Offer(
                        RerollEligibility.of(tier), entry.emeraldMin, entry.emeraldMax, entry.paysEmeralds)));
            }
        }
        //? if >=26.3 {
        /*return new BrowserCatalog(entries, novice, RegistryTrades26_3.offerCounts(level, key, 1),
                RegistryTrades26_3.allowDuplicates(level, key, 1));
        *///?} elif >=26.1 {
        /*return new BrowserCatalog(entries, novice, RegistryTrades.offerCounts(level, key, 1),
                RegistryTrades.allowDuplicates(level, key, 1));
        *///?} else {
        return new BrowserCatalog(entries, novice, new int[]{VanillaRoll.OFFERS_PER_LEVEL}, false);
        //?}
    }

    record Offer(TradeEntry entry, RerollTargets.Offer rule) {
        RerollEligibility eligibility() {
            return rule.eligibility();
        }
    }

    record BrowserCatalog(Map<String, Offer> offers, List<TradeEntry> novice, int[] offerCounts, boolean allowDuplicates) {
        BrowserCatalog {
            offers = Collections.unmodifiableMap(offers);
        }

        Map<String, Integer> targets(Map<String, Integer> saved) {
            return RerollTargets.resolve(saved, id -> {
                Offer offer = offers.get(id);
                return offer == null ? null : offer.rule();
            });
        }
    }
}
