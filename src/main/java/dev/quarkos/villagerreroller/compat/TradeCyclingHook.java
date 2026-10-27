package dev.quarkos.villagerreroller.compat;

//? if forge {
/*import net.minecraftforge.fml.ModList;
*///?} else {
import net.fabricmc.loader.api.FabricLoader;
//?}
import net.minecraft.world.inventory.MerchantMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presence check for Trade Cycling. The classes that call into that mod are
 * loaded only after {@link #installed()} is true, so a missing jar does not
 * crash this one. {@link #serverSupports()} also requires the connected server
 * to advertise the cycle channel, so the packet is never sent to a server that
 * would disconnect over it.
 */
public final class TradeCyclingHook {
    public static final String MOD_ID = "trade_cycling";
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static boolean loggedQueryFailure;

    private TradeCyclingHook() {
    }

    public static boolean installed() {
        //? if forge {
        /*//? if >=26.1 {
        return ModList.isLoaded(MOD_ID);
        //?} else {
        return ModList.get().isLoaded(MOD_ID);
        //?}
        *///?} else {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
        //?}
    }

    public static boolean serverSupports() {
        if (!installed()) {
            return false;
        }
        try {
            return TradeCyclingActions.serverHasChannel();
        } catch (Throwable error) {
            if (!loggedQueryFailure) {
                loggedQueryFailure = true;
                LOGGER.warn("Could not query the Trade Cycling channel", error);
            }
            return false;
        }
    }

    public static boolean canCycle(MerchantMenu menu) {
        if (!serverSupports() || menu == null) {
            return false;
        }
        return TradeCyclingActions.canCycle(menu);
    }

    public static boolean cycle(MerchantMenu menu) {
        if (!serverSupports() || menu == null) {
            return false;
        }
        return TradeCyclingActions.cycle(menu);
    }
}
