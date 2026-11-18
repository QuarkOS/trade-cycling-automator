package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.HallTradeFilter.Tile;
import org.junit.jupiter.api.Test;

import static dev.quarkos.villagerreroller.picker.HallTradeFilter.Filter.*;
import static dev.quarkos.villagerreroller.picker.HallTradeFilter.matches;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HallTradeFilterTest {
    private static final Tile MENDING = new Tile("Mending I", "Librarian", "enchant:minecraft:mending:1", true, true);
    private static final Tile UNBREAKING = new Tile("Unbreaking III", "Librarian", "enchant:minecraft:unbreaking:3", true, false);
    private static final Tile GLASS = new Tile("Glass", "Librarian", "buy:minecraft:glass:4:1", false, true);
    private static final Tile BREAD = new Tile("Bread", "Farmer", "buy:minecraft:bread:6:1", false, false);

    @Test
    void matchesNamesIgnoringCaseAndTrimsTheQuery() {
        assertTrue(matches("  MeNd  ", ALL, MENDING));
        assertTrue(matches("mending", ALL, MENDING));
        assertTrue(matches("mending i", ALL, MENDING));
        assertTrue(matches("unbreaking iii", ALL, UNBREAKING));
        assertFalse(matches("unbreaking iv", ALL, UNBREAKING));
        assertTrue(matches("laSS", ALL, GLASS));
    }

    @Test
    void matchesProfessionAndTradeId() {
        assertTrue(matches("librarian", ALL, MENDING));
        assertTrue(matches("  MINECRAFT:UNBREAKING:3 ", ALL, UNBREAKING));
        assertFalse(matches("librarian", ALL, BREAD));
    }

    @Test
    void combinesNameMatchingWithTheAvailabilityFilter() {
        assertTrue(matches("librarian", AVAILABLE, MENDING));
        assertFalse(matches("librarian", MISSING, MENDING));
        assertTrue(matches("librarian", MISSING, UNBREAKING));
        assertFalse(matches("librarian", AVAILABLE, UNBREAKING));
        assertFalse(matches("bread", MISSING, BREAD));
        assertFalse(matches("bread", AVAILABLE, GLASS));
    }

    @Test
    void blankQueryLeavesOnlyTheAvailabilityFilter() {
        assertTrue(matches("", ALL, MENDING));
        assertTrue(matches("  \t ", ALL, UNBREAKING));
        assertTrue(matches("", ALL, BREAD));
        assertTrue(matches("  ", AVAILABLE, GLASS));
        assertTrue(matches("", MISSING, UNBREAKING));
        assertFalse(matches("", MISSING, BREAD));
    }
}
