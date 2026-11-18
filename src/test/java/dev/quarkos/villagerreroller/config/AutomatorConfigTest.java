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
        assertTrue(config.autoHallDiscovery());
        assertEquals(16, config.hallDiscoveryRadius());
        assertEquals(AutomatorConfig.HallView.GRID, config.hallView());
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
                  "autoHallDiscovery": false,
                  "hallDiscoveryRadius": 60,
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
        assertFalse(again.autoHallDiscovery());
        assertEquals(60, again.hallDiscoveryRadius());
        assertTrue(again.serverFriendly());
        assertEquals(16, again.ticksBetweenCycles());
        assertEquals(3, again.timeoutMultiplier());
        assertEquals(12.5F, again.lookDegreesPerTick(), 0.001F);
    }

    @Test
    void discoveryRadiusFitsTheSupportedRange() {
        AutomatorConfig config = AutomatorConfig.parse(new StringReader("""
                {"hallDiscoveryRadius": -1}
                """));
        assertEquals(0, config.hallDiscoveryRadius());
        config.setHallDiscoveryRadius(65);
        assertEquals(64, config.hallDiscoveryRadius());
    }

    @Test
    void hallViewRoundTripsAndUnknownValuesUseGrid() {
        AutomatorConfig config = AutomatorConfig.parse(new StringReader("""
                {"hallView": "unknown"}
                """));
        assertEquals(AutomatorConfig.HallView.GRID, config.hallView());

        config.setHallView(AutomatorConfig.HallView.LIST);
        assertEquals("LIST", config.toJson().get("hallView").getAsString());
        AutomatorConfig again = AutomatorConfig.parse(new StringReader(config.toJson().toString()));
        assertEquals(AutomatorConfig.HallView.LIST, again.hallView());
    }
}
