package dev.quarkos.villagerreroller.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeNameLayoutTest {
    private static final int ROW_WIDTH = 156;

    @Test
    void nameBoxEndsAGapBeforeThePrice() {
        for (int priceWidth = 1; priceWidth <= 40; priceWidth++) {
            int priceX = TradeNameLayout.priceX(0, ROW_WIDTH, priceWidth);
            int nameRight = TradeNameLayout.nameRight(priceX);
            assertEquals(TradeNameLayout.PRICE_GAP, priceX - nameRight, "price width " + priceWidth);
            assertTrue(nameRight > TradeNameLayout.NAME_LEFT, "price width " + priceWidth);
            assertEquals(
                    ROW_WIDTH - TradeNameLayout.NAME_LEFT - TradeNameLayout.PRICE_GAP - TradeNameLayout.PRICE_RIGHT - priceWidth,
                    TradeNameLayout.availableWidth(ROW_WIDTH, priceWidth)
            );
        }
    }

    @Test
    void pickRowsKeepThePriceClearOfTheRemoveButton() {
        int rowLeft = 12;
        for (int width : new int[]{256, 288, 336}) {
            int priceWidth = 47;
            int priceX = TradeNameLayout.priceX(rowLeft, width, priceWidth);
            int nameRight = TradeNameLayout.nameLeft(rowLeft) + TradeNameLayout.availableWidth(width, priceWidth);
            assertEquals(priceX - TradeNameLayout.PRICE_GAP, nameRight);
            assertEquals(rowLeft + width - 18, priceX + priceWidth);
        }
    }

    @Test
    void oversizedTranslatedPriceStillLeavesANameColumnAtMinimumPanelWidth() {
        int width = 112 - 2 * 8;
        int budget = TradeNameLayout.priceBudget(width);
        assertTrue(budget >= 20);
        assertTrue(TradeNameLayout.availableWidth(width, budget) >= 20);
        int priceX = TradeNameLayout.priceX(0, width, budget);
        assertEquals(width - TradeNameLayout.PRICE_RIGHT, priceX + budget);
    }
}
