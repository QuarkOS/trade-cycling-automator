package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.McBridge;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * Queue before a hall run, and the result list after one.
 * The trade targets themselves stay on the merchant panel.
 */
public final class TradeHallScreen extends Screen {
    private static final int ROW = 29;
    private final List<Button> removeButtons = new ArrayList<>();
    private final int[] removeSlot = new int[8];
    private int scroll;
    private int wellX;
    private int wellY;
    private int wellW;
    private int wellH;
    private int visibleRows;
    private int maxScroll;
    private boolean dragging;
    private List<Component> hover = List.of();
    private Button villagersButton;

    public TradeHallScreen() {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        TradeHall.dismiss();
        super.onClose();
    }

    @Override
    protected void init() {
        Frame frame = frame();
        boolean summary = TradeHall.showingSummary();
        int villagersW = Math.min(94, Math.max(40, frame.w / 3));
        villagersButton = addRenderableWidget(FitButton.create(
                frame.left + frame.w - villagersW - 6,
                frame.top + 5,
                villagersW,
                14,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.my_villagers", villagerCount()),
                pressed -> {
                    if (this.minecraft != null) {
                        McBridge.setScreen(this.minecraft, new MyVillagersScreen(this));
                    }
                }));
        for (int i = 0; i < removeSlot.length; i++) {
            int button = i;
            removeSlot[i] = -1;
            Button remove = addRenderableWidget(FitButton.create(
                    0,
                    0,
                    48,
                    16,
                    Component.translatable("text." + VillagerReroller.MOD_ID + ".button.remove"),
                    pressed -> {
                        int slot = removeSlot[button];
                        if (slot >= 0) {
                            TradeHall.remove(slot);
                        }
                        if (this.minecraft != null) {
                            McBridge.setScreen(this.minecraft, new TradeHallScreen());
                        }
                    }));
            remove.visible = false;
            removeButtons.add(remove);
        }
        int y = frame.top + frame.h - 24;
        int gap = 6;
        int buttonW = Math.max(36, (frame.w - 16 - gap * 2) / 3);
        int x = frame.left + 8;
        if (!summary) {
            Button start = addRenderableWidget(FitButton.create(
                    x, y, buttonW, 18,
                    Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall_start"),
                    pressed -> {
                        if (this.minecraft != null) {
                            TradeHall.start(this.minecraft);
                        }
                    }));
            start.active = TradeHall.size() > 0;
            addRenderableWidget(FitButton.create(
                    x + buttonW + gap, y, buttonW, 18,
                    Component.translatable("text." + VillagerReroller.MOD_ID + ".button.clear"),
                    pressed -> {
                        TradeHall.clear();
                        if (this.minecraft != null) {
                            McBridge.setScreen(this.minecraft, new TradeHallScreen());
                        }
                    }));
            addRenderableWidget(FitButton.create(
                    x + (buttonW + gap) * 2, y, buttonW, 18,
                    Component.translatable("gui.done"),
                    pressed -> onClose()));
        } else {
            addRenderableWidget(FitButton.create(
                    frame.left + (frame.w - buttonW) / 2, y, buttonW, 18,
                    Component.translatable("gui.done"),
                    pressed -> onClose()));
        }
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        paint(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    *///?}

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        hover = List.of();
        Frame frame = frame();
        boolean summary = TradeHall.showingSummary();
        if (villagersButton != null) {
            villagersButton.setMessage(Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".button.my_villagers", villagerCount()));
        }
        GuiDraw.beveled(canvas, frame.left, frame.top, frame.w, frame.h);
        String title = Component.translatable(summary
                ? "text." + VillagerReroller.MOD_ID + ".hall.summary"
                : "text." + VillagerReroller.MOD_ID + ".hall.title").getString();
        int titleMax = Math.max(20, frame.w / 4);
        String titleTip = GuiDraw.fit(canvas, title, frame.left + 8, frame.top + 7, titleMax, 0xFF202020);
        if (titleTip != null && mouseY >= frame.top + 4 && mouseY <= frame.top + 16 && mouseX < frame.left + 8 + titleMax) {
            hover = List.of(Component.literal(title));
        }
        int pills = frame.left + 8 + canvas.font().width(GuiDraw.ellipsis(canvas.font(), title, titleMax)) + 6;
        int pillLimit = villagersButton == null ? frame.left + frame.w - 8 : widgetX(villagersButton) - 4;
        paintPills(canvas, pills, frame.top + 5, pillLimit, summary);
        String hint = Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.hint").getString();
        String hintTip = GuiDraw.fit(canvas, hint, frame.left + 8, frame.top + 21, frame.w - 16, 0xFF555555);
        if (hintTip != null && mouseY >= frame.top + 19 && mouseY <= frame.top + 30 && mouseX >= frame.left + 8 && mouseX <= frame.left + frame.w - 8) {
            hover = List.of(Component.literal(hint));
        }
        wellX = frame.left + 8;
        wellY = frame.top + 33;
        wellW = frame.w - 16;
        wellH = Math.max(ROW, frame.h - 33 - 28);
        GuiDraw.recessed(canvas, wellX, wellY, wellW, wellH, 0xFF8E8E8E);
        List<TradeHall.Slot> slots = TradeHall.slots();
        visibleRows = Math.max(1, (wellH - 4) / ROW);
        maxScroll = Math.max(0, slots.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        boolean showRemove = !summary && !TradeHall.isActive();
        for (Button button : removeButtons) {
            button.visible = false;
            button.active = false;
        }
        if (slots.isEmpty() && !summary) {
            String empty = Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.empty").getString();
            String emptyTip = GuiDraw.fit(canvas, empty, wellX + 8, wellY + 8, wellW - 20, 0xFF404040);
            if (emptyTip != null && inside(mouseX, mouseY, wellX, wellY, wellW, 16)) {
                hover = List.of(Component.literal(empty));
            }
        }
        canvas.scissor(wellX + 1, wellY + 1, wellX + wellW - 8, wellY + wellH - 1);
        for (int i = 0; i < visibleRows && scroll + i < slots.size(); i++) {
            paintSlot(canvas, slots.get(scroll + i), scroll + i, i, summary, showRemove, mouseX, mouseY);
        }
        canvas.unscissor();
        int travel = Math.max(1, wellH - 29);
        int barY = wellY + 1 + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
        canvas.scroller(wellX + wellW - 7, barY, maxScroll == 0);
        canvas.fill(frame.left + 6, frame.top + frame.h - 28, frame.left + frame.w - 6, frame.top + frame.h - 27, 0xFF888888);
        canvas.fill(frame.left + 6, frame.top + frame.h - 27, frame.left + frame.w - 6, frame.top + frame.h - 26, 0xFFE8E8E8);
    }

    private void paintPills(GuiDraw.Canvas canvas, int x, int y, int limit, boolean summary) {
        if (!summary) {
            int ready = 0;
            int skipped = 0;
            for (TradeHall.Slot slot : TradeHall.slots()) {
                if (this.minecraft != null && TradeHall.blocker(this.minecraft, slot.villagerId()) != null) {
                    skipped++;
                } else {
                    ready++;
                }
            }
            String readyText = ready + " " + Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.ready").getString();
            String skipText = skipped + " " + Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.skipped_short").getString();
            if (x + canvas.font().width(readyText) + 8 < limit) {
                x += GuiDraw.pill(canvas, x, y, readyText, 0xFFD7ECD9, 0xFF2E7D32, 0xFF1B5E20) + 3;
            }
            if (skipped > 0 && x + canvas.font().width(skipText) + 8 < limit) {
                GuiDraw.pill(canvas, x, y, skipText, 0xFFFFF0C2, 0xFFC77700, 0xFF8A5600);
            }
            return;
        }
        int found = 0;
        int skipped = 0;
        int failed = 0;
        for (TradeHall.Slot slot : TradeHall.slots()) {
            switch (slot.outcome()) {
                case FOUND -> found++;
                case SKIPPED, CANCELLED -> skipped++;
                case FAILED -> failed++;
                default -> {
                }
            }
        }
        String foundText = Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.found_count", found).getString();
        String skipText = Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.skipped_count", skipped).getString();
        String failText = Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.failed_count", failed).getString();
        if (x + canvas.font().width(foundText) + 8 < limit) {
            x += GuiDraw.pill(canvas, x, y, foundText, 0xFFD7ECD9, 0xFF2E7D32, 0xFF1B5E20) + 3;
        }
        if (x + canvas.font().width(skipText) + 8 < limit) {
            x += GuiDraw.pill(canvas, x, y, skipText, 0xFFFFF0C2, 0xFFC77700, 0xFF8A5600) + 3;
        }
        if (x + canvas.font().width(failText) + 8 < limit) {
            int fill = failed > 0 ? 0xFFFADBD8 : 0xFFE0E0E0;
            int border = failed > 0 ? 0xFFAA0000 : 0xFF808080;
            int color = failed > 0 ? 0xFFAA0000 : 0xFF505050;
            GuiDraw.pill(canvas, x, y, failText, fill, border, color);
        }
    }

    private void paintSlot(GuiDraw.Canvas canvas, TradeHall.Slot slot, int index, int visible, boolean summary, boolean showRemove, int mouseX, int mouseY) {
        int y = wellY + 2 + visible * ROW;
        Kind kind = kind(slot, summary);
        int fill = kind == Kind.SKIPPED ? 0xFFDDD5C4 : (visible % 2 == 0 ? 0xFFD4D4D4 : 0xFFCACACA);
        int rowW = wellW - 14;
        canvas.fill(wellX + 1, y, wellX + 1 + rowW, y + ROW - 2, fill);
        canvas.fill(wellX + 1, y, wellX + 4, y + ROW - 2, kind.stripe);
        canvas.fill(wellX + 1, y + ROW - 2, wellX + 1 + rowW, y + ROW - 1, 0xFF787878);
        String identity = (index + 1) + ". " + slot.label().getString();
        if (!slot.place().isEmpty()) {
            identity += " \u00b7 " + slot.place();
        }
        String status = status(slot, summary).getString();
        int statusW = canvas.font().width(status) + 8;
        int textMax = Math.max(16, rowW - (showRemove ? 56 : 8) - statusW - 16);
        String identityShown = GuiDraw.ellipsis(canvas.font(), identity, textMax);
        canvas.text(identityShown, wellX + 8, y + 4, 0xFF202020);
        int statusX = wellX + 8 + canvas.font().width(identityShown) + 6;
        int statusRight = wellX + rowW - (showRemove ? 54 : 4);
        if (statusX + 12 < statusRight) {
            String statusShown = GuiDraw.ellipsis(canvas.font(), status, statusRight - statusX);
            GuiDraw.pill(canvas, statusX, y + 2, statusShown, kind.pill, kind.border, kind.text);
        }
        String description = slot.description();
        int descMax = rowW - (showRemove ? 60 : 12);
        String descShown = GuiDraw.ellipsis(canvas.font(), description, descMax);
        canvas.text(descShown, wellX + 8, y + 16, 0xFF505050);
        if (showRemove && visible < removeButtons.size()) {
            Button button = removeButtons.get(visible);
            removeSlot[visible] = index;
            FitButton.place(button, wellX + rowW - 52, y + 6);
            button.visible = true;
            button.active = true;
        }
        boolean over = mouseX >= wellX && mouseX < wellX + rowW && mouseY >= y && mouseY < y + ROW;
        if (over && (!identityShown.equals(identity) || !descShown.equals(description))) {
            hover = List.of(Component.literal(identity), Component.literal(status), Component.literal(description));
        }
    }

    private Component status(TradeHall.Slot slot, boolean summary) {
        if (summary || slot.outcome() != TradeHall.Outcome.PENDING) {
            return TradeHall.outcomeLine(slot);
        }
        if (this.minecraft == null) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.pending");
        }
        String blocker = TradeHall.blocker(this.minecraft, slot.villagerId());
        if (blocker == null) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.ready");
        }
        return Component.translatable(
                "text." + VillagerReroller.MOD_ID + ".hall.skipped",
                Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.why." + blocker));
    }

    private Kind kind(TradeHall.Slot slot, boolean summary) {
        if (!summary && slot.outcome() == TradeHall.Outcome.PENDING) {
            if (this.minecraft != null && TradeHall.blocker(this.minecraft, slot.villagerId()) != null) {
                return Kind.SKIPPED;
            }
            return Kind.READY;
        }
        return switch (slot.outcome()) {
            case FOUND -> Kind.FOUND;
            case SKIPPED, CANCELLED -> Kind.SKIPPED;
            case FAILED -> Kind.FAILED;
            case PENDING -> Kind.READY;
        };
    }

    private int villagerCount() {
        return this.minecraft == null ? 0 : VillagerRosterClient.count(this.minecraft);
    }

    private Frame frame() {
        int w = Math.min(364, Math.max(80, this.width - 4));
        int h = Math.min(212, Math.max(80, this.height - 4));
        return new Frame(Math.max(2, (this.width - w) / 2), Math.max(2, (this.height - h) / 2), w, h);
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        if (!inside(mouseX, mouseY, wellX, wellY, wellW, wellH)) {
            return false;
        }
        int delta = (int) Math.signum(vertical);
        if (delta == 0) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - delta));
        return true;
    }

    //? if <1.21.9 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, wellX + wellW - 8, wellY, 8, wellH) && maxScroll > 0) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int travel = Math.max(1, wellH - 29);
            scroll = Math.max(0, Math.min(maxScroll, (int) ((mouseY - wellY) / (double) travel * maxScroll)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && inside(event.x(), event.y(), wellX + wellW - 8, wellY, 8, wellH) && maxScroll > 0) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            int travel = Math.max(1, wellH - 29);
            scroll = Math.max(0, Math.min(maxScroll, (int) ((event.y() - wellY) / (double) travel * maxScroll)));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }
    //?}

    //? if <1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scrollBy(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (scrollBy(mouseX, mouseY, vertical)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    //?}

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int widgetX(net.minecraft.client.gui.components.AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.x;
        *///?} else {
        return widget.getX();
        //?}
    }

    private enum Kind {
        READY(0xFF2E7D32, 0xFFD7ECD9, 0xFF2E7D32, 0xFF1B5E20),
        FOUND(0xFF2E7D32, 0xFFD7ECD9, 0xFF2E7D32, 0xFF1B5E20),
        SKIPPED(0xFFC77700, 0xFFFFF0C2, 0xFFC77700, 0xFF8A5600),
        FAILED(0xFFB71C1C, 0xFFFADBD8, 0xFFAA0000, 0xFFAA0000);

        private final int stripe;
        private final int pill;
        private final int border;
        private final int text;

        Kind(int stripe, int pill, int border, int text) {
            this.stripe = stripe;
            this.pill = pill;
            this.border = border;
            this.text = text;
        }
    }

    private record Frame(int left, int top, int w, int h) {
    }
}
