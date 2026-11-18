package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.Rect;

import static dev.quarkos.villagerreroller.picker.UiLayout.BUTTON_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;

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
    public static final int TRADE_ROW_HEIGHT = 20;
    private static final int PANEL_HEIGHT = 248;

    private MerchantLayout() {
    }

    public record Placement(int merchantLeft, int panelX, int panelWidth, boolean shown) {
    }

    public record Panel(Rect bounds, Rect title, Rect search, Rect trades, Rect status,
                        Rect method, Rect navigation, Rect action, Rect back, int statusLines) {
        public static Panel place(Placement placement, int screenHeight) {
            int height = Math.min(PANEL_HEIGHT, screenHeight - 2 * PANEL_MARGIN);
            Rect bounds = new Rect(placement.panelX(), (screenHeight - height) / 2,
                    placement.panelWidth(), height);
            return layout(bounds, false);
        }

        public static Panel compact(int screenWidth, int screenHeight) {
            int width = Math.min(240, screenWidth - 2 * PANEL_MARGIN);
            int height = Math.min(PANEL_HEIGHT, screenHeight - 2 * PANEL_MARGIN);
            return layout(new Rect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height), true);
        }

        private static Panel layout(Rect bounds, boolean compact) {
            boolean sharedFooter = bounds.height() < PANEL_HEIGHT;
            Rect inner = bounds.inset(compact ? 6 : PADDING);
            Rect back = compact ? new Rect(inner.right() - 56, inner.y(), 56, 16) : null;
            Rect title = back == null ? new Rect(inner.x(), inner.y(), inner.width(), LINE_HEIGHT)
                    : new Rect(inner.x(), inner.y(), back.x() - GAP - inner.x(), back.height());
            Rect search = new Rect(inner.x(), title.bottom() + GAP, inner.width(), BUTTON_HEIGHT);
            int actionY = inner.bottom() - BUTTON_HEIGHT;
            int methodWidth = sharedFooter ? inner.width() * 2 / 5 : inner.width();
            Rect action = sharedFooter
                    ? new Rect(inner.x() + methodWidth + GAP, actionY, inner.width() - methodWidth - GAP, BUTTON_HEIGHT)
                    : new Rect(inner.x(), actionY, inner.width(), BUTTON_HEIGHT);
            Rect navigation = new Rect(inner.x(), actionY - BUTTON_HEIGHT - GAP, inner.width(), BUTTON_HEIGHT);
            Rect method = new Rect(inner.x(), sharedFooter ? actionY : navigation.y() - BUTTON_HEIGHT - GAP,
                    methodWidth, BUTTON_HEIGHT);
            int statusLines = bounds.height() < 188 ? 2 : 3;
            int statusHeight = statusLines * LINE_HEIGHT + 2 * GAP;
            Rect status = new Rect(inner.x(), Math.min(method.y(), navigation.y()) - GAP - statusHeight,
                    inner.width(), statusHeight);
            int listY = search.bottom() + GAP;
            Rect trades = new Rect(inner.x(), listY, inner.width(), status.y() - GAP - listY);
            return new Panel(bounds, title, search, trades, status, method, navigation, action, back, statusLines);
        }

        public boolean shown() {
            return bounds.width() >= PANEL_MIN && trades.height() >= TRADE_ROW_HEIGHT;
        }
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
