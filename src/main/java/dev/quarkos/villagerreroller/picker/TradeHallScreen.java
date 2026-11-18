package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.GuiDraw.MUTED;
import static dev.quarkos.villagerreroller.picker.GuiDraw.TEXT;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.BUTTON_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;
import static dev.quarkos.villagerreroller.picker.UiLayout.ROW_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.SCROLLBAR_SPACE;

/** Queue, progress, and results for the trading hall. */
public final class TradeHallScreen extends Screen {
    private static final int REMOVE_WIDTH = 48;
    private final Screen parent;
    private final List<Button> removeButtons = new ArrayList<>();
    private final ListScroll bar = new ListScroll();
    private int[] removeSlots;
    private int slotCount;
    private Rect frame;
    private Rect header;
    private Rect info;
    private Rect caption;
    private Rect well;
    private Rect track;
    private int visibleRows;
    private List<Component> hover = List.of();
    private Button villagersButton;
    private Button primaryButton;
    private Button clearButton;
    private Button backButton;

    public TradeHallScreen() {
        this(null);
    }

    public TradeHallScreen(Screen parent) {
        super(message(TradeHall.showingSummary() ? "hall.summary" : "hall.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        TradeHall.dismiss();
        if (minecraft != null) {
            McBridge.setScreen(minecraft, parent);
        }
    }

    @Override
    protected void init() {
        frame = UiLayout.frame(width, height, 424, 244);
        Rect inner = frame.inset(PADDING);
        int villagersWidth = Math.min(108, inner.width() / 3);
        header = new Rect(inner.x(), inner.y(), inner.width() - villagersWidth - GAP, BUTTON_HEIGHT);
        Rect villagers = new Rect(inner.right() - villagersWidth, inner.y(), villagersWidth, BUTTON_HEIGHT);
        villagersButton = addRenderableWidget(FitButton.create(villagers,
                message("button.my_villagers", villagerCount()), pressed -> openVillagers()));
        FitButton.hint(villagersButton, message("button.list.tooltip"));

        info = new Rect(inner.x(), header.bottom() + GAP, inner.width(), LINE_HEIGHT);
        caption = new Rect(info.x(), info.bottom() + GAP, info.width(), LINE_HEIGHT);
        Rect footer = new Rect(inner.x(), inner.bottom() - BUTTON_HEIGHT, inner.width(), BUTTON_HEIGHT);
        int listTop = caption.bottom() + GAP;
        well = new Rect(inner.x(), listTop, inner.width(), Math.max(0, footer.y() - GAP - listTop));
        track = new Rect(well.right() - SCROLLBAR_SPACE, well.y(), SCROLLBAR_SPACE, well.height());
        visibleRows = well.height() / ROW_HEIGHT;

        addRemoveButtons();
        Rect left = footer.column(0, 3);
        Rect middle = footer.column(1, 3);
        int backWidth = Math.min(76, middle.width());
        clearButton = addRenderableWidget(FitButton.create(
                new Rect(left.x(), left.y(), Math.min(80, left.width()), BUTTON_HEIGHT),
                message("button.clear"), pressed -> {
                    TradeHall.clear();
                    Arrays.fill(removeSlots, -1);
                    bar.setOffset(0);
                }));
        backButton = addRenderableWidget(FitButton.create(
                new Rect(middle.right() - backWidth, middle.y(), backWidth, BUTTON_HEIGHT),
                message("button.back"), pressed -> onClose()));
        primaryButton = addRenderableWidget(FitButton.primary(footer.column(2, 3), actionLabel(), pressed -> onPrimary()));
        syncButtons(startBlocker());
    }

    private void openVillagers() {
        if (minecraft != null) {
            McBridge.setScreen(minecraft, parent instanceof MyVillagersScreen ? parent : new MyVillagersScreen(this));
        }
    }

    private void addRemoveButtons() {
        removeButtons.clear();
        removeSlots = new int[visibleRows];
        Arrays.fill(removeSlots, -1);
        for (int i = 0; i < visibleRows; i++) {
            int row = i;
            Button button = addRenderableWidget(FitButton.create(new Rect(0, 0, REMOVE_WIDTH, BUTTON_HEIGHT),
                    message("button.remove"), pressed -> {
                        if (removeSlots[row] >= 0) {
                            TradeHall.remove(removeSlots[row]);
                            Arrays.fill(removeSlots, -1);
                        }
                    }));
            button.visible = false;
            button.active = false;
            removeButtons.add(button);
        }
    }

    private void syncButtons(Component blocker) {
        villagersButton.setMessage(message("button.my_villagers", villagerCount()));
        primaryButton.setMessage(actionLabel());
        primaryButton.active = blocker == null;
        FitButton.hint(primaryButton, blocker);
        boolean summary = TradeHall.showingSummary();
        backButton.visible = !summary;
        backButton.active = !summary;
        clearButton.visible = !summary && !TradeHall.isActive();
        clearButton.active = clearButton.visible && TradeHall.size() > 0;
    }

    private Component actionLabel() {
        if (TradeHall.showingSummary()) {
            return Component.translatable("gui.done");
        }
        return message(TradeHall.isActive() ? "button.stop" : "button.hall_start");
    }

    private Component startBlocker() {
        if (TradeHall.showingSummary() || TradeHall.isActive()) {
            return null;
        }
        if (TradeHall.size() == 0) {
            return message("hall.empty");
        }
        if (!AutomatorConfig.get().enabled()) {
            return message("disabled");
        }
        return JobSiteReroller.isRunning() ? message("ui.busy") : null;
    }

    private void onPrimary() {
        if (TradeHall.showingSummary()) {
            onClose();
        } else if (TradeHall.isActive()) {
            TradeHall.requestStop();
        } else if (minecraft != null && startBlocker() == null) {
            TradeHall.start(minecraft);
        }
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        paint(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    *///?}

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        hover = List.of();
        boolean summary = TradeHall.showingSummary();
        boolean running = TradeHall.isActive();
        Component blocker = startBlocker();
        syncButtons(blocker);
        GuiDraw.panel(canvas, frame);
        GuiDraw.label(canvas, message(summary ? "hall.summary" : "hall.title"), header, TEXT, mouseX, mouseY);
        GuiDraw.well(canvas, well);

        List<TradeHall.Slot> slots = TradeHall.slots();
        List<SlotStatus> statuses = new ArrayList<>();
        int ready = 0;
        for (TradeHall.Slot slot : slots) {
            SlotStatus status = status(slot, summary, running);
            statuses.add(status);
            if (status.tone() == UiStatus.READY) {
                ready++;
            }
        }
        paintStatus(canvas, summary, running, ready, blocker, mouseX, mouseY);
        slotCount = slots.size();
        bar.clamp(ListScroll.maxOffset(slotCount, visibleRows));
        for (Button button : removeButtons) {
            button.visible = false;
            button.active = false;
        }
        if (slots.isEmpty()) {
            GuiDraw.empty(canvas, well, message("hall.empty_title"), message("hall.empty"), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < visibleRows && bar.offset() + i < slotCount; i++) {
            int index = bar.offset() + i;
            paintSlot(canvas, slots.get(index), statuses.get(index), index, i, !summary && !running, mouseX, mouseY);
        }
        GuiDraw.scrollbar(canvas, track, bar, slotCount, visibleRows);
    }

    private void paintStatus(GuiDraw.Canvas canvas, boolean summary, boolean running, int ready, Component blocker,
                             int mouseX, int mouseY) {
        if (summary) {
            GuiDraw.label(canvas, message("hall.results"), info, TEXT, mouseX, mouseY);
            GuiDraw.label(canvas, TradeHall.countsLine(), caption, MUTED, mouseX, mouseY);
        } else if (running) {
            GuiDraw.label(canvas, TradeHall.hudTitle(), info, TradeHall.hudStatus().color(), mouseX, mouseY);
            GuiDraw.label(canvas, TradeHall.hudDetail(), caption, MUTED, mouseX, mouseY);
        } else {
            Component availability = message("hall.ready_count", ready, TradeHall.size());
            int countWidth = Math.min(canvas.font().width(availability), info.width() / 2);
            Rect queue = new Rect(info.x(), info.y(), info.width() - countWidth - GAP, info.height());
            Rect count = new Rect(info.right() - countWidth, info.y(), countWidth, info.height());
            GuiDraw.label(canvas, message("hall.queue"), queue, TEXT, mouseX, mouseY);
            GuiDraw.label(canvas, availability, count, ready > 0 ? UiStatus.READY.color() : MUTED, mouseX, mouseY);
            Component hint = blocker == null || TradeHall.size() == 0 ? message("hall.hint") : blocker;
            GuiDraw.label(canvas, hint, caption, blocker != null && TradeHall.size() > 0 ? UiStatus.ERROR.color() : MUTED, mouseX, mouseY);
        }
    }

    private void paintSlot(GuiDraw.Canvas canvas, TradeHall.Slot slot, SlotStatus status, int index, int visible,
                           boolean removable, int mouseX, int mouseY) {
        Rect row = new Rect(well.x(), well.y() + visible * ROW_HEIGHT, well.width() - SCROLLBAR_SPACE, ROW_HEIGHT);
        boolean hovered = row.contains(mouseX, mouseY);
        GuiDraw.row(canvas, row, index, status.tone(), hovered);

        int textX = row.x() + PADDING;
        int right = row.right() - GAP;
        if (removable) {
            Button button = removeButtons.get(visible);
            removeSlots[visible] = index;
            right -= REMOVE_WIDTH;
            FitButton.place(button, right, row.y() + GAP);
            button.visible = true;
            button.active = true;
            right -= GAP;
        }
        Component identity = message("hall.position", index + 1, slot.label());
        if (!slot.place().isEmpty()) {
            identity = message("ui.with_detail", identity, slot.place());
        }
        Component description = Component.literal(slot.description());
        int textWidth = right - textX;
        GuiDraw.line(canvas, identity.getString(), new Rect(textX, row.y() + 3, textWidth, LINE_HEIGHT), TEXT);
        int statusWidth = Math.min(canvas.font().width(status.text()), textWidth / 2);
        GuiDraw.line(canvas, status.text().getString(), new Rect(textX, row.y() + 15, statusWidth, LINE_HEIGHT), status.tone().color());
        int picksX = textX + statusWidth + GAP;
        GuiDraw.line(canvas, description.getString(), new Rect(picksX, row.y() + 15, right - picksX, LINE_HEIGHT), MUTED);
        if (hovered && mouseX < right) {
            hover = List.of(identity, status.text(), description);
        }
    }

    private SlotStatus status(TradeHall.Slot slot, boolean summary, boolean running) {
        if (summary || (running && slot.outcome() != TradeHall.Outcome.PENDING)) {
            return new SlotStatus(TradeHall.outcomeLine(slot), UiStatus.forOutcome(slot.outcome()));
        }
        if (running) {
            return TradeHall.isCurrent(slot) ? new SlotStatus(TradeHall.hudDetail(), TradeHall.hudStatus())
                    : new SlotStatus(message("hall.pending"), UiStatus.RUNNING);
        }
        String blocker = minecraft == null ? null : TradeHall.blocker(minecraft, slot.villagerId());
        return blocker == null ? new SlotStatus(message("hall.ready"), UiStatus.READY)
                : new SlotStatus(message("hall.skipped", message("hall.why." + blocker)), UiStatus.RUNNING);
    }

    private int villagerCount() {
        return minecraft == null ? 0 : VillagerRosterClient.count(minecraft);
    }

    private boolean submit(int key) {
        if (GuiDraw.enter(key) && getFocused() == null && startBlocker() == null) {
            onPrimary();
            return true;
        }
        return false;
    }

    private boolean clickTrack(double mouseX, double mouseY, int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT && track.contains(mouseX, mouseY)
                && bar.clickTrack(mouseY, track.y(), track.height(), slotCount, visibleRows);
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        return well.contains(mouseX, mouseY) && bar.wheel(vertical, slotCount, visibleRows);
    }

    //? if <1.21.9 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return submit(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return clickTrack(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (bar.drag(mouseY, track.y(), track.height(), slotCount, visibleRows)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        bar.stop();
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return submit(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return clickTrack(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (bar.drag(event.y(), track.y(), track.height(), slotCount, visibleRows)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        bar.stop();
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

    private record SlotStatus(Component text, UiStatus tone) {
    }
}
