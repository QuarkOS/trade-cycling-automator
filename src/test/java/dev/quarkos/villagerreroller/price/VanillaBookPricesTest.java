package dev.quarkos.villagerreroller.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaBookPricesTest {
    @Test
    void bookRangeMatchesTheVanillaRollThenTheEmeraldCap() {
        for (int level = 1; level <= 5; level++) {
            for (boolean treasure : new boolean[]{false, true}) {
                int lowRoll = 0;
                int highRoll = 5 + level * 10 - 1;
                int min = 2 + lowRoll + 3 * level;
                int max = 2 + highRoll + 3 * level;
                if (treasure) {
                    min *= 2;
                    max *= 2;
                }
                assertEquals(Math.min(64, min), VanillaBookPrices.min(level, treasure), "min " + level + " treasure=" + treasure);
                assertEquals(Math.min(64, max), VanillaBookPrices.max(level, treasure), "max " + level + " treasure=" + treasure);
                assertEquals(max, VanillaBookPrices.uncappedMax(level, treasure));
            }
        }
    }

    @Test
    void cappedTableMatchesTheWikiAfterTheEmeraldCap() {
        assertEquals(5, VanillaBookPrices.min(1, false));
        assertEquals(19, VanillaBookPrices.max(1, false));
        assertEquals(8, VanillaBookPrices.min(2, false));
        assertEquals(32, VanillaBookPrices.max(2, false));
        assertEquals(11, VanillaBookPrices.min(3, false));
        assertEquals(45, VanillaBookPrices.max(3, false));
        assertEquals(14, VanillaBookPrices.min(4, false));
        assertEquals(58, VanillaBookPrices.max(4, false));
        assertEquals(17, VanillaBookPrices.min(5, false));
        assertEquals(64, VanillaBookPrices.max(5, false));
        assertEquals(71, VanillaBookPrices.uncappedMax(5, false));

        assertEquals(10, VanillaBookPrices.min(1, true));
        assertEquals(38, VanillaBookPrices.max(1, true));
        assertEquals(16, VanillaBookPrices.min(2, true));
        assertEquals(64, VanillaBookPrices.max(2, true));
        assertEquals(22, VanillaBookPrices.min(3, true));
        assertEquals(64, VanillaBookPrices.max(3, true));
        assertEquals(90, VanillaBookPrices.uncappedMax(3, true));
        assertEquals(28, VanillaBookPrices.min(4, true));
        assertEquals(64, VanillaBookPrices.max(4, true));
        assertEquals(116, VanillaBookPrices.uncappedMax(4, true));
        assertEquals(34, VanillaBookPrices.min(5, true));
        assertEquals(64, VanillaBookPrices.max(5, true));
        assertEquals(142, VanillaBookPrices.uncappedMax(5, true));
    }

    @Test
    void tradeRebalanceSpecialBooksUseTheSameFormula() {
        // Desert efficiency III, plains protection III, savanna sharpness III.
        assertEquals(11, VanillaBookPrices.min(3, false));
        assertEquals(45, VanillaBookPrices.max(3, false));
        // Jungle unbreaking II and taiga fortune II.
        assertEquals(8, VanillaBookPrices.min(2, false));
        assertEquals(32, VanillaBookPrices.max(2, false));
        // Snow silk touch, a single-level book.
        assertEquals(5, VanillaBookPrices.min(1, false));
        assertEquals(19, VanillaBookPrices.max(1, false));
        // Swamp mending is treasure, so the level-I cost is doubled.
        assertEquals(10, VanillaBookPrices.min(1, true));
        assertEquals(38, VanillaBookPrices.max(1, true));
    }

    @Test
    void enchantedGearAddsFiveThroughNineteenThenCaps() {
        // Fletcher bow base is 2: 7–21 emeralds.
        assertEquals(7, VanillaBookPrices.enchantedGearMin(2));
        assertEquals(21, VanillaBookPrices.enchantedGearMax(2));
        assertEquals(64, VanillaBookPrices.enchantedGearMax(50));
        assertEquals(55, VanillaBookPrices.enchantedGearMin(50));
    }
}
