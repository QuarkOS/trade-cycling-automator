package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiStatusTest {
    @Test
    void activeRunsTakePriorityOverPreviousResults() {
        assertEquals(UiStatus.RUNNING, UiStatus.forRun(true, false, JobSiteReroller.StopReason.TIMEOUT));
        assertEquals(UiStatus.IDLE, UiStatus.forRun(false, false, null));
        assertEquals(UiStatus.READY, UiStatus.forRun(false, true, null));
        assertEquals(UiStatus.READY, UiStatus.forRun(false, false, JobSiteReroller.StopReason.FOUND));
        assertEquals(UiStatus.ERROR, UiStatus.forRun(false, true, JobSiteReroller.StopReason.STOPPED));
        assertEquals(UiStatus.ERROR, UiStatus.forRun(false, true, JobSiteReroller.StopReason.TIMEOUT));
    }

    @Test
    void hallResultsUseTheSameStatusColors() {
        assertEquals(UiStatus.READY, UiStatus.forOutcome(TradeHall.Outcome.FOUND));
        assertEquals(UiStatus.RUNNING, UiStatus.forOutcome(TradeHall.Outcome.PENDING));
        assertEquals(UiStatus.RUNNING, UiStatus.forOutcome(TradeHall.Outcome.SKIPPED));
        assertEquals(UiStatus.ERROR, UiStatus.forOutcome(TradeHall.Outcome.CANCELLED));
        assertEquals(UiStatus.ERROR, UiStatus.forOutcome(TradeHall.Outcome.FAILED));
    }
}
