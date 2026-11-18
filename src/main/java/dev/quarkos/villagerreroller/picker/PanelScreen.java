package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

import java.util.List;

/** Draws the mod's panels over a clear view of the world. */
public abstract class PanelScreen extends Screen {
    protected PanelScreen(Component title) {
        super(title);
    }

    protected abstract List<Component> paint(GuiDraw.Canvas canvas, int mouseX, int mouseY);

    private List<Component> paintPanel(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        if (minecraft.level == null) {
            canvas.fill(0, 0, width, height, UiTheme.BACKGROUND);
        }
        return paint(canvas, mouseX, mouseY);
    }

    //? if >=1.21.11 {
    @Override
    public boolean isInGameUi() {
        return minecraft.level != null;
    }
    //?}

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiDraw.beginTooltips();
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        List<Component> tip = paintPanel(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
        GuiDraw.drawTooltip(pose);
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.beginTooltips();
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        List<Component> tip = paintPanel(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
        GuiDraw.drawTooltip(graphics);
    }
    //?}

    //? if >=1.20.2 && <26.1 {
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.beginTooltips();
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        List<Component> tip = paintPanel(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        canvas.tip(tip, mouseX, mouseY);
        GuiDraw.drawTooltip(graphics);
    }

    @Override
    public void extractBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    *///?}
}
