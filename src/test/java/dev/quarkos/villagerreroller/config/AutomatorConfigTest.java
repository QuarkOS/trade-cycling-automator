package dev.quarkos.villagerreroller.config;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(config.toJson().has("enchantment"));
        assertFalse(config.toJson().has("level"));
        assertFalse(config.toJson().has("maxPrice"));
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
                  "lookDegreesPerTick": 12.5
                }
                """));
        AutomatorConfig again = AutomatorConfig.parse(new StringReader(config.toJson().toString()));
        assertEquals(4, again.durabilityFloor());
        assertFalse(again.allowBareHand());
        assertTrue(again.serverFriendly());
        assertEquals(16, again.ticksBetweenCycles());
        assertEquals(3, again.timeoutMultiplier());
        assertEquals(12.5F, again.lookDegreesPerTick(), 0.001F);
    }
}
