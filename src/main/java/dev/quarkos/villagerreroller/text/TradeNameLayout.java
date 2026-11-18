package dev.quarkos.villagerreroller.text;

/**
 * Horizontal boxes for a trade row. The name is fitted to its own box, and
 * that box ends {@link #PRICE_GAP} pixels before the right-aligned price.
 */
public final class TradeNameLayout {
    public static final int NAME_LEFT = 24;
    public static final int PRICE_GAP = 4;
    public static final int PRICE_RIGHT = 18;
    private static final int MIN_NAME_WIDTH = 20;

    private TradeNameLayout() {
    }

    public static int priceX(int rowLeft, int rowWidth, int priceWidth) {
        return rowLeft + rowWidth - PRICE_RIGHT - priceWidth;
    }

    public static int nameLeft(int rowLeft) {
        return rowLeft + NAME_LEFT;
    }

    /**
     * Exclusive right edge of the name box.
     */
    public static int nameRight(int priceX) {
        return priceX - PRICE_GAP;
    }

    public static int availableWidth(int rowWidth, int priceWidth) {
        return nameRight(priceX(0, rowWidth, priceWidth)) - NAME_LEFT;
    }

    public static int priceBudget(int rowWidth) {
        return Math.max(0, availableWidth(rowWidth, 0) - MIN_NAME_WIDTH);
    }
}
