package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.TradeOdds;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Targets chosen in the merchant panel. The map value is the emerald ceiling,
 * or {@link Integer#MAX_VALUE} for any price.
 */
public final class TradeSession {
    public static final int ANY_PRICE = Integer.MAX_VALUE;

    private static boolean panelOpen;
    private static String filter = "";
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

    public static void rebuild(Level level, Villager villager, MerchantMenu menu) {
        int villagerLevel = menu == null ? 1 : menu.getTraderLevel();
        entries = TradeCatalog.list(level, villager, villagerLevel);
        pool = TradePools.from(entries, TradeCatalog.offerCounts(level, villager, villagerLevel), TradeCatalog.allowDuplicates(level, villager, villagerLevel));
        cachedEstimate = null;
        chosenMax.keySet().removeIf(id -> entries.stream().noneMatch(entry -> entry.id.equals(id)));
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
    }

    public static void cyclePrice(TradeEntry entry, int direction) {
        if (!entry.paysEmeralds || entry.emeraldMin == entry.emeraldMax) {
            toggle(entry);
            return;
        }
        if (!chosenMax.containsKey(entry.id)) {
            chosenMax.put(entry.id, direction < 0 ? ANY_PRICE : entry.emeraldMin);
            return;
        }
        int current = chosenMax.get(entry.id);
        if (current == ANY_PRICE) {
            chosenMax.put(entry.id, direction < 0 ? entry.emeraldMax : entry.emeraldMin);
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
}
