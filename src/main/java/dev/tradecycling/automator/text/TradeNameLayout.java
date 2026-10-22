package dev.tradecycling.automator.text;

/**
 * Horizontal boxes for a trade row. The name is clipped to its own box, and
 * that box ends {@link #PRICE_GAP} pixels before the right-aligned price.
 */
public final class TradeNameLayout {
    public static final int NAME_LEFT = 36;
    public static final int PRICE_GAP = 8;
    public static final int PRICE_RIGHT = 16;

    private TradeNameLayout() {
    }

    public static int priceX(int rowLeft, int rowWidth, int priceWidth) {
        return rowLeft + rowWidth - PRICE_RIGHT - priceWidth;
    }

    public static int nameLeft(int rowLeft) {
        return rowLeft + NAME_LEFT;
    }

    /**
     * Exclusive right edge of the name scissor.
     */
    public static int nameRight(int priceX) {
        return priceX - PRICE_GAP;
    }

    public static int availableWidth(int rowWidth, int priceWidth) {
        return nameRight(priceX(0, rowWidth, priceWidth)) - NAME_LEFT;
    }

    /**
     * Exclusive right edge of the name advance after the scissor is applied.
     * {@code offset} is how many pixels the string is shifted left.
     */
    public static int clippedRight(int nameLeft, int nameRight, int textWidth, int offset) {
        int advanceRight = nameLeft + textWidth - Math.max(0, offset);
        return Math.min(advanceRight, nameRight);
    }
}
