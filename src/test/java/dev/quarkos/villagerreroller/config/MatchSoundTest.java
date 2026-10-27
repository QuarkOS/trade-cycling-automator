package dev.quarkos.villagerreroller.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSoundTest {
    @Test
    void volumeStaysInsideTheSlider() {
        assertEquals(0, MatchSound.clampPercent(-10));
        assertEquals(0, MatchSound.clampPercent(0));
        assertEquals(40, MatchSound.clampPercent(40));
        assertEquals(100, MatchSound.clampPercent(250));
        assertEquals(1.0F, MatchSound.scale(100));
        assertEquals(0.4F, MatchSound.scale(40));
        assertEquals(0.0F, MatchSound.scale(-1));
    }

    @Test
    void silentWhenDisabledOrTurnedDown() {
        assertTrue(MatchSound.audible(true, MatchSound.DEFAULT_VOLUME));
        assertTrue(MatchSound.audible(true, 1));
        assertFalse(MatchSound.audible(false, 100));
        assertFalse(MatchSound.audible(true, 0));
    }
}
