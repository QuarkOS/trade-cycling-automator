package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import net.minecraft.network.chat.Component;

/**
 * Job-block rerolls are the default. Trade Cycling's packet is available only
 * when that mod is loaded, and only after the player picks it.
 */
public final class RerollMethod {
    public enum Kind {
        JOB_SITE,
        TRADE_CYCLING
    }

    private static Kind selected = Kind.JOB_SITE;

    private RerollMethod() {
    }

    public static void reset() {
        selected = Kind.JOB_SITE;
    }

    public static Kind selected() {
        if (selected == Kind.TRADE_CYCLING && !canChoose()) {
            selected = Kind.JOB_SITE;
        }
        return selected;
    }

    public static boolean usesTradeCycling() {
        return selected() == Kind.TRADE_CYCLING;
    }

    /** The client jar is present, so the toggle can be drawn. */
    public static boolean showToggle() {
        return TradeCyclingHook.installed();
    }

    /** The connected server advertised the cycle channel. */
    public static boolean canChoose() {
        return TradeCyclingHook.serverSupports();
    }

    public static Component unsupportedTooltip() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".method.unsupported");
    }

    public static void toggle() {
        if (!canChoose() || JobSiteReroller.isRunning()) {
            return;
        }
        selected = usesTradeCycling() ? Kind.JOB_SITE : Kind.TRADE_CYCLING;
    }

    public static Component label() {
        if (showToggle() && !canChoose()) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".method.trade_cycling");
        }
        String key = usesTradeCycling() ? "method.trade_cycling" : "method.job_site";
        return Component.translatable("text." + VillagerReroller.MOD_ID + "." + key);
    }
}
