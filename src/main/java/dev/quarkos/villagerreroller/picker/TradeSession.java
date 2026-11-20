package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/** The novice targets and emerald ceilings used by the active or next reroll. */
public final class TradeSession {
    public static final int ANY_PRICE = Integer.MAX_VALUE;

    private static String profession = "";
    private static TradeCatalog.BrowserCatalog catalog;
    private static Map<String, Integer> chosenMax = Map.of();
    private static boolean proofAcceptAny;

    private TradeSession() {
    }

    /** Drops the in-memory session while leaving saved picks on disk. */
    public static void simulateClientRestart() {
        profession = "";
        catalog = null;
        chosenMax = Map.of();
        proofAcceptAny = false;
    }

    static void rebuild(Level level, Villager villager) {
        String id = McBridge.professionId(villager);
        load(id, TradeCatalog.browse(level, Identifier.tryParse(id), villager),
                ProfessionPicks.load(PickFiles.configFile()).picks(id));
    }

    static void load(String professionId, TradeCatalog.BrowserCatalog current, Map<String, Integer> saved) {
        profession = professionId;
        catalog = current;
        restoreSelections(saved);
    }

    /** A Hall slot keeps its own ceilings, even if the profession's saved picks change. */
    static int restoreSelections(Map<String, Integer> saved) {
        chosenMax = catalog == null ? Map.of() : Map.copyOf(catalog.targets(saved));
        return chosenMax.size();
    }

    public static List<TradeEntry> entries() {
        return catalog == null ? List.of() : catalog.novice();
    }

    public static String profession() {
        return profession;
    }

    public static boolean hasSelection() {
        return !chosenMax.isEmpty();
    }

    public static boolean selected(String id) {
        return chosenMax.containsKey(id);
    }

    public static int chosenMax(String id) {
        return chosenMax.getOrDefault(id, 0);
    }

    public static Map<String, Integer> selections() {
        return chosenMax;
    }

    /** The dedicated-server proof stops on whatever offer is open, which is the lock-in path. */
    public static void harnessLockAny() {
        proofAcceptAny = true;
    }

    public static boolean offerMatchesSelection(MerchantOffers offers) {
        if (proofAcceptAny && offers != null && !offers.isEmpty()) {
            return true;
        }
        return matches(offers, entries(), chosenMax);
    }

    static boolean matches(MerchantOffers offers, List<TradeEntry> entries, Map<String, Integer> targets) {
        if (offers == null || targets.isEmpty()) {
            return false;
        }
        for (MerchantOffer offer : offers) {
            for (TradeEntry entry : entries) {
                Integer max = targets.get(entry.id);
                if (max != null && entry.matches(offer, max)) {
                    return true;
                }
            }
        }
        return false;
    }
}
