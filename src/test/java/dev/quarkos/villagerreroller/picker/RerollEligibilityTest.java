package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RerollEligibilityTest {
    @Test
    void onlyNoviceTradesCanReroll() {
        assertTrue(RerollEligibility.of(1).canReroll());
        for (int tier = 2; tier <= 5; tier++) {
            assertFalse(RerollEligibility.of(tier).canReroll());
        }
        assertFalse(RerollEligibility.of(0).canReroll());
    }

    @Test
    void usesTheFirstProfessionTierForTheReason() {
        var tiers = RerollEligibility.values();
        for (int level = 1; level <= 5; level++) {
            assertEquals(tiers[level - 1], RerollEligibility.of(level));
            assertEquals("merchant.level." + level, RerollEligibility.of(level).levelKey());
        }
        assertEquals(RerollEligibility.UNAVAILABLE, RerollEligibility.of(-1));
        assertEquals(RerollEligibility.UNAVAILABLE, RerollEligibility.of(6));
    }
}
