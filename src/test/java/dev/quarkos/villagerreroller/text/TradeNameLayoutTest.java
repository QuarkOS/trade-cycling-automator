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
    void clippedNameStaysOutOfThePriceAtEveryOffset() {
        int textWidth = 220;
        int rowLeft = 468;
        for (int priceWidth = 6; priceWidth <= 36; priceWidth += 5) {
            int priceX = TradeNameLayout.priceX(rowLeft, ROW_WIDTH, priceWidth);
            int nameLeft = TradeNameLayout.nameLeft(rowLeft);
            int nameRight = TradeNameLayout.nameRight(priceX);
            int available = nameRight - nameLeft;
            int overflow = textWidth - available;
            for (int offset = 0; offset <= overflow; offset++) {
                int visibleRight = TradeNameLayout.clippedRight(nameLeft, nameRight, textWidth, offset);
                assertTrue(
                        visibleRight <= priceX - TradeNameLayout.PRICE_GAP,
                        "price " + priceWidth + " offset " + offset + " reached " + visibleRight + " priceX " + priceX
                );
                // A glyph one pixel past its advance is still cut by the same scissor.
                int overhang = Math.min(nameLeft + textWidth - offset + 1, nameRight);
                assertTrue(overhang <= priceX - TradeNameLayout.PRICE_GAP);
            }
            int atEnd = TradeNameLayout.clippedRight(nameLeft, nameRight, textWidth, overflow);
            assertEquals(nameRight, atEnd, "maximum scroll should sit on the name clip");
            assertTrue(priceX - atEnd >= TradeNameLayout.PRICE_GAP);
        }
    }
}
