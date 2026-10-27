package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookStepTest {
    @Test
    void aLargeTurnIsCappedAtTheRate() {
        assertEquals(30.0F, LookStep.approach(0.0F, 90.0F, 30.0F), 0.001F);
        // An exact half-turn wraps to -180, so the capped step goes negative.
        assertEquals(-30.0F, LookStep.approach(0.0F, 180.0F, 30.0F), 0.001F);
        assertEquals(2.0F, LookStep.approach(10.0F, -170.0F, 8.0F), 0.001F);
    }

    @Test
    void aSmallTurnFinishesInOneStep() {
        assertEquals(12.0F, LookStep.approach(10.0F, 12.0F, 30.0F), 0.001F);
        assertTrue(LookStep.within(12.0F, -4.0F, 12.0F, -4.0F, 3.0F));
        assertFalse(LookStep.within(0.0F, 0.0F, 90.0F, 0.0F, 3.0F));
    }

    @Test
    void yawWrapsAcrossTheSeam() {
        assertEquals(-170.0F, LookStep.approach(170.0F, -170.0F, 30.0F), 0.001F);
        assertTrue(LookStep.within(179.0F, 0.0F, -179.0F, 0.0F, 3.0F));
    }
}
