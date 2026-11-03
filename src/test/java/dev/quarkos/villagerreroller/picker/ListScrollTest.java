package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListScrollTest {
    @Test
    void thumbFillsTheTrackWhenEverythingFits() {
        assertEquals(0, ListScroll.maxOffset(4, 4));
        assertEquals(80, ListScroll.thumbHeight(80, 3, 4));
        assertEquals(0, ListScroll.thumbTop(80, 80, 0, 0));
    }

    @Test
    void thumbShrinksAndTracksTheOffset() {
        int track = 100;
        int items = 20;
        int visible = 4;
        int max = ListScroll.maxOffset(items, visible);
        assertEquals(16, max);
        int thumb = ListScroll.thumbHeight(track, items, visible);
        assertTrue(thumb < track);
        assertTrue(thumb >= 10);
        assertEquals(0, ListScroll.thumbTop(track, thumb, 0, max));
        int mid = ListScroll.thumbTop(track, thumb, max / 2, max);
        int end = ListScroll.thumbTop(track, thumb, max, max);
        assertTrue(mid > 0 && mid < end);
        assertEquals(track - thumb, end);
    }

    @Test
    void trackClickPagesAndThumbDragFollowsThePointer() {
        ListScroll scroll = new ListScroll();
        assertTrue(scroll.clickTrack(90, 0, 100, 20, 4));
        assertEquals(4, scroll.offset());
        assertFalse(scroll.dragging());

        scroll.setOffset(0);
        int thumb = ListScroll.thumbHeight(100, 20, 4);
        assertTrue(scroll.clickTrack(thumb / 2.0, 0, 100, 20, 4));
        assertTrue(scroll.dragging());
        assertEquals(0, scroll.offset());
        assertTrue(scroll.drag(100 - thumb / 2.0, 0, 100, 20, 4));
        assertEquals(ListScroll.maxOffset(20, 4), scroll.offset());
    }

    @Test
    void wheelMovesOneRowAndStopsAtTheEnds() {
        ListScroll scroll = new ListScroll();
        assertFalse(scroll.wheel(-1, 2, 4));
        assertTrue(scroll.wheel(-1, 10, 3));
        assertEquals(1, scroll.offset());
        assertTrue(scroll.wheel(1, 10, 3));
        assertEquals(0, scroll.offset());
        scroll.setOffset(ListScroll.maxOffset(10, 3));
        assertTrue(scroll.wheel(-1, 10, 3));
        assertEquals(ListScroll.maxOffset(10, 3), scroll.offset());
    }
}
