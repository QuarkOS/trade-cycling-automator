package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HallDiscoveryTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String FARMER = "minecraft:farmer";
    private static final HallDiscovery.Position CENTER = new HallDiscovery.Position(0, 0, 0);

    @Test
    void includesTheRadiusBoundaryInThreeDimensions() {
        var edge = candidate(1, 0, 3, 4, LIBRARIAN, true);
        var inside = candidate(2, 0, -4, 0, LIBRARIAN, true);
        var outside = candidate(3, 3, 3, 3, LIBRARIAN, true);
        assertEquals(List.of(inside, edge),
                HallDiscovery.select(CENTER, 5, List.of(edge, outside, inside), Set.of(LIBRARIAN), Set.of(), 32));
    }

    @Test
    void skipsQueuedIneligibleAndUntargetedVillagers() {
        var queued = candidate(1, 1, 0, 0, LIBRARIAN, true);
        var ineligible = candidate(2, 1, 0, 0, LIBRARIAN, false);
        var untargeted = candidate(3, 1, 0, 0, FARMER, true);
        var eligible = candidate(4, 2, 0, 0, LIBRARIAN, true);
        assertEquals(List.of(eligible), HallDiscovery.select(CENTER, 16,
                List.of(queued, ineligible, untargeted, eligible), Set.of(LIBRARIAN), Set.of(queued.id()), 32));
    }

    @Test
    void deduplicatesBeforeApplyingTheCapAndBreaksDistanceTiesById() {
        var duplicate = candidate(1, 4, 0, 0, FARMER, true);
        var nearest = candidate(1, 1, 0, 0, FARMER, true);
        var tiedFirst = candidate(2, 2, 0, 0, FARMER, true);
        var tiedSecond = candidate(3, -2, 0, 0, FARMER, true);
        assertEquals(List.of(nearest, tiedFirst), HallDiscovery.select(CENTER, 16,
                List.of(duplicate, tiedSecond, tiedFirst, nearest, nearest), Set.of(FARMER), Set.of(), 2));
    }

    @Test
    void radiusZeroDisablesDiscoveryEvenAtTheCenter() {
        var candidate = candidate(1, 0, 0, 0, LIBRARIAN, true);
        assertEquals(List.of(), HallDiscovery.select(CENTER, 0, List.of(candidate), Set.of(LIBRARIAN), Set.of(), 32));
    }

    private static HallDiscovery.Candidate candidate(int id, double x, double y, double z, String profession, boolean eligible) {
        return new HallDiscovery.Candidate(new UUID(0, id), new HallDiscovery.Position(x, y, z), profession, eligible);
    }
}
