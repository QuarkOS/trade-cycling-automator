package dev.quarkos.villagerreroller.picker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Assigns each villager one rank using the lowest known price for the selected trade. */
public final class PriceRanking {
    private static final int[] COLORS = {UiTheme.GREEN, UiTheme.YELLOW, UiTheme.AMBER, UiTheme.RED};

    private PriceRanking() {
    }

    public record Offer(UUID villagerId, int price, double distanceSquared) {
    }

    public record Ranked(Offer offer, int rank, int color) {
    }

    public static List<Ranked> rank(List<Offer> offers) {
        Set<UUID> seen = new HashSet<>();
        List<Offer> sorted = offers.stream()
                .filter(offer -> offer.price() > 0 && offer.distanceSquared() >= 0 && Double.isFinite(offer.distanceSquared()))
                .sorted(Comparator.comparingInt(Offer::price).thenComparingDouble(Offer::distanceSquared)
                        .thenComparing(Offer::villagerId))
                .filter(offer -> seen.add(offer.villagerId()))
                .toList();
        List<Ranked> ranked = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            double fraction = sorted.size() == 1 ? 0 : (double) index / (sorted.size() - 1);
            ranked.add(new Ranked(sorted.get(index), index + 1, color(fraction)));
        }
        return List.copyOf(ranked);
    }

    private static int color(double fraction) {
        double stop = fraction * (COLORS.length - 1);
        int lower = Math.min((int) stop, COLORS.length - 2);
        int from = COLORS[lower];
        int to = COLORS[lower + 1];
        double weight = stop - lower;
        int red = blend(from, to, weight, 16);
        int green = blend(from, to, weight, 8);
        int blue = blend(from, to, weight, 0);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int blend(int from, int to, double weight, int shift) {
        int start = (from >> shift) & 255;
        int end = (to >> shift) & 255;
        return (int) Math.round(start + (end - start) * weight);
    }
}
