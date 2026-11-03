package dev.quarkos.villagerreroller.picker;

/**
 * Scroll offset and thumb geometry for a vertical list.
 * The thumb height is the visible fraction of the content, and its top tracks the offset.
 * Dragging grabs the thumb. A click on the track pages by one viewport.
 */
public final class ListScroll {
    private int offset;
    private boolean dragging;
    private int grab;

    public int offset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    public void clamp(int max) {
        offset = Math.max(0, Math.min(Math.max(0, max), offset));
    }

    public boolean dragging() {
        return dragging;
    }

    public void stop() {
        dragging = false;
    }

    public static int maxOffset(int items, int visible) {
        return Math.max(0, items - Math.max(1, visible));
    }

    /** Thumb height in pixels. A list that fits uses the whole track. */
    public static int thumbHeight(int track, int items, int visible) {
        if (track <= 0) {
            return 0;
        }
        int shown = Math.max(1, visible);
        if (items <= shown) {
            return track;
        }
        int thumb = (int) Math.round(track * (shown / (double) items));
        int min = Math.min(track, 10);
        return Math.max(min, Math.min(track, thumb));
    }

    /** Thumb top relative to the track. */
    public static int thumbTop(int track, int thumb, int offset, int max) {
        if (max <= 0 || thumb >= track) {
            return 0;
        }
        int travel = track - thumb;
        int clamped = Math.max(0, Math.min(max, offset));
        return (int) Math.round(clamped / (double) max * travel);
    }

    /**
     * Click inside the track. A hit on the thumb starts a drag; a hit above or below pages.
     * Returns false when the list does not overflow.
     */
    public boolean clickTrack(double mouseY, int trackY, int track, int items, int visible) {
        dragging = false;
        int max = maxOffset(items, visible);
        clamp(max);
        if (max <= 0 || track <= 0) {
            return false;
        }
        int local = (int) Math.floor(mouseY) - trackY;
        if (local < 0 || local >= track) {
            return false;
        }
        int thumb = thumbHeight(track, items, visible);
        int top = thumbTop(track, thumb, offset, max);
        if (local >= top && local < top + thumb) {
            dragging = true;
            grab = local - top;
            return true;
        }
        int page = Math.max(1, visible);
        if (local < top) {
            offset = Math.max(0, offset - page);
        } else {
            offset = Math.min(max, offset + page);
        }
        return true;
    }

    /** Move the grabbed thumb so it stays under the cursor. */
    public boolean drag(double mouseY, int trackY, int track, int items, int visible) {
        if (!dragging) {
            return false;
        }
        int max = maxOffset(items, visible);
        int thumb = thumbHeight(track, items, visible);
        int travel = Math.max(1, track - thumb);
        int local = (int) Math.floor(mouseY) - trackY - grab;
        offset = (int) Math.round(local / (double) travel * max);
        clamp(max);
        return true;
    }

    /** One wheel notch moves one row. Positive vertical scrolls toward the start. */
    public boolean wheel(double vertical, int items, int visible) {
        int max = maxOffset(items, visible);
        if (max <= 0) {
            return false;
        }
        int delta = (int) Math.signum(vertical);
        if (delta == 0) {
            return false;
        }
        offset = Math.max(0, Math.min(max, offset - delta));
        return true;
    }
}
