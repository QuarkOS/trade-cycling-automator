package dev.quarkos.villagerreroller.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerollRulesTest {
    @Test
    void cooldownHasAOneTickFloor() {
        assertEquals(1, RerollRules.enforcedCooldown(0));
        assertFalse(RerollRules.coolingDown(10, -1, 0));
        assertTrue(RerollRules.coolingDown(10, 10, 0));
        assertFalse(RerollRules.coolingDown(11, 10, 0));
        assertTrue(RerollRules.coolingDown(15, 10, 20));
        assertFalse(RerollRules.coolingDown(30, 10, 20));
    }

    @Test
    void distanceRejectsAnotherLevelAndPastTheCap() {
        assertTrue(RerollRules.tooFar(false, 0.0, 8.0));
        assertTrue(RerollRules.tooFar(true, 8.1 * 8.1, 8.0));
        assertFalse(RerollRules.tooFar(true, 8.0 * 8.0, 8.0));
        assertFalse(RerollRules.tooFar(true, 1.0, 1.0));
    }

    @Test
    void freshVillagersMatchTheVanillaLock() {
        assertFalse(RerollRules.locked(true, 1, 0, false));
        assertTrue(RerollRules.locked(true, 2, 0, false));
        assertTrue(RerollRules.locked(true, 1, 1, false));
        assertTrue(RerollRules.locked(true, 1, 0, true));
        assertFalse(RerollRules.locked(false, 5, 100, true));
    }

    @Test
    void costCanBeFreeEmeraldsOrXp() {
        assertTrue(RerollRules.canAfford(0, 5, 0, 0));
        assertTrue(RerollRules.canAfford(1, 3, 3, 0));
        assertFalse(RerollRules.canAfford(1, 3, 2, 100));
        assertTrue(RerollRules.canAfford(2, 10, 0, 10));
        assertFalse(RerollRules.canAfford(2, 10, 64, 9));
        assertEquals(7, RerollRules.totalXp(1, 0.0F, 7));
        assertEquals(0, RerollRules.totalXp(0, 0.0F, 7));
    }
}
