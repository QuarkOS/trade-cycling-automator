package dev.quarkos.villagerreroller.config;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomatorConfigTest {
    @Test
    void legacyEnchantmentKeysAreIgnoredAndDefaultsFillTheGaps() {
        AutomatorConfig config = AutomatorConfig.parse(new StringReader("""
                {
                  "enabled": false,
                  "enchantment": "minecraft:mending",
                  "level": 5,
                  "maxPrice": 64,
                  "maxAttempts": 12
                }
                """));
        assertFalse(config.enabled());
        assertEquals(12, config.maxAttempts());
        assertEquals(AutomatorConfig.DEFAULT_DURABILITY_FLOOR, config.durabilityFloor());
        assertTrue(config.allowBareHand());
        assertFalse(config.serverFriendly());
        assertEquals(AutomatorConfig.NORMAL_TICKS, config.ticksBetweenCycles());
        assertEquals(AutomatorConfig.NORMAL_TIMEOUT, config.timeoutMultiplier());
        assertEquals(AutomatorConfig.NORMAL_LOOK, config.lookDegreesPerTick(), 0.001F);
        assertTrue(config.matchSound());
        assertEquals(MatchSound.DEFAULT_VOLUME, config.matchVolume());
        assertFalse(config.toJson().has("enchantment"));
        assertFalse(config.toJson().has("level"));
        assertFalse(config.toJson().has("maxPrice"));
    }

    @Test
    void anEmptyFileAndANonObjectKeepTheDefaults() {
        AutomatorConfig empty = AutomatorConfig.parse(new StringReader("{}"));
        assertTrue(empty.enabled());
        assertEquals(500, empty.maxAttempts());
        assertEquals(AutomatorConfig.DEFAULT_DURABILITY_FLOOR, empty.durabilityFloor());
        assertTrue(empty.allowBareHand());
        assertEquals(AutomatorConfig.NORMAL_TICKS, empty.ticksBetweenCycles());
        assertEquals(AutomatorConfig.NORMAL_LOOK, empty.lookDegreesPerTick(), 0.001F);

        AutomatorConfig array = AutomatorConfig.parse(new StringReader("[]"));
        assertTrue(array.enabled());
        assertEquals(500, array.maxAttempts());

        AutomatorConfig nil = AutomatorConfig.parse(new StringReader("null"));
        assertEquals(AutomatorConfig.DEFAULT_DURABILITY_FLOOR, nil.durabilityFloor());
    }

    @Test
    void wrongTypesAreIgnoredAndOutOfRangeNumbersClamp() {
        AutomatorConfig config = AutomatorConfig.parse(new StringReader("""
                {
                  "enabled": "yes",
                  "maxAttempts": -3,
                  "durabilityFloor": -1,
                  "lookDegreesPerTick": 0,
                  "ticksBetweenCycles": "slow",
                  "timeoutMultiplier": 0,
                  "matchVolume": 400,
                  "allowBareHand": "sometimes"
                }
                """));
        assertTrue(config.enabled());
        assertEquals(1, config.maxAttempts());
        assertEquals(0, config.durabilityFloor());
        assertEquals(1.0F, config.lookDegreesPerTick(), 0.001F);
        assertEquals(AutomatorConfig.NORMAL_TICKS, config.ticksBetweenCycles());
        assertEquals(1, config.timeoutMultiplier());
        assertEquals(100, config.matchVolume());
        assertTrue(config.allowBareHand());
    }

    @Test
    void truncatedJsonIsRejected() {
        assertThrows(JsonSyntaxException.class, () -> AutomatorConfig.parse(new StringReader("{")));
    }

    @Test
    void serverFriendlyPresetSlowsTheCadenceAndTheCamera() {
        AutomatorConfig config = new AutomatorConfig();
        config.applyServerFriendly(true);
        assertEquals(AutomatorConfig.FRIENDLY_TICKS, config.ticksBetweenCycles());
        assertEquals(AutomatorConfig.FRIENDLY_TIMEOUT, config.timeoutMultiplier());
        assertEquals(AutomatorConfig.FRIENDLY_LOOK, config.lookDegreesPerTick(), 0.001F);
        config.applyServerFriendly(false);
        assertEquals(AutomatorConfig.NORMAL_TICKS, config.ticksBetweenCycles());
        assertEquals(AutomatorConfig.NORMAL_LOOK, config.lookDegreesPerTick(), 0.001F);
    }

    @Test
    void savedNumbersRoundTrip() {
        AutomatorConfig config = AutomatorConfig.parse(new StringReader("""
                {
                  "durabilityFloor": 4,
                  "allowBareHand": false,
                  "serverFriendly": true,
                  "ticksBetweenCycles": 16,
                  "timeoutMultiplier": 3,
                  "lookDegreesPerTick": 12.5,
                  "matchSound": false,
                  "matchVolume": 40
                }
                """));
        AutomatorConfig again = AutomatorConfig.parse(new StringReader(config.toJson().toString()));
        assertEquals(4, again.durabilityFloor());
        assertFalse(again.matchSound());
        assertEquals(40, again.matchVolume());
        assertFalse(again.allowBareHand());
        assertTrue(again.serverFriendly());
        assertEquals(16, again.ticksBetweenCycles());
        assertEquals(3, again.timeoutMultiplier());
        assertEquals(12.5F, again.lookDegreesPerTick(), 0.001F);
    }
}
