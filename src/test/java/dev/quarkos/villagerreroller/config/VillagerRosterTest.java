package dev.quarkos.villagerreroller.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerRosterTest {
    @Test
    void roundTripKeepsTheFirstLockTime() throws Exception {
        Path dir = Files.createTempDirectory("villager-roster");
        Path file = dir.resolve(VillagerRosterFiles.FILE_NAME);
        VillagerRoster roster = new VillagerRoster();
        roster.upsert("local:New World", entry("aaa", "Mending", 5, 1000L, offer("enchant:minecraft:mending:1", "Mending I", 10)));
        roster.upsert("local:New World", entry("aaa", "Mending", 5, 9999L, offer("enchant:minecraft:mending:1", "Mending I", 12)));
        assertTrue(roster.save(file));

        VillagerRoster loaded = VillagerRoster.load(file);
        List<VillagerRoster.Entry> entries = loaded.entries("local:New World");
        assertEquals(1, entries.size());
        assertEquals(1000L, entries.get(0).updatedAt());
        assertEquals(12, entries.get(0).trades().get(0).effectiveCost());
        assertTrue(Files.readString(file).contains("\"version\": 1"));
    }

    @Test
    void wantedTradesMarkMissingOverCapAndLockedIn() {
        List<VillagerRoster.Entry> roster = List.of(
                entry("one", "", 5, 1L, offer("enchant:minecraft:mending:1", "Mending I", 10), sell("sell:minecraft:paper:24:1", "Paper")),
                entry("two", "", 2, 2L, offer("enchant:minecraft:silk_touch:1", "Silk Touch I", 16)),
                entry("three", "Farmer", 1, 3L, offer("buy:minecraft:bread:1:1", "Bread", 1))
        );
        ProfessionPicks picks = new ProfessionPicks();
        picks.replaceCatalog("minecraft:librarian",
                Set.of(
                        "enchant:minecraft:mending:1",
                        "enchant:minecraft:efficiency:5",
                        "enchant:minecraft:silk_touch:1",
                        "enchant:minecraft:protection:4",
                        "sell:minecraft:paper:24:1"),
                Map.of(
                        "enchant:minecraft:mending:1", 12,
                        "enchant:minecraft:efficiency:5", 20,
                        "enchant:minecraft:silk_touch:1", 10,
                        "enchant:minecraft:protection:4", Integer.MAX_VALUE,
                        "sell:minecraft:paper:24:1", Integer.MAX_VALUE));

        List<VillagerRoster.WantedCheck> checks = VillagerRoster.evaluateWanted(picks, roster, null);
        VillagerRoster.WantedCheck mending = find(checks, "enchant:minecraft:mending:1");
        VillagerRoster.WantedCheck efficiency = find(checks, "enchant:minecraft:efficiency:5");
        VillagerRoster.WantedCheck silk = find(checks, "enchant:minecraft:silk_touch:1");
        VillagerRoster.WantedCheck protection = find(checks, "enchant:minecraft:protection:4");
        assertEquals(VillagerRoster.WantedState.LOCKED_IN, mending.state());
        assertEquals(1, mending.bestVillagerIndex());
        assertEquals(VillagerRoster.WantedState.MISSING, efficiency.state());
        assertEquals(VillagerRoster.WantedState.OVER_CAP, silk.state());
        assertEquals(16, silk.bestCost());
        assertEquals(2, silk.bestVillagerIndex());
        assertEquals(VillagerRoster.WantedState.MISSING, protection.state());
        VillagerRoster.WantedCheck paper = find(checks, "sell:minecraft:paper:24:1");
        assertEquals(VillagerRoster.WantedState.LOCKED_IN, paper.state());
        assertEquals("Efficiency V", VillagerRoster.prettyTrade("enchant:minecraft:efficiency:5"));
    }

    @Test
    void unreadableFileIsNotOverwritten() throws Exception {
        Path dir = Files.createTempDirectory("villager-roster-bad");
        Path file = dir.resolve(VillagerRosterFiles.FILE_NAME);
        Files.writeString(file, "{not json");
        VillagerRoster loaded = VillagerRoster.load(file);
        assertFalse(loaded.readable());
        assertFalse(loaded.save(file));
        assertEquals("{not json", Files.readString(file));
    }

    private static VillagerRoster.WantedCheck find(List<VillagerRoster.WantedCheck> checks, String id) {
        return checks.stream().filter(check -> id.equals(check.tradeId())).findFirst().orElseThrow();
    }

    private static VillagerRoster.Entry entry(String uuid, String name, int level, long updated, VillagerRoster.LockedOffer... offers) {
        return new VillagerRoster.Entry(uuid, "minecraft:overworld", 1, 64, 1, name, "minecraft:librarian", level, updated, List.of(offers));
    }

    private static VillagerRoster.LockedOffer offer(String id, String name, int cost) {
        return new VillagerRoster.LockedOffer(id, name, "", 0, "minecraft:emerald", 1, cost, cost, true);
    }

    private static VillagerRoster.LockedOffer sell(String id, String name) {
        return new VillagerRoster.LockedOffer(id, name, "", 0, "minecraft:paper", 24, 1, 1, false);
    }
}
