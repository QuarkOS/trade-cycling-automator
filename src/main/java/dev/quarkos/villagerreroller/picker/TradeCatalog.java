package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.VanillaRoll;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

import java.util.List;

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
