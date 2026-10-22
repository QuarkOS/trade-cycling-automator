package dev.quarkos.villagerreroller.price;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianBookPricesTest {
    private static final Set<String> NOT_SOLD = Set.of(
            "minecraft:soul_speed",
            "minecraft:swift_sneak",
            "minecraft:wind_burst"
    );

    @Test
    void rangesMatchTheWikiFormulaAfterTheEmeraldCap() {
        LibrarianBookPrices prices = LibrarianBookPrices.load(predicate -> true);
        for (int level = 1; level <= 5; level++) {
            assertEquals(capped(level, false), prices.normalPrices().get(level));
            assertEquals(capped(level, true), prices.treasurePrices().get(level));
        }
    }

    @Test
    void rejectsLevelsAndPricesOutsideTheWiki() {
        LibrarianBookPrices prices = LibrarianBookPrices.load(predicate -> true);

        assertTrue(prices.validate("minecraft:mending", 1, 10).isEmpty());
        assertTrue(prices.validate("minecraft:mending", 1, 38).isEmpty());
        assertEquals(LibrarianBookPrices.ProblemKind.LEVEL_TOO_HIGH, prices.validate("minecraft:mending", 2, 16).orElseThrow().kind());
        assertEquals(LibrarianBookPrices.ProblemKind.PRICE_BELOW_WIKI_MIN, prices.validate("minecraft:mending", 1, 9).orElseThrow().kind());
        assertEquals(LibrarianBookPrices.ProblemKind.PRICE_ABOVE_WIKI_MAX, prices.validate("minecraft:mending", 1, 39).orElseThrow().kind());

        assertTrue(prices.validate("minecraft:efficiency", 5, 64).isEmpty());
        assertEquals(64, prices.validate("minecraft:efficiency", 5, 71).orElseThrow().limit());
        assertEquals(LibrarianBookPrices.ProblemKind.PRICE_ABOVE_WIKI_MAX, prices.validate("minecraft:sharpness", 5, 71).orElseThrow().kind());

        assertTrue(prices.validate("minecraft:protection", 4, 14).isEmpty());
        assertEquals(LibrarianBookPrices.ProblemKind.PRICE_BELOW_WIKI_MIN, prices.validate("minecraft:protection", 4, 13).orElseThrow().kind());
        assertEquals(LibrarianBookPrices.ProblemKind.LEVEL_TOO_LOW, prices.validate("minecraft:protection", 0, 14).orElseThrow().kind());
    }

    @Test
    void librariansDoNotSellEveryTreasureEnchantment() {
        LibrarianBookPrices prices = LibrarianBookPrices.load(predicate -> true);
        for (String id : NOT_SOLD) {
            assertTrue(prices.enchantment(id).isEmpty(), id);
        }
        assertTrue(prices.enchantment("minecraft:mending").orElseThrow().treasure());
        assertTrue(prices.enchantment("minecraft:frost_walker").orElseThrow().treasure());
        assertTrue(prices.enchantment("minecraft:binding_curse").orElseThrow().treasure());
        assertTrue(prices.enchantment("minecraft:vanishing_curse").orElseThrow().treasure());
        assertFalse(prices.enchantment("minecraft:lunge").orElseThrow().treasure());
        assertEquals(3, prices.enchantment("minecraft:lunge").orElseThrow().maxLevel());
    }

    @Test
    void lungeIsOnlyListedWhenTheVersionPredicateMatches() {
        LibrarianBookPrices withoutLunge = LibrarianBookPrices.load(predicate -> false);
        assertTrue(withoutLunge.enchantment("minecraft:lunge").isEmpty());
        assertTrue(withoutLunge.enchantment("minecraft:mending").isPresent());

        LibrarianBookPrices withLunge = LibrarianBookPrices.load(predicate -> true);
        assertTrue(withLunge.enchantment("minecraft:lunge").isPresent());
        assertEquals(40, withLunge.enchantments().size());
        assertEquals(39, withoutLunge.enchantments().size());
    }

    private static LibrarianBookPrices.PriceRange capped(int level, boolean treasure) {
        int min = 2 + 3 * level;
        int max = 6 + 13 * level;
        if (treasure) {
            min *= 2;
            max *= 2;
        }
        return new LibrarianBookPrices.PriceRange(Math.min(64, min), Math.min(64, max));
    }
}
