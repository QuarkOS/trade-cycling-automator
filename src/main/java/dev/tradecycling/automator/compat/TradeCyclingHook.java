package dev.tradecycling.automator.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Presence check for Trade Cycling. The classes that call into that mod are
 * loaded only after {@link #installed()} is true, so a missing jar does not
 * crash this one.
 */
public final class TradeCyclingHook {
    public static final String MOD_ID = "trade_cycling";

    private TradeCyclingHook() {
    }

    public static boolean installed() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean canCycle(MerchantMenu menu) {
        if (!installed() || menu == null) {
            return false;
        }
        return TradeCyclingActions.canCycle(menu);
    }

    public static boolean cycle(MerchantMenu menu) {
        if (!installed() || menu == null) {
            return false;
        }
        return TradeCyclingActions.cycle(menu);
    }
}
