package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.TradeOdds;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the rows the trade panel already built into the pool the odds math rolls.
 */
public final class TradePools {
    private TradePools() {
    }

    public static TradeOdds.Pool from(List<TradeEntry> entries, int[] offerCounts, boolean duplicates) {
        if (entries == null || entries.isEmpty()) {
            return TradeOdds.Pool.EMPTY;
        }
        List<TradeOdds.RolledRow> rows = new ArrayList<>(entries.size());
        for (TradeEntry entry : entries) {
            rows.add(new TradeOdds.RolledRow(entry.id, entry.emeraldMin, entry.emeraldMax, entry.roll));
        }
        return TradeOdds.pool(rows, offerCounts, duplicates);
    }
}
