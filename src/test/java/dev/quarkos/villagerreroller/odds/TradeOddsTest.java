package dev.quarkos.villagerreroller.odds;

import dev.quarkos.villagerreroller.price.VanillaBookPrices;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The closed form is checked against a direct Monte Carlo of the vanilla
 * generation steps in {@link TradeOdds#rollOnce}: draw listings the way
 * {@code addOffersFromItemListings} does, then {@code nextInt} for the
 * enchantment weight, the level, and the emerald price.
 */
class TradeOddsTest {
    private static final int TRIALS = 40_000;
    private static final long SEED = 0x51A11E70L;

    @Test
    void twoOfThreeListingsIncludeAFixedTradeTwoThirdsOfTheTime() {
        TradeOdds.Pool pool = novice(book("minecraft:mending", 1, true, 1));
        assertEquals(2.0 / 3.0, TradeOdds.hitProbability(pool, target("buy:minecraft:bookshelf:1:9", Integer.MAX_VALUE)), 1e-12);
        assertEquals(2.0 / 3.0, TradeOdds.hitProbability(pool, target("buy:minecraft:paper:24:24", Integer.MAX_VALUE)), 1e-12);
        assertEquals(0, TradeOdds.hitProbability(pool, target("buy:minecraft:bookshelf:1:9", 8)), 1e-12);
    }

    @Test
    void enchantmentWeightThenUniformLevelThenThePriceFormula() {
        TradeOdds.Pool pool = novice(
                book("minecraft:mending", 1, true, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT),
                book("minecraft:efficiency", 1, false, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT),
                book("minecraft:efficiency", 2, false, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT),
                book("minecraft:efficiency", 3, false, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT),
                book("minecraft:efficiency", 4, false, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT),
                book("minecraft:efficiency", 5, false, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT)
        );
        // The book listing is one of three and two are drawn. Mending is one of two
        // enchantments and has a single level. Only the minimum treasure roll (10) counts.
        double expected = (2.0 / 3.0) * (1.0 / 2.0) * (1.0 / 15.0);
        assertEquals(expected, TradeOdds.hitProbability(pool, target("enchant:minecraft:mending:1", 10)), 1e-12);
        assertEquals(2.0 / 3.0 * 0.5 * 0.2, TradeOdds.hitProbability(pool, target("enchant:minecraft:efficiency:5", Integer.MAX_VALUE)), 1e-12);
    }

    @Test
    void anyOfTargetsOnDifferentListingsAndTheSameBookListing() {
        TradeOdds.Pool pool = novice(
                book("minecraft:mending", 1, true, 1),
                book("minecraft:efficiency", 1, false, 1)
        );
        Map<String, Integer> eitherBook = new LinkedHashMap<>();
        eitherBook.put("enchant:minecraft:mending:1", Integer.MAX_VALUE);
        eitherBook.put("enchant:minecraft:efficiency:1", Integer.MAX_VALUE);
        // The book listing always produces one of the two, and it is drawn 2/3 of the time.
        assertEquals(2.0 / 3.0, TradeOdds.hitProbability(pool, eitherBook), 1e-12);

        Map<String, Integer> paperOrMending = new LinkedHashMap<>();
        paperOrMending.put("buy:minecraft:paper:24:24", Integer.MAX_VALUE);
        paperOrMending.put("enchant:minecraft:mending:1", Integer.MAX_VALUE);
        // paper+shelf hits, paper+book hits, shelf+book hits only when the book is mending.
        assertEquals(5.0 / 6.0, TradeOdds.hitProbability(pool, paperOrMending), 1e-12);
    }

    @Test
    void heavierEnchantmentBranchesAreMoreLikely() {
        TradeOdds.RolledRow common = book("minecraft:protection", 1, false, 10);
        TradeOdds.RolledRow rare = book("minecraft:mending", 1, true, 2);
        TradeOdds.Pool pool = TradeOdds.pool(List.of(common, rare), new int[]{1}, false);
        assertEquals(10.0 / 12.0, TradeOdds.hitProbability(pool, target("enchant:minecraft:protection:1", Integer.MAX_VALUE)), 1e-12);
        assertEquals(2.0 / 12.0, TradeOdds.hitProbability(pool, target("enchant:minecraft:mending:1", Integer.MAX_VALUE)), 1e-12);
        // Vanilla trades do not use these table weights. The live reader passes weight 1.
        assertEquals(1, VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT);
    }

    @Test
    void bookPriceMatchesTheVanillaRollIncludingTheEmeraldCap() {
        for (int level = 1; level <= 5; level++) {
            for (boolean treasure : new boolean[]{false, true}) {
                int min = VanillaBookPrices.min(level, treasure);
                int max = VanillaBookPrices.max(level, treasure);
                assertEquals(0, TradeOdds.bookAtMost(level, treasure, min - 1), "below min " + level);
                assertTrue(TradeOdds.bookAtMost(level, treasure, min) > 0);
                assertEquals(1, TradeOdds.bookAtMost(level, treasure, max), "at max " + level);
                assertEquals(1, TradeOdds.bookAtMost(level, treasure, Integer.MAX_VALUE));
            }
        }
        // Treasure III raw costs are 22, 24, ... and 14 of the 35 rolls clamp to 64.
        // A uniform draw over the displayed 22–64 range would not leave 21/35 at 63 or below.
        assertEquals(21.0 / 35.0, TradeOdds.bookAtMost(3, true, 63), 1e-12);
        assertEquals(1.0 / 15.0, TradeOdds.bookAtMost(1, true, 10), 1e-12);
    }

    @Test
    void enchantedGearUsesFiveThroughNineteenNotTheDisplayedRange() {
        TradeOdds.RolledRow bow = new TradeOdds.RolledRow(
                "gear:minecraft:bow:2",
                7,
                21,
                TradeOdds.Roll.gear("gear", 2)
        );
        TradeOdds.Pool pool = TradeOdds.pool(List.of(bow), new int[]{1}, false);
        assertEquals(1.0 / 15.0, TradeOdds.hitProbability(pool, target("gear:minecraft:bow:2", 7)), 1e-12);
        assertEquals(1, TradeOdds.hitProbability(pool, target("gear:minecraft:bow:2", 21)), 1e-12);
        // Base 50 clamps several of the 15 rolls onto 64, so 64 is more likely than 55.
        assertTrue(TradeOdds.gearAtMost(50, 64) - TradeOdds.gearAtMost(50, 63) > 1.0 / 15.0);
    }

    @Test
    void duplicateRollsAndARangeOfOfferCounts() {
        TradeOdds.RolledRow hit = fixed("yes", 1);
        TradeOdds.RolledRow miss = fixed("no", 1);
        TradeOdds.Pool replaced = TradeOdds.pool(List.of(hit, miss), new int[]{2}, true);
        TradeOdds.Pool distinct = TradeOdds.pool(List.of(hit, miss), new int[]{2}, false);
        assertEquals(0.75, TradeOdds.hitProbability(replaced, target("yes", 1)), 1e-12);
        assertEquals(1, TradeOdds.hitProbability(distinct, target("yes", 1)), 1e-12);

        TradeOdds.Pool mixedCount = TradeOdds.pool(List.of(hit, miss), new int[]{1, 2}, false);
        assertEquals(0.75, TradeOdds.hitProbability(mixedCount, target("yes", 1)), 1e-12);
    }

    @Test
    void effectsOnOneListingMatchTogether() {
        TradeOdds.RolledRow night = new TradeOdds.RolledRow("stew:night", 1, 1, TradeOdds.Roll.together("stew"));
        TradeOdds.RolledRow jump = new TradeOdds.RolledRow("stew:jump", 1, 1, TradeOdds.Roll.together("stew"));
        TradeOdds.Pool pool = TradeOdds.pool(List.of(night, jump), new int[]{1}, false);
        assertEquals(1, TradeOdds.hitProbability(pool, target("stew:night", 1)), 1e-12);
        assertEquals(1, TradeOdds.hitProbability(pool, target("stew:jump", 1)), 1e-12);
    }

    @Test
    void closedFormMatchesAMonteCarloOfTheVanillaLoop() {
        TradeOdds.Pool pool = novice(
                book("minecraft:mending", 1, true, 1),
                book("minecraft:unbreaking", 1, false, 1),
                book("minecraft:unbreaking", 2, false, 1),
                book("minecraft:unbreaking", 3, false, 1)
        );
        Map<String, Integer> mendingMin = target("enchant:minecraft:mending:1", 10);
        agree(pool, mendingMin);

        Map<String, Integer> anyOf = new LinkedHashMap<>();
        anyOf.put("buy:minecraft:paper:24:24", Integer.MAX_VALUE);
        anyOf.put("enchant:minecraft:unbreaking:3", 45);
        agree(pool, anyOf);

        TradeOdds.Pool weighted = TradeOdds.pool(List.of(
                book("minecraft:protection", 1, false, 10),
                book("minecraft:mending", 1, true, 2)
        ), new int[]{1}, false);
        agree(weighted, target("enchant:minecraft:mending:1", 38));

        TradeOdds.RolledRow bow = new TradeOdds.RolledRow("gear:minecraft:bow:2", 7, 21, TradeOdds.Roll.gear("gear", 2));
        agree(TradeOdds.pool(List.of(bow, fixed("stick", 1)), new int[]{1}, false), target("gear:minecraft:bow:2", 10));
    }

    @Test
    void labelsRoundTheReciprocalAndUseTheAttemptPace() {
        assertEquals(2, VanillaRoll.OFFERS_PER_LEVEL);
        assertEquals(false, VanillaRoll.TRADE_REBALANCE_DEFAULT);
        TradeOdds.Pool pool = novice(book("minecraft:mending", 1, true, 1));
        TradeOdds.Estimate estimate = TradeOdds.estimate(pool, target("enchant:minecraft:mending:1", Integer.MAX_VALUE), AttemptPace.JOB_SITE_TICKS);
        // 1 / (2/3) rounds to 2 attempts. 2 * 140 ticks is 14 seconds.
        assertEquals(2, estimate.oneIn());
        assertEquals("2", estimate.oneInText());
        assertEquals("14 sec", estimate.timeText());
        assertEquals("7 sec", TradeOdds.formatDuration(7));
        assertEquals("7.0 min", TradeOdds.formatDuration(7 * 60));
        assertEquals("1.8 hr", TradeOdds.formatDuration(105 * 60));

        TradeOdds.Estimate impossible = TradeOdds.estimate(pool, target("enchant:minecraft:mending:1", 9), AttemptPace.TRADE_CYCLING_TICKS);
        assertTrue(impossible.impossible());
        assertEquals(8, AttemptPace.TRADE_CYCLING_TICKS);
        assertEquals(140, AttemptPace.JOB_SITE_TICKS);
    }

    private static void agree(TradeOdds.Pool pool, Map<String, Integer> targets) {
        double expected = TradeOdds.hitProbability(pool, targets);
        double sampled = TradeOdds.simulate(pool, targets, SEED, TRIALS);
        double tolerance = Math.max(0.01, 6 * Math.sqrt(expected * (1 - expected) / TRIALS));
        assertEquals(expected, sampled, tolerance, "expected " + expected + " sampled " + sampled);
    }

    private static TradeOdds.Pool novice(TradeOdds.RolledRow... books) {
        List<TradeOdds.RolledRow> rows = new java.util.ArrayList<>();
        rows.add(fixed("buy:minecraft:paper:24:24", 24));
        rows.addAll(List.of(books));
        rows.add(fixed("buy:minecraft:bookshelf:1:9", 9));
        return TradeOdds.pool(rows, new int[]{VanillaRoll.OFFERS_PER_LEVEL}, false);
    }

    private static TradeOdds.RolledRow fixed(String id, int price) {
        return new TradeOdds.RolledRow(id, price, price, TradeOdds.Roll.fixed(id));
    }

    private static TradeOdds.RolledRow book(String enchantment, int level, boolean treasure, int weight) {
        String id = "enchant:" + enchantment + ":" + level;
        return new TradeOdds.RolledRow(
                id,
                VanillaBookPrices.min(level, treasure),
                VanillaBookPrices.max(level, treasure),
                TradeOdds.Roll.book("books", enchantment, weight, level, treasure)
        );
    }

    private static Map<String, Integer> target(String id, int maxPrice) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        targets.put(id, maxPrice);
        return targets;
    }
}
