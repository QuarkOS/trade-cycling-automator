package dev.quarkos.villagerreroller.odds;

/**
 * Constants shared by the live trade reader and the odds tests.
 *
 * <p>Before trade sets (1.19 through 1.21.11), {@code Villager.updateTrades}
 * always asks {@code addOffersFromItemListings} for 2 offers. 26.1 and newer
 * read the trade set's {@code amount} instead, and fall back to this same 2
 * when that provider is not a constant or an inclusive uniform range.
 */
public final class VanillaRoll {
    public static final int OFFERS_PER_LEVEL = 2;

    /**
     * Librarian books are a uniform draw across the listing's tradeable tag
     * ({@code nextInt(list.size())} before 1.21, {@code getRandomElementOf}
     * after). That is not {@code Enchantment#getWeight()}, which is the
     * enchanting-table weight.
     */
    public static final int UNIFORM_ENCHANTMENT_WEIGHT = 1;

    /** Trade Rebalance is an experiment. Worlds leave it off unless the flag is on. */
    public static final boolean TRADE_REBALANCE_DEFAULT = false;

    private VanillaRoll() {
    }

    public static int[] inclusive(int min, int max) {
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        int span = max - min;
        if (span > 16) {
            return new int[]{min, (min + max) / 2, max};
        }
        int[] values = new int[span + 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = min + i;
        }
        return values;
    }
}
