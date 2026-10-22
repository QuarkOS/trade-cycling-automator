package dev.tradecycling.automator.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollingNamesTest {
    @Test
    void textThatFitsStaysPut() {
        assertEquals(0, ScrollingNames.offset(40, 80, 0));
        assertEquals(0, ScrollingNames.offset(40, 40, 12_345));
        assertEquals(0, ScrollingNames.offset(10, 0, 0));
    }

    @Test
    void overflowTravelsToTheEndAndBackOnTheVanillaPeriod() {
        int available = 40;
        int overflow = 60;
        int text = available + overflow;
        double period = Math.max(overflow * ScrollingNames.PERIOD_PER_PIXEL, ScrollingNames.MIN_PERIOD_SECONDS);

        assertEquals(overflow, ScrollingNames.offset(text, available, 0L));
        assertEquals(overflow / 2, ScrollingNames.offset(text, available, (long) (period / 4.0 * 1000.0)));
        assertEquals(0, ScrollingNames.offset(text, available, (long) (period / 2.0 * 1000.0)));
        assertEquals(overflow, ScrollingNames.offset(text, available, (long) (period * 1000.0)));
    }

    @Test
    void aShortOverflowStillTakesAtLeastThreeSeconds() {
        int overflow = 2;
        int text = 12;
        int available = 10;
        assertEquals(3.0, Math.max(overflow * ScrollingNames.PERIOD_PER_PIXEL, ScrollingNames.MIN_PERIOD_SECONDS));
        assertEquals(overflow, ScrollingNames.offset(text, available, 0L));
        assertEquals(1, ScrollingNames.offset(text, available, 750L));
        assertEquals(0, ScrollingNames.offset(text, available, 1_500L));
    }

    @Test
    void offsetStaysInsideTheOverflow() {
        int text = 100;
        int available = 30;
        int overflow = text - available;
        for (long millis = 0; millis < 20_000; millis += 50) {
            int offset = ScrollingNames.offset(text, available, millis);
            assertTrue(offset >= 0 && offset <= overflow, "offset " + offset + " at " + millis);
        }
    }
}
