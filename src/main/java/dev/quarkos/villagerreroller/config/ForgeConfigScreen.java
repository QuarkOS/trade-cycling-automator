package dev.quarkos.villagerreroller.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.FitButton;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.ListScroll;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.UiLayout;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import dev.quarkos.villagerreroller.picker.UiStatus;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.List;

import static dev.quarkos.villagerreroller.picker.GuiDraw.MUTED;
import static dev.quarkos.villagerreroller.picker.GuiDraw.TEXT;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.BUTTON_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;
import static dev.quarkos.villagerreroller.picker.UiLayout.SCROLLBAR_SPACE;

/** Vanilla settings shared by Fabric, Forge, and NeoForge. */
public final class ForgeConfigScreen extends Screen {
    private static final int ROWS = 7;
    private static final int ROW_STEP = BUTTON_HEIGHT + GAP;

    private final Screen parent;
    private final List<Setting> settings = new ArrayList<>();
    private final ListScroll scroll = new ListScroll();
    private boolean enabled;
    private boolean bareHand;
    private boolean matchSound;
    private boolean serverFriendly;
    private boolean stopped;
    private Button stopButton;
    private EditBox attemptsBox;
    private EditBox floorBox;
    private EditBox volumeBox;
    private EditBox ticksBox;
    private EditBox timeoutBox;
    private EditBox lookBox;
    private Rect frame;
    private Rect inner;
    private Rect options;
    private Rect track;
    private int visibleRows;

    public ForgeConfigScreen(Screen parent) {
        super(message("config.title"));
        this.parent = parent;
        AutomatorConfig config = AutomatorConfig.get();
        this.enabled = config.enabled();
        this.bareHand = config.allowBareHand();
        this.matchSound = config.matchSound();
        this.serverFriendly = config.serverFriendly();
    }

    @Override
    protected void init() {
        settings.clear();
        frame = UiLayout.frame(width, height, 420, 232);
        inner = frame.inset(PADDING);
        int footerY = inner.bottom() - BUTTON_HEIGHT;
        int optionsY = inner.y() + LINE_HEIGHT + PADDING;
        int optionsHeight = Math.max(0, footerY - GAP - optionsY);
        visibleRows = Math.min(ROWS, (optionsHeight + GAP) / ROW_STEP);
        int trackSpace = visibleRows < ROWS ? SCROLLBAR_SPACE + GAP : 0;
        options = new Rect(inner.x(), optionsY, inner.width() - trackSpace, optionsHeight);
        track = new Rect(inner.right() - SCROLLBAR_SPACE, optionsY, SCROLLBAR_SPACE, optionsHeight);

        AutomatorConfig config = AutomatorConfig.get();
        section(0, 0, "reroll");
        toggle(1, 0, "enabled", enabled, pressed -> pressed.setMessage(onOff(enabled = !enabled)));
        toggle(2, 0, "bare_hand", bareHand, pressed -> pressed.setMessage(onOff(bareHand = !bareHand)));
        attemptsBox = field(3, 0, "max_attempts", attemptsBox, Integer.toString(config.maxAttempts()));
        floorBox = field(4, 0, "durability_floor", floorBox, Integer.toString(config.durabilityFloor()));

        section(0, 1, "pace");
        toggle(1, 1, "server_friendly", serverFriendly, pressed -> {
            serverFriendly = !serverFriendly;
            AutomatorConfig draft = new AutomatorConfig();
            draft.applyServerFriendly(serverFriendly);
            ticksBox.setValue(Integer.toString(draft.ticksBetweenCycles()));
            timeoutBox.setValue(Integer.toString(draft.timeoutMultiplier()));
            lookBox.setValue(number(draft.lookDegreesPerTick()));
            pressed.setMessage(onOff(serverFriendly));
        });
        ticksBox = field(2, 1, "ticks_between", ticksBox, Integer.toString(config.ticksBetweenCycles()));
        timeoutBox = field(3, 1, "timeout_multiplier", timeoutBox, Integer.toString(config.timeoutMultiplier()));
        lookBox = field(4, 1, "look_speed", lookBox, number(config.lookDegreesPerTick()));

        section(5, 0, "feedback");
        toggle(6, 0, "match_sound", matchSound, pressed -> pressed.setMessage(onOff(matchSound = !matchSound)));
        volumeBox = field(6, 1, "match_volume", volumeBox, Integer.toString(config.matchVolume()));

        Rect footer = new Rect(inner.x(), footerY, inner.width(), BUTTON_HEIGHT);
        Rect left = footer.column(0, 3);
        Rect middle = footer.column(1, 3);
        Rect right = footer.column(2, 3);
        int stopWidth = Math.min(92, left.width());
        int cancelWidth = Math.min(72, middle.width());
        stopButton = addRenderableWidget(FitButton.create(
                new Rect(left.x(), left.y(), stopWidth, BUTTON_HEIGHT), message("config.stop_now"), pressed -> stopNow()));
        FitButton.hint(stopButton, message("config.stop_now.tooltip"));
        addRenderableWidget(FitButton.create(
                new Rect(middle.right() - cancelWidth, middle.y(), cancelWidth, BUTTON_HEIGHT),
                Component.translatable("gui.cancel"), pressed -> onClose()));
        addRenderableWidget(FitButton.primary(right, message("config.save"), pressed -> saveAndClose()));
        placeSettings();
        stopButton.active = busy();
    }

    private void section(int row, int column, String key) {
        settings.add(new Setting(row, column, message("config.section." + key), null, null));
    }

    private void toggle(int row, int column, String key, boolean value, Button.OnPress press) {
        Rect control = control(row, column);
        Component label = message("config." + key);
        Button button = addRenderableWidget(FitButton.create(control, onOff(value), press));
        FitButton.hint(button, label.copy().append("\n").append(message("config." + key + ".tooltip")));
        settings.add(new Setting(row, column, label, button, message("config." + key + ".tooltip")));
    }

    private EditBox field(int row, int column, String key, EditBox previous, String fallback) {
        Rect control = control(row, column);
        Component label = message("config." + key);
        EditBox box = addRenderableWidget(new EditBox(font, control.x(), control.y(), control.width(), control.height(), label));
        box.setMaxLength(32);
        box.setValue(previous == null ? fallback : previous.getValue());
        settings.add(new Setting(row, column, label, box, message("config." + key + ".tooltip")));
        return box;
    }

    private Rect row(int row, int column) {
        Rect bounds = options.column(column, 2);
        return new Rect(bounds.x(), bounds.y() + (row - scroll.offset()) * ROW_STEP, bounds.width(), BUTTON_HEIGHT);
    }

    private Rect control(int row, int column) {
        Rect bounds = row(row, column);
        int controlWidth = Math.min(58, bounds.width() * 2 / 5);
        return new Rect(bounds.right() - controlWidth, bounds.y(), controlWidth, bounds.height());
    }

    private boolean visible(int row) {
        return row >= scroll.offset() && row < scroll.offset() + visibleRows;
    }

    private void placeSettings() {
        scroll.clamp(ListScroll.maxOffset(ROWS, visibleRows));
        for (Setting setting : settings) {
            AbstractWidget widget = setting.widget();
            if (widget == null) {
                continue;
            }
            boolean visible = visible(setting.row());
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                Rect control = control(setting.row(), setting.column());
                FitButton.place(widget, control.x(), control.y());
            } else if (getFocused() == widget) {
                //? if <1.19.3 {
                /*if (widget instanceof EditBox box) {
                    box.setFocus(false);
                }
                *///?}
                setFocused(null);
            }
        }
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static boolean busy() {
        return TradeHall.isActive() || JobSiteReroller.isRunning();
    }

    private void stopNow() {
        stopped |= busy();
        if (TradeHall.isActive()) {
            TradeHall.requestStop();
        }
        if (JobSiteReroller.isRunning()) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.PANIC);
        }
    }

    private void saveAndClose() {
        AutomatorConfig config = AutomatorConfig.get();
        config.setEnabled(enabled);
        config.setAllowBareHand(bareHand);
        config.setMatchSound(matchSound);
        config.setMatchVolume(parseInt(volumeBox.getValue(), config.matchVolume()));
        config.setServerFriendly(serverFriendly);
        config.setMaxAttempts(parseInt(attemptsBox.getValue(), config.maxAttempts()));
        config.setDurabilityFloor(parseInt(floorBox.getValue(), config.durabilityFloor()));
        config.setTicksBetweenCycles(parseInt(ticksBox.getValue(), config.ticksBetweenCycles()));
        config.setTimeoutMultiplier(parseInt(timeoutBox.getValue(), config.timeoutMultiplier()));
        config.setLookDegreesPerTick(parseFloat(lookBox.getValue(), config.lookDegreesPerTick()));
        if (!enabled) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.DISABLED);
        }
        AutomatorConfig.save();
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            McBridge.setScreen(minecraft, parent);
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(String text, float fallback) {
        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String number(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        List<Component> tip = paint(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.20.2 {
        /*renderBackground(graphics);
        *///?}
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        List<Component> tip = paint(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        List<Component> tip = paint(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
    }
    *///?}

    private List<Component> paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.panel(canvas, frame);
        stopButton.active = busy();
        List<Component> tip = paintTitle(canvas, mouseX, mouseY);
        for (Setting setting : settings) {
            if (!visible(setting.row())) {
                continue;
            }
            Rect row = row(setting.row(), setting.column());
            if (setting.widget() == null) {
                GuiDraw.label(canvas, setting.label(), row, TEXT, mouseX, mouseY);
                canvas.fill(row.x(), row.bottom() - 1, row.right(), row.bottom(), 0xFF454545);
                continue;
            }
            GuiDraw.row(canvas, row, setting.row(), UiStatus.IDLE, row.contains(mouseX, mouseY));
            Rect control = control(setting.row(), setting.column());
            Rect label = new Rect(row.x() + GAP, row.y(), control.x() - row.x() - 2 * GAP, row.height());
            GuiDraw.line(canvas, setting.label().getString(), label, MUTED);
            if (row.contains(mouseX, mouseY)) {
                tip = new ArrayList<>();
                tip.add(setting.label());
                if (setting.widget() instanceof EditBox box && font.width(box.getValue()) > control.width() - 2 * GAP) {
                    tip.add(Component.literal(box.getValue()));
                }
                tip.add(setting.tip());
            }
        }
        GuiDraw.scrollbar(canvas, track, scroll, ROWS, visibleRows);
        return tip;
    }

    private List<Component> paintTitle(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        boolean running = busy();
        UiStatus tone = running ? TradeHall.isActive() ? TradeHall.hudStatus() : UiStatus.RUNNING
                : stopped ? UiStatus.ERROR : UiStatus.IDLE;
        Component status = TradeHall.isActive() ? TradeHall.hudTitle()
                : running ? JobSiteReroller.status()
                : message(stopped ? "config.stopped" : "status.idle");
        int statusWidth = Math.min(font.width(status), inner.width() * 2 / 5);
        Rect titleBox = new Rect(inner.x(), inner.y(), inner.width() - statusWidth - GAP, LINE_HEIGHT);
        Rect statusBox = new Rect(inner.right() - statusWidth, inner.y(), statusWidth, LINE_HEIGHT);
        GuiDraw.line(canvas, title.getString(), titleBox, TEXT);
        GuiDraw.label(canvas, status, statusBox, tone.color(), mouseX, mouseY);
        return titleBox.contains(mouseX, mouseY) ? List.of(title, message("config.note")) : List.of();
    }

    private boolean submit(int key) {
        if (GuiDraw.enter(key) && !(getFocused() instanceof Button)) {
            saveAndClose();
            return true;
        }
        return false;
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        if ((options.contains(mouseX, mouseY) || track.contains(mouseX, mouseY)) && scroll.wheel(vertical, ROWS, visibleRows)) {
            placeSettings();
            return true;
        }
        return false;
    }

    private boolean clickTrack(double x, double y, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && track.contains(x, y)
                && scroll.clickTrack(y, track.y(), track.height(), ROWS, visibleRows)) {
            placeSettings();
            return true;
        }
        return false;
    }

    private boolean drag(double y) {
        if (scroll.drag(y, track.y(), track.height(), ROWS, visibleRows)) {
            placeSettings();
            return true;
        }
        return false;
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
        return drag(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scroll.stop();
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
        return drag(event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scroll.stop();
        return super.mouseReleased(event);
    }
    //?}

    //? if <1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollBy(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return scrollBy(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    //?}

    private record Setting(int row, int column, Component label, AbstractWidget widget, Component tip) {
    }
}
