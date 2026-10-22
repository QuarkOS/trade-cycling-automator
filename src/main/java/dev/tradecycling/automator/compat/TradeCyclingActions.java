package dev.tradecycling.automator.compat;

import de.maxhenkel.tradecycling.FabricTradeCyclingClientMod;
import de.maxhenkel.tradecycling.gui.CycleTradesButton;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Calls Trade Cycling's own cycle action. Do not load this class unless
 * {@link TradeCyclingHook#installed()} is true.
 */
final class TradeCyclingActions {
    private TradeCyclingActions() {
    }

    static boolean canCycle(MerchantMenu menu) {
        return CycleTradesButton.canCycle(menu);
    }

    static boolean cycle(MerchantMenu menu) {
        if (!CycleTradesButton.canCycle(menu)) {
            return false;
        }
        FabricTradeCyclingClientMod mod = FabricTradeCyclingClientMod.instance();
        if (mod == null) {
            return false;
        }
        mod.sendCycleTradesPacket();
        return true;
    }
}
