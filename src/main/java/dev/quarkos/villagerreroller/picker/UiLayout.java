package dev.quarkos.villagerreroller.picker;

/** Shared spacing in scaled GUI pixels. */
public final class UiLayout {
    public static final int GAP = 4;
    public static final int PADDING = 8;
    public static final int LINE_HEIGHT = 10;
    public static final int BUTTON_HEIGHT = 20;
    public static final int ROW_HEIGHT = 28;
    public static final int SCROLLBAR_SPACE = 8;

    private UiLayout() {
    }

    public static Rect frame(int screenWidth, int screenHeight, int maxWidth, int maxHeight) {
        int width = Math.min(maxWidth, Math.max(0, screenWidth - 2 * GAP));
        int height = Math.min(maxHeight, Math.max(0, screenHeight - 2 * GAP));
        return new Rect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public Rect inset(int padding) {
            return new Rect(x + padding, y + padding, Math.max(0, width - 2 * padding),
                    Math.max(0, height - 2 * padding));
        }

        public Rect column(int index, int count) {
            int usable = width - GAP * (count - 1);
            int start = usable * index / count;
            int end = usable * (index + 1) / count;
            return new Rect(x + start + GAP * index, y, end - start, height);
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    public record ChecklistSections(Rect wanted, Rect villagers) {
        public static ChecklistSections place(Rect body, int wantedRows) {
            int heading = LINE_HEIGHT + GAP;
            int wantedHeight = heading + Math.max(1, Math.min(3, wantedRows)) * BUTTON_HEIGHT;
            int maxWantedHeight = body.height() - heading - ROW_HEIGHT - GAP;
            if (maxWantedHeight < heading + BUTTON_HEIGHT) {
                return new ChecklistSections(body.column(0, 2), body.column(1, 2));
            }
            int split = Math.min(wantedHeight, maxWantedHeight) + GAP;
            return new ChecklistSections(
                    new Rect(body.x(), body.y(), body.width(), split - GAP),
                    new Rect(body.x(), body.y() + split, body.width(), body.height() - split));
        }
    }
}
