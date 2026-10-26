package dev.quarkos.villagerreroller.config;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
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

/**
 * Vanilla settings screen shared by Fabric and Forge. Mod Menu opens it when
 * that mod is installed. Forge opens it from the mods list. The same widgets
 * write {@code villager_reroller.json}.
 */
public final class ForgeConfigScreen extends Screen {
    private final Screen parent;
    private boolean enabled;
    private boolean bareHand;
    private boolean serverFriendly;
    private boolean panic;
    private Button enabledButton;
    private Button bareHandButton;
    private Button serverButton;
    private Button panicButton;
    private EditBox attemptsBox;
    private EditBox floorBox;
    private EditBox ticksBox;
    private EditBox timeoutBox;
    private EditBox lookBox;

    public ForgeConfigScreen(Screen parent) {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.title"));
        this.parent = parent;
        AutomatorConfig config = AutomatorConfig.get();
        this.enabled = config.enabled();
        this.bareHand = config.allowBareHand();
        this.serverFriendly = config.serverFriendly();
    }

    @Override
    protected void init() {
        int column = Math.min(170, Math.max(140, (this.width - 24) / 2));
        int left = this.width / 2 - 4 - column;
        int right = this.width / 2 + 4;
        int y = 36;
        enabledButton = button(left, y, column, enabledLabel(), pressed -> {
            enabled = !enabled;
            pressed.setMessage(enabledLabel());
        });
        bareHandButton = button(left, y + 24, column, bareHandLabel(), pressed -> {
            bareHand = !bareHand;
            pressed.setMessage(bareHandLabel());
        });
        attemptsBox = field(left, y + 62, column, attemptsLabel(), Integer.toString(AutomatorConfig.get().maxAttempts()), 5);
        floorBox = field(left, y + 98, column, floorLabel(), Integer.toString(AutomatorConfig.get().durabilityFloor()), 5);

        serverButton = button(right, y, column, serverLabel(), pressed -> {
            serverFriendly = !serverFriendly;
            AutomatorConfig draft = new AutomatorConfig();
            draft.applyServerFriendly(serverFriendly);
            ticksBox.setValue(Integer.toString(draft.ticksBetweenCycles()));
            timeoutBox.setValue(Integer.toString(draft.timeoutMultiplier()));
            lookBox.setValue(trim(draft.lookDegreesPerTick()));
            pressed.setMessage(serverLabel());
        });
        ticksBox = field(right, y + 62, column, ticksLabel(), Integer.toString(AutomatorConfig.get().ticksBetweenCycles()), 4);
        timeoutBox = field(right, y + 98, column, timeoutLabel(), Integer.toString(AutomatorConfig.get().timeoutMultiplier()), 3);
        lookBox = field(right, y + 134, column, lookLabel(), trim(AutomatorConfig.get().lookDegreesPerTick()), 5);
        panicButton = button(right, y + 160, column, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.panic"), pressed -> {
            panic = !panic;
            pressed.setMessage(Component.translatable(panic
                    ? "text." + VillagerReroller.MOD_ID + ".config.panic.on"
                    : "text." + VillagerReroller.MOD_ID + ".config.panic"));
        });
        button(this.width / 2 - 100, this.height - 28, 200, Component.translatable("gui.done"), pressed -> saveAndClose());
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
        config.setServerFriendly(serverFriendly);
        config.setMaxAttempts(parseInt(attemptsBox.getValue(), config.maxAttempts()));
        config.setDurabilityFloor(parseInt(floorBox.getValue(), config.durabilityFloor()));
        config.setTicksBetweenCycles(parseInt(ticksBox.getValue(), config.ticksBetweenCycles()));
        config.setTimeoutMultiplier(parseInt(timeoutBox.getValue(), config.timeoutMultiplier()));
        config.setLookDegreesPerTick(parseFloat(lookBox.getValue(), config.lookDegreesPerTick()));
        if (!enabled) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.DISABLED);
        }
        if (panic) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.PANIC);
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
        if (over(panicButton, mouseX, mouseY)) {
            return tip("panic");
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
        drawFieldLabel(pose, ticksBox, ticksLabel());
        drawFieldLabel(pose, timeoutBox, timeoutLabel());
        drawFieldLabel(pose, lookBox, lookLabel());
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
        drawFieldLabel(graphics, ticksBox, ticksLabel());
        drawFieldLabel(graphics, timeoutBox, timeoutLabel());
        drawFieldLabel(graphics, lookBox, lookLabel());
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
        drawFieldLabel(graphics, ticksBox, ticksLabel());
        drawFieldLabel(graphics, timeoutBox, timeoutLabel());
        drawFieldLabel(graphics, lookBox, lookLabel());
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
