package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.ChecklistSections;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import org.junit.jupiter.api.Test;

import static dev.quarkos.villagerreroller.picker.UiLayout.BUTTON_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.ROW_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLayoutTest {
    @Test
    void narrowFilterRowKeepsEveryButtonInsideTheFrame() {
        Rect frame = UiLayout.frame(284, 160, 424, 280);
        assertEquals(new Rect(4, 4, 276, 152), frame);
        Rect inner = frame.inset(UiLayout.PADDING);
        int searchWidth = inner.width() / 4;
        Rect filters = new Rect(inner.x() + searchWidth + GAP, inner.y(),
                inner.width() - searchWidth - GAP, BUTTON_HEIGHT);
        Rect previous = filters.column(0, 4);
        assertEquals(filters.x(), previous.x());
        for (int i = 1; i < 4; i++) {
            Rect next = filters.column(i, 4);
            assertEquals(previous.right() + GAP, next.x());
            assertTrue(next.width() >= 40);
            previous = next;
        }
        assertEquals(inner.right(), previous.right());
    }

    @Test
    void shortChecklistFitsBothListsSideBySide() {
        Rect body = new Rect(12, 60, 260, 64);
        ChecklistSections sections = ChecklistSections.place(body, 8);
        assertEquals(body.y(), sections.wanted().y());
        assertEquals(body.y(), sections.villagers().y());
        assertEquals(body.bottom(), sections.villagers().bottom());
        assertEquals(body.right(), sections.villagers().right());
        assertEquals(sections.wanted().right() + GAP, sections.villagers().x());
        assertTrue(sections.wanted().height() >= LINE_HEIGHT + GAP + BUTTON_HEIGHT);
        assertTrue(sections.villagers().height() >= LINE_HEIGHT + GAP + ROW_HEIGHT);
    }

    @Test
    void tallerChecklistLeavesRoomForACompleteVillagerRow() {
        Rect body = new Rect(12, 60, 260, 90);
        ChecklistSections sections = ChecklistSections.place(body, 8);
        assertEquals(body.width(), sections.wanted().width());
        assertEquals(sections.wanted().bottom() + GAP, sections.villagers().y());
        assertEquals(body.bottom(), sections.villagers().bottom());
        assertTrue(sections.villagers().height() >= LINE_HEIGHT + GAP + ROW_HEIGHT);
    }
}
