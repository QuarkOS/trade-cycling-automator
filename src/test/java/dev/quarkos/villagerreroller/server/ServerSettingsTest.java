package dev.quarkos.villagerreroller.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSettingsTest {
    @TempDir
    Path temp;

    @Test
    void defaultsMatchAFreshServer() {
        ServerSettings settings = ServerSettings.defaults();
        assertTrue(settings.enabled());
        assertEquals("free", settings.cost());
        assertEquals(0, settings.costKind());
        assertEquals(1, settings.costAmount());
        assertEquals(1, settings.cooldownTicks());
        assertEquals("villager_reroller.reroll", settings.permission());
        assertFalse(settings.requirePermission());
        assertEquals(2, settings.opLevel());
        assertTrue(settings.onlyFreshVillagers());
        assertEquals(8.0, settings.maxDistance());
    }

    @Test
    void aMissingFileIsCreatedFromTheDefaults() {
        Path file = temp.resolve("config/villager_reroller-server.json");
        ServerSettings settings = ServerSettings.load(file);
        assertTrue(Files.isRegularFile(file));
        assertEquals("free", settings.cost());
        assertEquals(8.0, settings.maxDistance());
    }

    @Test
    void garbageValuesClampToTheLegalRange() throws Exception {
        Path file = temp.resolve("villager_reroller-server.json");
        Files.writeString(file, """
                {
                  "enabled": false,
                  "cost": "Emerald",
                  "costAmount": -4,
                  "cooldownTicks": 99999999,
                  "permission": "  ",
                  "requirePermission": true,
                  "opLevel": 9,
                  "onlyFreshVillagers": false,
                  "maxDistance": 0.2
                }
                """);
        ServerSettings settings = ServerSettings.load(file);
        assertFalse(settings.enabled());
        assertEquals("emeralds", settings.cost());
        assertEquals(0, settings.costAmount());
        assertEquals(20 * 60 * 30, settings.cooldownTicks());
        assertEquals("villager_reroller.reroll", settings.permission());
        assertTrue(settings.requirePermission());
        assertEquals(4, settings.opLevel());
        assertFalse(settings.onlyFreshVillagers());
        assertEquals(1.0, settings.maxDistance());
    }

    @Test
    void experienceAndUnknownCostsResolveToAKind() throws Exception {
        Path experience = temp.resolve("xp.json");
        Files.writeString(experience, "{\"cost\":\"experience\",\"costAmount\":3}");
        ServerSettings xp = ServerSettings.load(experience);
        assertEquals("xp", xp.cost());
        assertEquals(3, xp.costAmount());

        Path unknown = temp.resolve("nope.json");
        Files.writeString(unknown, "{\"cost\":\"diamonds\"}");
        assertEquals("free", ServerSettings.load(unknown).cost());
    }

    @Test
    void truncatedJsonFallsBackToTheDefaults() throws Exception {
        Path file = temp.resolve("broken.json");
        Files.writeString(file, "{");
        ServerSettings settings = ServerSettings.load(file);
        assertTrue(settings.enabled());
        assertEquals("free", settings.cost());
        assertEquals(8.0, settings.maxDistance());
        assertTrue(Files.readString(file).contains("\"cost\""));
    }
}
