package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

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

    private static void assertNoOverlap(int screenWidth, MerchantLayout.Placement place) {
        assertTrue(place.merchantLeft() >= MerchantLayout.PANEL_MARGIN);
        assertTrue(place.merchantLeft() + MerchantLayout.MERCHANT_WIDTH <= screenWidth);
        assertTrue(place.panelX() >= place.merchantLeft() + MerchantLayout.MERCHANT_WIDTH);
        assertTrue(place.panelX() + place.panelWidth() <= screenWidth - MerchantLayout.PANEL_MARGIN);
        assertTrue(place.panelWidth() >= MerchantLayout.PANEL_MIN);
    }
}
