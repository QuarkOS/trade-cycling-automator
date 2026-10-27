package dev.quarkos.villagerreroller.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionPicksTest {
    @Test
    void picksSurviveANewLoadAndKeepEachProfession() throws IOException {
        Path dir = Files.createTempDirectory("villager-reroller-picks");
        Path file = dir.resolve(PickFiles.FILE_NAME);
        ProfessionPicks picks = new ProfessionPicks();
        Set<String> librarian = catalog("enchant:minecraft:mending:1", "enchant:minecraft:efficiency:5");
        Map<String, Integer> librarianPrices = new LinkedHashMap<>();
        librarianPrices.put("enchant:minecraft:mending:1", 14);
        librarianPrices.put("enchant:minecraft:efficiency:5", Integer.MAX_VALUE);
        picks.replaceCatalog("minecraft:librarian", librarian, librarianPrices);

        Set<String> farmer = catalog("buy:minecraft:bread:1:1");
        picks.replaceCatalog("minecraft:farmer", farmer, Map.of("buy:minecraft:bread:1:1", 4));
        assertTrue(picks.save(file));

        ProfessionPicks restored = ProfessionPicks.load(file);
        assertEquals(14, restored.picks("minecraft:librarian").get("enchant:minecraft:mending:1"));
        assertEquals(Integer.MAX_VALUE, restored.picks("minecraft:librarian").get("enchant:minecraft:efficiency:5"));
        assertEquals(4, restored.picks("minecraft:farmer").get("buy:minecraft:bread:1:1"));
        assertTrue(Files.readString(file).contains("\"version\": 1"));
    }

    @Test
    void replacingOneCatalogKeepsOtherLevelsAndProfessions() {
        ProfessionPicks picks = new ProfessionPicks();
        picks.replaceCatalog(
                "minecraft:librarian",
                catalog("enchant:minecraft:mending:1", "buy:minecraft:bookshelf:1:9"),
                Map.of(
                        "enchant:minecraft:mending:1", 10,
                        "buy:minecraft:bookshelf:1:9", 9,
                        "enchant:minecraft:efficiency:5", 64
                )
        );
        // A higher level's book is not on this catalog, so it has to be stored beside it.
        Map<String, Integer> withLevelFive = new LinkedHashMap<>(picks.picks("minecraft:librarian"));
        withLevelFive.put("enchant:minecraft:sharpness:5", 40);
        picks.replaceCatalog(
                "minecraft:librarian",
                catalog("enchant:minecraft:mending:1", "buy:minecraft:bookshelf:1:9", "enchant:minecraft:sharpness:5"),
                withLevelFive
        );
        picks.replaceCatalog(
                "minecraft:farmer",
                catalog("buy:minecraft:bread:1:1"),
                Map.of("buy:minecraft:bread:1:1", 3)
        );

        picks.replaceCatalog(
                "minecraft:librarian",
                catalog("enchant:minecraft:mending:1", "buy:minecraft:bookshelf:1:9"),
                Map.of("enchant:minecraft:mending:1", 12)
        );

        Map<String, Integer> librarian = picks.picks("minecraft:librarian");
        assertEquals(12, librarian.get("enchant:minecraft:mending:1"));
        assertFalse(librarian.containsKey("buy:minecraft:bookshelf:1:9"));
        assertEquals(40, librarian.get("enchant:minecraft:sharpness:5"));
        assertEquals(3, picks.picks("minecraft:farmer").get("buy:minecraft:bread:1:1"));
    }

    @Test
    void unreadableFileIsNotOverwritten() throws IOException {
        Path dir = Files.createTempDirectory("villager-reroller-picks");
        Path file = dir.resolve(PickFiles.FILE_NAME);
        Files.writeString(file, "{");
        ProfessionPicks broken = ProfessionPicks.load(file);
        assertFalse(broken.readable());
        assertTrue(broken.picks("minecraft:librarian").isEmpty());
        broken.replaceCatalog("minecraft:librarian", catalog("buy:minecraft:bread:1:1"), Map.of("buy:minecraft:bread:1:1", 1));
        assertFalse(broken.save(file));
        assertEquals("{", Files.readString(file));
    }

    @Test
    void quotedTradeIdsRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("villager-reroller-picks");
        Path file = dir.resolve(PickFiles.FILE_NAME);
        ProfessionPicks picks = new ProfessionPicks();
        String id = "mod:trade\"quoted";
        picks.replaceCatalog("mod:profession", catalog(id), Map.of(id, 8));
        assertTrue(picks.save(file));
        assertEquals(8, ProfessionPicks.load(file).picks("mod:profession").get(id));
    }

    @Test
    void fitPriceClampsASavedCeilingIntoTheCurrentRow() {
        assertEquals(0, ProfessionPicks.fitPrice(0, 5, 19, true));
        assertEquals(5, ProfessionPicks.fitPrice(2, 5, 19, true));
        assertEquals(12, ProfessionPicks.fitPrice(12, 5, 19, true));
        assertEquals(19, ProfessionPicks.fitPrice(40, 5, 19, true));
        assertEquals(Integer.MAX_VALUE, ProfessionPicks.fitPrice(Integer.MAX_VALUE, 5, 19, true));
        assertEquals(Integer.MAX_VALUE, ProfessionPicks.fitPrice(3, 1, 1, false));
    }

    private static Set<String> catalog(String... ids) {
        Set<String> catalog = new LinkedHashSet<>();
        for (String id : ids) {
            catalog.add(id);
        }
        return catalog;
    }
}
