package dev.quarkos.villagerreroller.text;

/**
 * Horizontal back-and-forth offset used by vanilla widgets.
 * The period matches {@code ActiveTextCollector.PERIOD_PER_SCROLLED_PIXEL}
 * and {@code MIN_SCROLL_PERIOD}: half a second per overflow pixel, and at least
 * three seconds for a full trip to the end and back.
 */
public final class ScrollingNames {
    public static final double PERIOD_PER_PIXEL = 0.5;
    public static final double MIN_PERIOD_SECONDS = 3.0;

    private ScrollingNames() {
    }

    /**
     * @param textWidth      width of the full string in pixels
     * @param availableWidth width of the clip rectangle
     * @param epochMillis    {@code Util.getMillis()}
     * @return how many pixels to shift the string left, from 0 through the overflow
     */
    public static int offset(int textWidth, int availableWidth, long epochMillis) {
        if (availableWidth <= 0 || textWidth <= availableWidth) {
            return 0;
        }
        int overflow = textWidth - availableWidth;
        double seconds = epochMillis / 1000.0;
        double period = Math.max(overflow * PERIOD_PER_PIXEL, MIN_PERIOD_SECONDS);
        double angle = (Math.PI * 2.0) * seconds / period;
        double wave = Math.sin((Math.PI / 2.0) * Math.cos(angle)) / 2.0 + 0.5;
        int shifted = (int) (wave * overflow);
        if (shifted < 0) {
            return 0;
        }
        return Math.min(shifted, overflow);
    }
}
