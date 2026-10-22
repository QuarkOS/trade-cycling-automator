package dev.quarkos.villagerreroller.price;

/**
 * Emerald bounds from {@code VillagerTrades.EnchantBookForEmeralds} and
 * {@code EnchantedItemForEmeralds} in Minecraft 1.21.11.
 *
 * <p>A book costs {@code 2 + random.nextInt(5 + level * 10) + 3 * level}.
 * {@code nextInt} is {@code 0} inclusive and {@code n} exclusive, so the roll is
 * {@code 0} through {@code 4 + level * 10}. Treasure enchantments
 * ({@code #minecraft:double_trade_price}) double that result. The offer then
 * clamps at 64 emeralds. Trade Rebalance picks the enchantment and its level
 * differently, then uses this same cost.
 *
 * <p>Enchanted gear adds {@code 5 + random.nextInt(15)} (5 through 19) to the
 * base cost and clamps at 64.
 */
public final class VanillaBookPrices {
    public static final int EMERALD_CAP = 64;

    private VanillaBookPrices() {
    }

    public static int min(int level, boolean treasure) {
        return cap(rawMin(level) * (treasure ? 2 : 1));
    }

    public static int max(int level, boolean treasure) {
        return cap(rawMax(level) * (treasure ? 2 : 1));
    }

    /**
     * Highest cost the roll can produce before the 64 emerald clamp.
     */
    public static int uncappedMax(int level, boolean treasure) {
        return rawMax(level) * (treasure ? 2 : 1);
    }

    public static int enchantedGearMin(int baseEmeraldCost) {
        return cap(baseEmeraldCost + 5);
    }

    public static int enchantedGearMax(int baseEmeraldCost) {
        return cap(baseEmeraldCost + 19);
    }

    private static int rawMin(int level) {
        return 2 + 3 * level;
    }

    private static int rawMax(int level) {
        return 2 + (5 + level * 10 - 1) + 3 * level;
    }

    private static int cap(int emeralds) {
        return Math.min(EMERALD_CAP, emeralds);
    }
}
