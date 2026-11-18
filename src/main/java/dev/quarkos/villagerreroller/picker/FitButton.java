package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
//? if >=1.19.3 {
import net.minecraft.client.gui.components.Tooltip;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}

import java.util.List;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;

/** Vanilla button with a fitted label and the original message for narration and screen lookups. */
public final class FitButton extends Button {
    private final boolean primary;
    private Component hint;
    private Component displayedTip;
    private boolean selected;

    private FitButton(Rect bounds, Component message, OnPress onPress, boolean primary) {
        //? if <1.19.3 {
        /*super(bounds.x(), bounds.y(), bounds.width(), bounds.height(), message, onPress);
        *///?} else {
        super(bounds.x(), bounds.y(), bounds.width(), bounds.height(), message, onPress, DEFAULT_NARRATION);
        //?}
        this.primary = primary;
    }

    public static Button create(Rect bounds, Component message, OnPress onPress) {
        return new FitButton(bounds, message, onPress, false);
    }

    public static Button primary(Rect bounds, Component message, OnPress onPress) {
        return new FitButton(bounds, message, onPress, true);
    }

    public static void hint(Button button, Component hint) {
        if (button instanceof FitButton fit) {
            fit.hint = hint;
        }
    }

    public static void select(Button button, boolean selected) {
        if (button instanceof FitButton fit) {
            fit.selected = selected;
        }
    }

    public static void place(AbstractWidget widget, int x, int y) {
        //? if <1.19.3 {
        /*widget.x = x;
        widget.y = y;
        *///?} else {
        widget.setX(x);
        widget.setY(y);
        //?}
    }

    private Component fitLabel(Component full) {
        String text = full.getString();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, text, getWidth() - 2 * GAP);
        return shown.equals(text) ? full : Component.literal(shown);
    }

    private void decorate(GuiDraw.Canvas canvas, Component label, boolean cut, int mouseX, int mouseY) {
        Rect bounds = GuiDraw.bounds(this);
        if (active && (primary || selected)) {
            canvas.fill(bounds.x() + 2, bounds.bottom() - 3, bounds.right() - 2, bounds.bottom() - 1,
                    primary ? 0xFF87AE74 : GuiDraw.MUTED);
        }
        Component tip = cut && hint != null ? label.copy().append("\n").append(hint) : cut ? label : hint;
        //? if <1.19.3 {
        /*if (tip != null && bounds.contains(mouseX, mouseY)) {
            canvas.tip(List.of(tip), mouseX, mouseY);
        }
        *///?} else {
        if (!java.util.Objects.equals(displayedTip, tip)) {
            displayedTip = tip;
            setTooltip(tip == null ? null : Tooltip.create(tip));
        }
        //?}
    }

    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        Component fitted = fitLabel(full);
        setMessage(fitted);
        super.renderButton(pose, mouseX, mouseY, partialTick);
        setMessage(full);
        decorate(GuiDraw.of(pose), full, fitted != full, mouseX, mouseY);
    }
    *///?}

    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        Component fitted = fitLabel(full);
        setMessage(fitted);
        super.renderWidget(pose, mouseX, mouseY, partialTick);
        setMessage(full);
        decorate(GuiDraw.of(pose), full, fitted != full, mouseX, mouseY);
    }
    *///?}

    //? if >=1.20 && <1.21.11 {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        Component fitted = fitLabel(full);
        setMessage(fitted);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        setMessage(full);
        decorate(GuiDraw.of(graphics), full, fitted != full, mouseX, mouseY);
    }
    *///?}

    //? if >=1.21.11 && <26.1 {
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        Component fitted = fitLabel(full);
        setMessage(fitted);
        renderDefaultSprite(graphics);
        renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        setMessage(full);
        decorate(GuiDraw.of(graphics), full, fitted != full, mouseX, mouseY);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        Component fitted = fitLabel(full);
        setMessage(fitted);
        extractDefaultSprite(graphics);
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        setMessage(full);
        decorate(GuiDraw.of(graphics), full, fitted != full, mouseX, mouseY);
    }
    *///?}
}
