package dev.quarkos.villagerreroller.picker;

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
}
