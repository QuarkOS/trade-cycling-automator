package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceRankingTest {
    @Test
    void ranksPriceBeforeDistanceAndUsesDistanceForTies() {
        var cheapFar = offer(1, 5, 900);
        var near = offer(2, 8, 1);
        var far = offer(3, 8, 16);
        var expensive = offer(4, 20, 0);
        var ranked = PriceRanking.rank(List.of(expensive, far, near, cheapFar));
        assertEquals(List.of(cheapFar, near, far, expensive), ranked.stream().map(PriceRanking.Ranked::offer).toList());
        assertEquals(List.of(1, 2, 3, 4), ranked.stream().map(PriceRanking.Ranked::rank).toList());
        assertEquals(List.of(UiTheme.GREEN, UiTheme.YELLOW, UiTheme.AMBER, UiTheme.RED),
                ranked.stream().map(PriceRanking.Ranked::color).toList());
    }

    @Test
    void deduplicatesOffersAndBreaksRemainingTiesById() {
        var cheapest = offer(1, 4, 64);
        var other = offer(2, 4, 64);
        assertEquals(List.of(cheapest, other), PriceRanking.rank(List.of(other, offer(1, 12, 64), cheapest))
                .stream().map(PriceRanking.Ranked::offer).toList());
    }

    @Test
    void oneOfferIsGreenAndNoOffersProducesNoRanks() {
        assertEquals(List.of(), PriceRanking.rank(List.of()));
        assertEquals(List.of(new PriceRanking.Ranked(offer(1, 4, 0), 1, UiTheme.GREEN)),
                PriceRanking.rank(List.of(offer(1, 4, 0))));
    }

    private static PriceRanking.Offer offer(int id, int price, double distanceSquared) {
        return new PriceRanking.Offer(new UUID(0, id), price, distanceSquared);
    }
}
