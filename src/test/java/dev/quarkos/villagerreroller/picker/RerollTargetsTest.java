package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.config.ProfessionPicks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RerollTargetsTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String FARMER = "minecraft:farmer";
    private static final String BOOK = "enchant:minecraft:unbreaking:3";
    private static final String PAPER = "sell:minecraft:paper:24:1";
    private static final String LANTERN = "buy:minecraft:lantern:1:1";
    private static final String GLASS = "buy:minecraft:glass:4:1";
    private static final String UNKNOWN = "buy:old:removed_item:1:5";

    private final Map<String, RerollTargets.Offer> catalog = Map.of(
            BOOK, offer(1, 11, 45, true),
            PAPER, offer(1, 1, 1, false),
            LANTERN, offer(2, 1, 1, true),
            GLASS, offer(3, 1, 1, true));

    @TempDir
    Path directory;

    @Test
    void savedBrowserPricesBecomeTheProfessionTargets() {
        Map<String, Integer> library = Map.of(BOOK, 25, PAPER, 1, LANTERN, 1, GLASS, 1, UNKNOWN, 5);
        var picks = new ProfessionPicks();
        picks.replaceCatalog(LIBRARIAN, library.keySet(), library);
        picks.replaceCatalog(FARMER, catalog.keySet(), Map.of(PAPER, 2));
        Path file = directory.resolve("picks.json");
        assertTrue(picks.save(file));

        var reloaded = ProfessionPicks.load(file);
        assertEquals(Map.of(BOOK, 25, PAPER, Integer.MAX_VALUE),
                RerollTargets.resolve(reloaded.picks(LIBRARIAN), catalog::get));
        assertEquals(library, reloaded.picks(LIBRARIAN), "legacy picks remain available to display and remove");
        assertEquals(Map.of(PAPER, 2), reloaded.picks(FARMER));
    }

    @Test
    void fitsLegacyPricesAndPreservesAnyPrice() {
        assertEquals(Map.of(BOOK, 11), RerollTargets.resolve(Map.of(BOOK, 1), catalog::get));
        assertEquals(Map.of(BOOK, 45), RerollTargets.resolve(Map.of(BOOK, 64), catalog::get));
        assertEquals(Map.of(BOOK, Integer.MAX_VALUE),
                RerollTargets.resolve(Map.of(BOOK, Integer.MAX_VALUE), catalog::get));
        assertTrue(RerollTargets.resolve(Map.of(BOOK, 0, LANTERN, 1), catalog::get).isEmpty());
    }

    private static RerollTargets.Offer offer(int tier, int min, int max, boolean paysEmeralds) {
        return new RerollTargets.Offer(RerollEligibility.of(tier), min, max, paysEmeralds);
    }
}
