package dev.quarkos.villagerreroller.picker;

public enum UiStatus {
    IDLE(UiTheme.MUTED),
    READY(UiTheme.GREEN),
    RUNNING(UiTheme.AMBER),
    ERROR(UiTheme.RED);

    private final int color;

    UiStatus(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    static UiStatus forRun(boolean running, boolean ready, JobSiteReroller.StopReason result) {
        if (running) {
            return RUNNING;
        }
        if (result != null) {
            return result == JobSiteReroller.StopReason.FOUND ? READY : ERROR;
        }
        return ready ? READY : IDLE;
    }

    static UiStatus forOutcome(TradeHall.Outcome outcome) {
        return switch (outcome) {
            case FOUND -> READY;
            case SKIPPED, PENDING -> RUNNING;
            case FAILED, CANCELLED -> ERROR;
        };
    }
}
