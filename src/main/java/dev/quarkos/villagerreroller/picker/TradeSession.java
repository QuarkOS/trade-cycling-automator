package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.odds.TradeOdds;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Targets chosen in the merchant panel for the villager's current profession.
 * The map value is the emerald ceiling, or {@link Integer#MAX_VALUE} for any price.
 * Picks are written to {@code config/villager_reroller-picks.json} and loaded again
 * when that profession's panel opens, including after a client restart.
 */
public final class TradeSession {
    public static final int ANY_PRICE = Integer.MAX_VALUE;

    private static boolean panelOpen;
    private static String filter = "";
    private static String profession = "";
    private static List<TradeEntry> entries = List.of();
    private static TradeOdds.Pool pool = TradeOdds.Pool.EMPTY;
    private static final Map<String, Integer> chosenMax = new LinkedHashMap<>();
    private static TradeOdds.Estimate cachedEstimate;
    private static int cachedEstimateKey;

    private TradeSession() {
    }

    public static boolean panelOpen() {
        return panelOpen;
    }

    public static void setPanelOpen(boolean open) {
        panelOpen = open;
    }

    public static String filter() {
        return filter;
    }

    public static void setFilter(String value) {
        filter = value == null ? "" : value;
    }

    public static List<TradeEntry> entries() {
        return entries;
    }

    public static String profession() {
        return profession;
    }

    /**
     * Drops the in-memory session the way a new client process would.
     * The picks file is left in place so the next panel open can restore it.
     */
    public static void simulateClientRestart() {
        panelOpen = false;
        filter = "";
        profession = "";
        entries = List.of();
        chosenMax.clear();
    }

    public static void rebuild(Level level, Villager villager, MerchantMenu menu) {
        int villagerLevel = menu == null ? 1 : menu.getTraderLevel();
        String next = villager == null ? "" : McBridge.professionId(villager);
        if (known(profession) && !profession.equals(next)) {
            persist();
        }
        entries = TradeCatalog.list(level, villager, villagerLevel);
        pool = TradePools.from(entries, TradeCatalog.offerCounts(level, villager, villagerLevel), TradeCatalog.allowDuplicates(level, villager, villagerLevel));
        cachedEstimate = null;
        profession = known(next) ? next : "";
        chosenMax.clear();
        if (!known(profession) || entries.isEmpty()) {
            return;
        }
        Map<String, Integer> saved = ProfessionPicks.load(PickFiles.configFile()).picks(profession);
        for (TradeEntry entry : entries) {
            Integer max = saved.get(entry.id);
            if (max == null) {
                continue;
            }
            int fitted = ProfessionPicks.fitPrice(max, entry.emeraldMin, entry.emeraldMax, entry.paysEmeralds);
            if (fitted > 0) {
                chosenMax.put(entry.id, fitted);
            }
        }
    }

    public static TradeOdds.Pool pool() {
        return pool;
    }

    /**
     * Odds for the current selection at the chosen ceilings. Empty when nothing is selected.
     */
    public static TradeOdds.Estimate estimate(int ticksPerAttempt) {
        if (chosenMax.isEmpty() || pool.listings().isEmpty()) {
            return null;
        }
        int key = Objects.hash(chosenMax, pool, ticksPerAttempt);
        if (cachedEstimate != null && cachedEstimateKey == key) {
            return cachedEstimate;
        }
        cachedEstimateKey = key;
        cachedEstimate = TradeOdds.estimate(pool, Map.copyOf(chosenMax), ticksPerAttempt);
        return cachedEstimate;
    }

    public static void clearSelection() {
        chosenMax.clear();
        cachedEstimate = null;
    }

    public static List<TradeEntry> visible() {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return entries;
        }
        List<TradeEntry> matched = new ArrayList<>();
        for (TradeEntry entry : entries) {
            if (entry.name.toLowerCase(Locale.ROOT).contains(needle)) {
                matched.add(entry);
            }
        }
        return matched;
    }

    public static boolean selected(String id) {
        return chosenMax.containsKey(id);
    }

    public static int chosenMax(String id) {
        return chosenMax.getOrDefault(id, 0);
    }

    public static boolean anyPrice(String id) {
        Integer value = chosenMax.get(id);
        return value != null && value == ANY_PRICE;
    }

    public static Map<String, Integer> selections() {
        return Map.copyOf(chosenMax);
    }

    public static boolean hasSelection() {
        return !chosenMax.isEmpty();
    }

    public static void toggle(TradeEntry entry) {
        if (chosenMax.containsKey(entry.id)) {
            chosenMax.remove(entry.id);
        } else {
            chosenMax.put(entry.id, entry.paysEmeralds ? entry.emeraldMin : ANY_PRICE);
        }
        persist();
    }

    public static void cyclePrice(TradeEntry entry, int direction) {
        if (!entry.paysEmeralds || entry.emeraldMin == entry.emeraldMax) {
            toggle(entry);
            return;
        }
        if (!chosenMax.containsKey(entry.id)) {
            chosenMax.put(entry.id, direction < 0 ? ANY_PRICE : entry.emeraldMin);
            persist();
            return;
        }
        int current = chosenMax.get(entry.id);
        if (current == ANY_PRICE) {
            chosenMax.put(entry.id, direction < 0 ? entry.emeraldMax : entry.emeraldMin);
            persist();
            return;
        }
        int next = current + direction;
        if (next > entry.emeraldMax) {
            chosenMax.put(entry.id, ANY_PRICE);
        } else if (next < entry.emeraldMin) {
            chosenMax.put(entry.id, ANY_PRICE);
        } else {
            chosenMax.put(entry.id, next);
        }
        persist();
    }

    /**
     * Selects every catalog row that is not already on the counter, at any price.
     * A novice librarian rolls two of three listings, so the missing one shows up
     * on the next different roll.
     */
    public static int selectAllExcept(MerchantOffers offers) {
        chosenMax.clear();
        for (TradeEntry entry : entries) {
            if (!offered(entry, offers)) {
                chosenMax.put(entry.id, ANY_PRICE);
            }
        }
        persist();
        return chosenMax.size();
    }

    public static boolean offerMatchesSelection(MerchantOffers offers) {
        if (offers == null || chosenMax.isEmpty()) {
            return false;
        }
        for (MerchantOffer offer : offers) {
            for (TradeEntry entry : entries) {
                Integer max = chosenMax.get(entry.id);
                if (max != null && entry.matches(offer, max)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean offered(TradeEntry entry, MerchantOffers offers) {
        if (offers == null) {
            return false;
        }
        for (MerchantOffer offer : offers) {
            if (entry.matches(offer, ANY_PRICE)) {
                return true;
            }
        }
        return false;
    }

    private static void persist() {
        if (!known(profession) || entries.isEmpty()) {
            return;
        }
        Set<String> catalog = new LinkedHashSet<>();
        for (TradeEntry entry : entries) {
            catalog.add(entry.id);
        }
        ProfessionPicks picks = ProfessionPicks.load(PickFiles.configFile());
        picks.replaceCatalog(profession, catalog, chosenMax);
        picks.save(PickFiles.configFile());
    }

    private static boolean known(String id) {
        return id != null && !id.isEmpty() && !"unknown".equals(id);
    }
}
