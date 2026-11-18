package dev.quarkos.villagerreroller.config;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.TradeHall;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
//? if >=1.19.3 {
import net.minecraft.client.gui.components.Tooltip;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}

/**
 * Vanilla settings screen shared by Fabric, Forge, and NeoForge. Mod Menu opens
 * it when that mod is installed. Forge and NeoForge open it from the mods list.
 * The same widgets write {@code villager_reroller.json}.
 */
public final class ForgeConfigScreen extends Screen {
    private final Screen parent;
    private boolean enabled;
    private boolean bareHand;
    private boolean matchSound;
    private boolean serverFriendly;
    private Button enabledButton;
    private Button bareHandButton;
    private Button matchButton;
    private Button serverButton;
    private Button stopButton;
    private int noteY = -1;
    private int rerollHeaderY = -1;
    private int paceHeaderY = -1;
    private EditBox attemptsBox;
    private EditBox floorBox;
    private EditBox volumeBox;
    private EditBox ticksBox;
    private EditBox timeoutBox;
    private EditBox lookBox;

    public ForgeConfigScreen(Screen parent) {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.title"));
        this.parent = parent;
        AutomatorConfig config = AutomatorConfig.get();
        this.enabled = config.enabled();
        this.bareHand = config.allowBareHand();
        this.matchSound = config.matchSound();
        this.serverFriendly = config.serverFriendly();
    }

    @Override
    protected void init() {
        int column = Math.min(170, Math.max(120, (this.width - 40) / 2));
        int left = this.width / 2 - 6 - column;
        int right = this.width / 2 + 6;
        boolean roomy = this.height >= 280;
        int y = 24;
        if (roomy) {
            noteY = 22;
            y = 40;
            rerollHeaderY = y;
            paceHeaderY = y;
            y += 14;
        } else {
            noteY = -1;
            rerollHeaderY = -1;
            paceHeaderY = -1;
        }
        int stepButton = roomy ? 24 : 22;
        int stepField = roomy ? 36 : 34;
        int cursor = y;
        enabledButton = button(left, cursor, column, enabledLabel(), pressed -> {
            enabled = !enabled;
            pressed.setMessage(enabledLabel());
        });
        cursor = cursor + stepButton;
        bareHandButton = button(left, cursor, column, bareHandLabel(), pressed -> {
            bareHand = !bareHand;
            pressed.setMessage(bareHandLabel());
        });
        cursor = cursor + stepField;
        attemptsBox = field(left, cursor, column, attemptsLabel(), Integer.toString(AutomatorConfig.get().maxAttempts()), 5);
        cursor = cursor + stepField;
        floorBox = field(left, cursor, column, floorLabel(), Integer.toString(AutomatorConfig.get().durabilityFloor()), 5);
        cursor = cursor + stepField;
        matchButton = button(left, cursor, column, matchLabel(), pressed -> {
            matchSound = !matchSound;
            pressed.setMessage(matchLabel());
        });
        cursor = cursor + stepField;
        volumeBox = field(left, cursor, column, volumeLabel(), Integer.toString(AutomatorConfig.get().matchVolume()), 3);

        cursor = y;
        serverButton = button(right, cursor, column, serverLabel(), pressed -> {
            serverFriendly = !serverFriendly;
            AutomatorConfig draft = new AutomatorConfig();
            draft.applyServerFriendly(serverFriendly);
            ticksBox.setValue(Integer.toString(draft.ticksBetweenCycles()));
            timeoutBox.setValue(Integer.toString(draft.timeoutMultiplier()));
            lookBox.setValue(trim(draft.lookDegreesPerTick()));
            pressed.setMessage(serverLabel());
        });
        cursor = cursor + stepField;
        ticksBox = field(right, cursor, column, ticksLabel(), Integer.toString(AutomatorConfig.get().ticksBetweenCycles()), 4);
        cursor = cursor + stepField;
        timeoutBox = field(right, cursor, column, timeoutLabel(), Integer.toString(AutomatorConfig.get().timeoutMultiplier()), 3);
        cursor = cursor + stepField;
        lookBox = field(right, cursor, column, lookLabel(), trim(AutomatorConfig.get().lookDegreesPerTick()), 5);

        int stopW = 98;
        int doneW = 98;
        int gap = 8;
        int rowX = this.width / 2 - (stopW + gap + doneW) / 2;
        stopButton = button(rowX, this.height - 28, stopW,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".config.stop_now"), pressed -> {
                    boolean busy = TradeHall.isActive() || JobSiteReroller.isRunning();
                    if (TradeHall.isActive()) {
                        TradeHall.requestStop();
                    }
                    if (JobSiteReroller.isRunning()) {
                        JobSiteReroller.stop(JobSiteReroller.StopReason.PANIC);
                    }
                    if (busy) {
                        pressed.setMessage(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.stopped"));
                    }
                });
        //? if >=1.19.3 {
        stopButton.setTooltip(Tooltip.create(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.stop_now.tooltip")));
        //?}
        button(rowX + stopW + gap, this.height - 28, doneW, Component.translatable("gui.done"), pressed -> saveAndClose());
    }

    private Button button(int x, int y, int width, Component label, Button.OnPress press) {
        //? if <1.19.3 {
        /*return addRenderableWidget(new Button(x, y, width, 20, label, press));
        *///?} else {
        return addRenderableWidget(Button.builder(label, press).bounds(x, y, width, 20).build());
        //?}
    }

    private EditBox field(int x, int y, int width, Component label, String value, int maxLength) {
        EditBox box = new EditBox(this.font, x, y, width, 20, label);
        box.setMaxLength(maxLength);
        box.setValue(value);
        //? if >=1.19.3 {
        box.setHint(label);
        //?}
        addRenderableWidget(box);
        return box;
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
        if (this.minecraft != null) {
            McBridge.setScreen(this.minecraft, parent);
        }
    }

    private Component enabledLabel() {
        return onOff("text." + VillagerReroller.MOD_ID + ".config.enabled", enabled);
    }

    private Component bareHandLabel() {
        return onOff("text." + VillagerReroller.MOD_ID + ".config.bare_hand", bareHand);
    }

    private Component matchLabel() {
        return onOff("text." + VillagerReroller.MOD_ID + ".config.match_sound", matchSound);
    }

    private Component serverLabel() {
        return onOff("text." + VillagerReroller.MOD_ID + ".config.server_friendly", serverFriendly);
    }

    private static Component onOff(String key, boolean value) {
        return Component.translatable(key)
                .append(": ")
                .append(Component.translatable(value ? "options.on" : "options.off"));
    }

    private static Component attemptsLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_attempts");
    }

    private static Component floorLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.durability_floor");
    }

    private static Component volumeLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.match_volume");
    }

    private static Component ticksLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.ticks_between");
    }

    private static Component timeoutLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.timeout_multiplier");
    }

    private static Component lookLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.look_speed");
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
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return Float.toString(value);
    }

    private Component hoveredTip(int mouseX, int mouseY) {
        if (over(enabledButton, mouseX, mouseY)) {
            return tip("enabled");
        }
        if (over(bareHandButton, mouseX, mouseY)) {
            return tip("bare_hand");
        }
        if (overField(attemptsBox, mouseX, mouseY)) {
            return tip("max_attempts");
        }
        if (overField(floorBox, mouseX, mouseY)) {
            return tip("durability_floor");
        }
        if (over(matchButton, mouseX, mouseY)) {
            return tip("match_sound");
        }
        if (overField(volumeBox, mouseX, mouseY)) {
            return tip("match_volume");
        }
        if (over(serverButton, mouseX, mouseY)) {
            return tip("server_friendly");
        }
        if (overField(ticksBox, mouseX, mouseY)) {
            return tip("ticks_between");
        }
        if (overField(timeoutBox, mouseX, mouseY)) {
            return tip("timeout_multiplier");
        }
        if (overField(lookBox, mouseX, mouseY)) {
            return tip("look_speed");
        }
        if (over(stopButton, mouseX, mouseY)) {
            return tip("stop_now");
        }
        return null;
    }

    private static Component tip(String key) {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config." + key + ".tooltip");
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.render(pose, mouseX, mouseY, partialTick);
        drawFieldLabel(pose, attemptsBox, attemptsLabel());
        drawFieldLabel(pose, floorBox, floorLabel());
        drawFieldLabel(pose, volumeBox, volumeLabel());
        drawFieldLabel(pose, ticksBox, ticksLabel());
        drawFieldLabel(pose, timeoutBox, timeoutLabel());
        drawFieldLabel(pose, lookBox, lookLabel());
        this.font.drawShadow(pose, this.title, (this.width - this.font.width(this.title)) / 2, 6, 0xFFFFFF);
        if (noteY >= 0) {
            String note = Component.translatable("text." + VillagerReroller.MOD_ID + ".config.note").getString();
            String shown = GuiDraw.ellipsis(this.font, note, this.width - 16);
            this.font.drawShadow(pose, shown, (this.width - this.font.width(shown)) / 2, noteY, 0xA0A0A0);
        }
        if (rerollHeaderY >= 0 && enabledButton != null) {
            this.font.drawShadow(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.reroll"), widgetX(enabledButton), rerollHeaderY, 0xFFFFFF);
        }
        if (paceHeaderY >= 0 && serverButton != null) {
            this.font.drawShadow(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.pace"), widgetX(serverButton), paceHeaderY, 0xFFFFFF);
        }
        Component hovered = hoveredTip(mouseX, mouseY);
        if (hovered != null) {
            renderTooltip(pose, hovered, mouseX, mouseY);
        }
    }

    private void drawFieldLabel(PoseStack pose, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        this.font.drawShadow(pose, label, widgetX(box), widgetY(box) - 12, 0xFFFFFF);
    }
    *///?}
    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFieldLabel(graphics, attemptsBox, attemptsLabel());
        drawFieldLabel(graphics, floorBox, floorLabel());
        drawFieldLabel(graphics, volumeBox, volumeLabel());
        drawFieldLabel(graphics, ticksBox, ticksLabel());
        drawFieldLabel(graphics, timeoutBox, timeoutLabel());
        drawFieldLabel(graphics, lookBox, lookLabel());
        graphics.drawString(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 6, 0xFFFFFFFF, true);
        if (noteY >= 0) {
            String note = Component.translatable("text." + VillagerReroller.MOD_ID + ".config.note").getString();
            String shown = GuiDraw.ellipsis(this.font, note, this.width - 16);
            graphics.drawString(this.font, shown, (this.width - this.font.width(shown)) / 2, noteY, 0xFFA0A0A0, true);
        }
        if (rerollHeaderY >= 0 && enabledButton != null) {
            graphics.drawString(this.font, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.reroll"), widgetX(enabledButton), rerollHeaderY, 0xFFFFFFFF, true);
        }
        if (paceHeaderY >= 0 && serverButton != null) {
            graphics.drawString(this.font, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.pace"), widgetX(serverButton), paceHeaderY, 0xFFFFFFFF, true);
        }
        Component hovered = hoveredTip(mouseX, mouseY);
        if (hovered != null) {
            //? if <1.21.6 {
            /*graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
            *///?}
            //? if >=1.21.6 {
            graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
            //?}
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        graphics.drawString(this.font, label, widgetX(box), widgetY(box) - 12, 0xFFFFFFFF, true);
    }
    //?}
    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawFieldLabel(graphics, attemptsBox, attemptsLabel());
        drawFieldLabel(graphics, floorBox, floorLabel());
        drawFieldLabel(graphics, volumeBox, volumeLabel());
        drawFieldLabel(graphics, ticksBox, ticksLabel());
        drawFieldLabel(graphics, timeoutBox, timeoutLabel());
        drawFieldLabel(graphics, lookBox, lookLabel());
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 6, 0xFFFFFFFF, true);
        if (noteY >= 0) {
            String note = Component.translatable("text." + VillagerReroller.MOD_ID + ".config.note").getString();
            String shown = GuiDraw.ellipsis(this.font, note, this.width - 16);
            graphics.text(this.font, shown, (this.width - this.font.width(shown)) / 2, noteY, 0xFFA0A0A0, true);
        }
        if (rerollHeaderY >= 0 && enabledButton != null) {
            graphics.text(this.font, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.reroll"), widgetX(enabledButton), rerollHeaderY, 0xFFFFFFFF, true);
        }
        if (paceHeaderY >= 0 && serverButton != null) {
            graphics.text(this.font, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.section.pace"), widgetX(serverButton), paceHeaderY, 0xFFFFFFFF, true);
        }
        Component hovered = hoveredTip(mouseX, mouseY);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        graphics.text(this.font, label, widgetX(box), widgetY(box) - 12, 0xFFFFFFFF, true);
    }
    *///?}

    private boolean over(AbstractWidget widget, int mouseX, int mouseY) {
        if (widget == null) {
            return false;
        }
        int left = widgetX(widget);
        int top = widgetY(widget);
        return mouseX >= left && mouseX < left + widget.getWidth() && mouseY >= top && mouseY < top + widget.getHeight();
    }

    private boolean overField(EditBox box, int mouseX, int mouseY) {
        if (box == null) {
            return false;
        }
        int left = widgetX(box);
        int top = widgetY(box) - 12;
        return mouseX >= left && mouseX < left + box.getWidth() && mouseY >= top && mouseY < widgetY(box) + box.getHeight();
    }

    private int widgetX(AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.x;
        *///?} else {
        return widget.getX();
        //?}
    }

    private int widgetY(AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.y;
        *///?} else {
        return widget.getY();
        //?}
    }
}
