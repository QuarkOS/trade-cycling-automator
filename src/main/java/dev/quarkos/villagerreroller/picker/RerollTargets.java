package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.config.ProfessionPicks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Converts saved browser prices to ceilings for the current novice pool. */
final class RerollTargets {
    private RerollTargets() {
    }

    record Offer(RerollEligibility eligibility, int emeraldMin, int emeraldMax, boolean paysEmeralds) {
        int fitPrice(int saved) {
            return ProfessionPicks.fitPrice(saved, emeraldMin, emeraldMax, paysEmeralds);
        }
    }

    static Map<String, Integer> resolve(Map<String, Integer> saved, Function<String, Offer> catalog) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        for (var pick : saved.entrySet()) {
            Offer offer = catalog.apply(pick.getKey());
            if (offer != null && offer.eligibility().canReroll()) {
                int ceiling = offer.fitPrice(pick.getValue());
                if (ceiling > 0) {
                    targets.put(pick.getKey(), ceiling);
                }
            }
        }
        return targets;
    }
}
