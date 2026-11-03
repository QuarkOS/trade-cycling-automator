package dev.quarkos.villagerreroller.picker;

/**
 * Where the vanilla merchant screen and the Reroll panel sit.
 * The merchant is 276 pixels wide. When the centered screen does not leave a
 * usable gap, and its background follows {@code leftPos}, the screen slides
 * left the way a recipe book does. Otherwise the panel stays closed so it
 * cannot cover the result slot.
 */
public final class MerchantLayout {
    public static final int MERCHANT_WIDTH = 276;
    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_MIN = 112;
    public static final int PANEL_GAP = 4;
    public static final int PANEL_MARGIN = 2;

    private MerchantLayout() {
    }

    public record Placement(int merchantLeft, int panelX, int panelWidth, boolean shown) {
    }

    public static Placement place(int screenWidth, boolean backgroundFollows) {
        int naturalLeft = (screenWidth - MERCHANT_WIDTH) / 2;
        int room = screenWidth - PANEL_MARGIN - naturalLeft - MERCHANT_WIDTH - PANEL_GAP;
        int panelW;
        if (room >= PANEL_MIN || !backgroundFollows) {
            panelW = Math.min(PANEL_WIDTH, Math.max(0, room));
        } else {
            int slidLeft = Math.max(PANEL_MARGIN, screenWidth - PANEL_MARGIN - PANEL_WIDTH - PANEL_GAP - MERCHANT_WIDTH);
            int slidRoom = screenWidth - PANEL_MARGIN - slidLeft - MERCHANT_WIDTH - PANEL_GAP;
            panelW = Math.min(PANEL_WIDTH, Math.max(0, slidRoom));
        }
        int merchantLeft = naturalLeft;
        if (backgroundFollows) {
            int needed = screenWidth - PANEL_MARGIN - panelW - PANEL_GAP - MERCHANT_WIDTH;
            merchantLeft = Math.max(PANEL_MARGIN, Math.min(naturalLeft, needed));
        }
        panelW = Math.min(PANEL_WIDTH, Math.max(0, screenWidth - PANEL_MARGIN - merchantLeft - MERCHANT_WIDTH - PANEL_GAP));
        int panelX = merchantLeft + MERCHANT_WIDTH + PANEL_GAP;
        if (panelX + panelW > screenWidth - PANEL_MARGIN) {
            panelW = Math.max(0, screenWidth - PANEL_MARGIN - panelX);
        }
        if (panelW < PANEL_MIN) {
            return new Placement(naturalLeft, 0, 0, false);
        }
        return new Placement(merchantLeft, panelX, panelW, true);
    }
}
