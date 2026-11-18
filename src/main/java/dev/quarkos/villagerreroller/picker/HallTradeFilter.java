package dev.quarkos.villagerreroller.picker;

import java.util.Locale;

final class HallTradeFilter {
    private HallTradeFilter() {
    }

    enum Filter { ALL, AVAILABLE, MISSING }

    record Tile(String name, String profession, String tradeId, boolean wanted, boolean available) {
    }

    static boolean matches(String query, Filter filter, Tile tile) {
        boolean included = switch (filter) {
            case ALL -> true;
            case AVAILABLE -> tile.available();
            case MISSING -> tile.wanted() && !tile.available();
        };
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return included && (needle.isEmpty()
                || tile.name().toLowerCase(Locale.ROOT).contains(needle)
                || tile.profession().toLowerCase(Locale.ROOT).contains(needle)
                || tile.tradeId().toLowerCase(Locale.ROOT).contains(needle));
    }
}
