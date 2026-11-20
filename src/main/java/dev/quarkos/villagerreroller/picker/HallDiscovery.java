package dev.quarkos.villagerreroller.picker;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Selects discovery candidates from data the client has already checked. */
public final class HallDiscovery {
    private HallDiscovery() {
    }

    public record Position(double x, double y, double z) {
        public double distanceSquared(Position other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    public record Candidate(UUID id, Position position, String profession, boolean eligible) {
    }

    public static List<Candidate> select(Position center, int radius, List<Candidate> candidates,
                                         Set<String> professionsWithPicks, Set<UUID> queued, int limit) {
        if (radius <= 0 || limit <= 0 || professionsWithPicks.isEmpty()) {
            return List.of();
        }
        double radiusSquared = (double) radius * radius;
        Set<UUID> seen = new HashSet<>(queued);
        return candidates.stream()
                .filter(candidate -> candidate.eligible() && professionsWithPicks.contains(candidate.profession()))
                .filter(candidate -> center.distanceSquared(candidate.position()) <= radiusSquared)
                .sorted(Comparator.comparingDouble((Candidate candidate) -> center.distanceSquared(candidate.position()))
                        .thenComparing(Candidate::id))
                .filter(candidate -> seen.add(candidate.id()))
                .limit(limit)
                .toList();
    }
}
