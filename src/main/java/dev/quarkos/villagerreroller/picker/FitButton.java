package dev.quarkos.villagerreroller.picker;

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

/**
 * A vanilla button whose label stays the full component (so screen tests can
 * find it) while the drawn text is cut with an ellipsis when it does not fit.
 */
public final class FitButton extends Button {
    private FitButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        //? if <1.19.3 {
        /*super(x, y, width, height, message, onPress);
        *///?} else {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        //?}
    }

    public static Button create(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new FitButton(x, y, width, height, message, onPress);
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

    private int widgetWidth() {
        //? if <1.19.3 {
        /*return this.width;
        *///?} else {
        return this.getWidth();
        //?}
    }

    private void prepareTip(Component full, boolean cut) {
        //? if >=1.19.3 {
        if (cut) {
            setTooltip(Tooltip.create(full));
        } else {
            setTooltip(null);
        }
        //?}
    }

    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, full.getString(), widgetWidth() - 6);
        boolean cut = !shown.equals(full.getString());
        if (cut) {
            setMessage(Component.literal(shown));
        }
        super.renderButton(pose, mouseX, mouseY, partialTick);
        if (cut) {
            setMessage(full);
        }
        prepareTip(full, cut);
    }
    *///?}

    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, full.getString(), widgetWidth() - 6);
        boolean cut = !shown.equals(full.getString());
        if (cut) {
            setMessage(Component.literal(shown));
        }
        super.renderWidget(pose, mouseX, mouseY, partialTick);
        if (cut) {
            setMessage(full);
        }
        prepareTip(full, cut);
    }
    *///?}

    //? if >=1.20 && <1.21.11 {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, full.getString(), widgetWidth() - 6);
        boolean cut = !shown.equals(full.getString());
        if (cut) {
            setMessage(Component.literal(shown));
        }
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (cut) {
            setMessage(full);
        }
        prepareTip(full, cut);
    }
    *///?}

    //? if >=1.21.11 && <26.1 {
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, full.getString(), widgetWidth() - 6);
        boolean cut = !shown.equals(full.getString());
        if (cut) {
            setMessage(Component.literal(shown));
        }
        // Button is abstract here; its concrete Plain subclass is what calls these.
        this.renderDefaultSprite(graphics);
        this.renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        if (cut) {
            setMessage(full);
        }
        prepareTip(full, cut);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component full = getMessage();
        String shown = GuiDraw.ellipsis(Minecraft.getInstance().font, full.getString(), widgetWidth() - 6);
        boolean cut = !shown.equals(full.getString());
        if (cut) {
            setMessage(Component.literal(shown));
        }
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        if (cut) {
            setMessage(full);
        }
        prepareTip(full, cut);
    }
    *///?}
}
