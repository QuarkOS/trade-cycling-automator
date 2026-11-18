package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import org.junit.jupiter.api.Test;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantLayoutTest {
    @Test
    void narrowWindowSlidesTheMerchantToFreeAPanel() {
        // 854x480 at GUI scale 2.
        MerchantLayout.Placement place = MerchantLayout.place(427, true);
        assertTrue(place.shown());
        assertEquals(2, place.merchantLeft());
        assertNoOverlap(427, place);
    }

    @Test
    void scaleThreeOnAShortWindowLeavesTheMerchantAlone() {
        // 854x480 at GUI scale 3. A 276-wide screen cannot spare a panel.
        MerchantLayout.Placement place = MerchantLayout.place(284, true);
        assertFalse(place.shown());
        assertEquals((284 - MerchantLayout.MERCHANT_WIDTH) / 2, place.merchantLeft());
    }

    @Test
    void wideWindowKeepsTheMerchantCentered() {
        // 1920x1080 at GUI scale 2 and 3.
        for (int width : new int[]{960, 640}) {
            MerchantLayout.Placement place = MerchantLayout.place(width, true);
            assertTrue(place.shown(), "width " + width);
            assertEquals(MerchantLayout.PANEL_WIDTH, place.panelWidth(), "width " + width);
            assertEquals((width - MerchantLayout.MERCHANT_WIDTH) / 2, place.merchantLeft(), "width " + width);
            assertNoOverlap(width, place);
        }
    }

    @Test
    void autoScaleOnALargeWindowSlidesJustEnough() {
        // 1920x1080 auto is GUI scale 4, 480 pixels wide.
        MerchantLayout.Placement place = MerchantLayout.place(480, true);
        assertTrue(place.shown());
        assertEquals(22, place.merchantLeft());
        assertEquals(MerchantLayout.PANEL_WIDTH, place.panelWidth());
        assertNoOverlap(480, place);
    }

    @Test
    void withoutTheBackgroundShiftATightWindowDoesNotOverlap() {
        MerchantLayout.Placement place = MerchantLayout.place(427, false);
        assertFalse(place.shown());
        assertEquals((427 - MerchantLayout.MERCHANT_WIDTH) / 2, place.merchantLeft());
    }

    @Test
    void smallestSidePanelKeepsAFullTradeRowAndAllControls() {
        int width = MerchantLayout.MERCHANT_WIDTH + MerchantLayout.PANEL_MIN
                + MerchantLayout.PANEL_GAP + 2 * MerchantLayout.PANEL_MARGIN;
        MerchantLayout.Placement placement = MerchantLayout.place(width, true);
        assertEquals(MerchantLayout.PANEL_MIN, placement.panelWidth());
        MerchantLayout.Panel panel = MerchantLayout.Panel.place(placement, 160);

        assertTrue(panel.shown());
        assertEquals(2, panel.statusLines());
        assertEquals(panel.method().y(), panel.action().y());
        assertTrue(panel.action().x() >= panel.method().right() + GAP);
        assertTrue(panel.action().width() > panel.method().width());
        checkPanel(panel);
    }

    @Test
    void tallerPanelGivesOddsAndTimeSeparateLines() {
        MerchantLayout.Placement placement = MerchantLayout.place(640, true);
        MerchantLayout.Panel panel = MerchantLayout.Panel.place(placement, 280);
        assertTrue(panel.shown());
        assertEquals(3, panel.statusLines());
        assertTrue(panel.method().bottom() + GAP <= panel.navigation().y());
        assertEquals(panel.search().width(), panel.action().width());
        checkPanel(panel);
    }

    @Test
    void standardScaledHeightKeepsFourRowsAndTheTimeEstimate() {
        MerchantLayout.Panel panel = MerchantLayout.Panel.place(MerchantLayout.place(427, true), 240);
        assertTrue(panel.shown());
        assertEquals(3, panel.statusLines());
        assertTrue(panel.trades().height() >= 4 * MerchantLayout.TRADE_ROW_HEIGHT);
        checkPanel(panel);
    }

    @Test
    void panelStaysClosedWhenThereIsNoRoomForAFullRow() {
        assertFalse(MerchantLayout.Panel.place(MerchantLayout.place(640, true), 130).shown());
    }

    @Test
    void compactPanelFitsBoth1280WideWindowsAtScaleFour() {
        MerchantLayout.Placement side = MerchantLayout.place(320, true);
        assertFalse(side.shown());
        MerchantLayout.Panel shortPanel = MerchantLayout.Panel.compact(320, 180);
        MerchantLayout.Panel tallPanel = MerchantLayout.Panel.compact(320, 240);

        assertTrue(shortPanel.trades().height() >= 2 * MerchantLayout.TRADE_ROW_HEIGHT);
        assertTrue(tallPanel.trades().height() >= 4 * MerchantLayout.TRADE_ROW_HEIGHT);
        for (MerchantLayout.Panel panel : new MerchantLayout.Panel[]{shortPanel, tallPanel}) {
            assertTrue(panel.shown());
            assertTrue(panel.bounds().x() >= MerchantLayout.PANEL_MARGIN);
            assertTrue(panel.bounds().right() <= 320 - MerchantLayout.PANEL_MARGIN);
            assertEquals(panel.title().right() + GAP, panel.back().x());
            assertEquals(panel.title().bottom(), panel.back().bottom());
            assertEquals(panel.search().right(), panel.back().right());
            checkPanel(panel);
        }
        assertTrue(shortPanel.bounds().bottom() <= 180 - MerchantLayout.PANEL_MARGIN);
        assertTrue(tallPanel.bounds().bottom() <= 240 - MerchantLayout.PANEL_MARGIN);
    }

    private static void checkPanel(MerchantLayout.Panel panel) {
        assertTrue(panel.title().bottom() + GAP <= panel.search().y());
        assertTrue(panel.search().bottom() + GAP <= panel.trades().y());
        assertTrue(panel.trades().height() >= MerchantLayout.TRADE_ROW_HEIGHT);
        assertTrue(panel.trades().bottom() + GAP <= panel.status().y());
        assertTrue(panel.status().bottom() + GAP <= Math.min(panel.method().y(), panel.navigation().y()));
        assertTrue(panel.navigation().bottom() + GAP <= panel.action().y());
        Rect bounds = panel.bounds();
        for (Rect child : new Rect[]{panel.title(), panel.search(), panel.trades(), panel.status(),
                panel.method(), panel.navigation(), panel.action()}) {
            assertTrue(child.x() > bounds.x());
            assertTrue(child.right() < bounds.right());
            assertTrue(child.y() > bounds.y());
            assertTrue(child.bottom() < bounds.bottom());
        }
    }

    private static void assertNoOverlap(int screenWidth, MerchantLayout.Placement place) {
        assertTrue(place.merchantLeft() >= MerchantLayout.PANEL_MARGIN);
        assertTrue(place.merchantLeft() + MerchantLayout.MERCHANT_WIDTH <= screenWidth);
        assertTrue(place.panelX() >= place.merchantLeft() + MerchantLayout.MERCHANT_WIDTH);
        assertTrue(place.panelX() + place.panelWidth() <= screenWidth - MerchantLayout.PANEL_MARGIN);
        assertTrue(place.panelWidth() >= MerchantLayout.PANEL_MIN);
    }
}
