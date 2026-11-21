package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/** Uses vanilla text editing inside a padded, themed field. */
public class ThemedEditBox extends EditBox {
    private final int topInset;
    private final int outerHeight;
    private Component placeholder;

    public ThemedEditBox(Font font, Rect bounds, Component title) {
        super(font, bounds.x() + GAP, bounds.y() + textInset(bounds), Math.max(1, bounds.width() - 2 * GAP),
                bounds.height() - textInset(bounds), title);
        topInset = textInset(bounds);
        outerHeight = bounds.height();
        setBordered(false);
        setTextColor(TEXT);
        setTextColorUneditable(DISABLED);
    }

    protected final void placeholder(Component label) {
        placeholder = label;
    }

    public final void place(Rect bounds) {
        FitButton.place(this, bounds.x() + GAP, bounds.y() + topInset);
    }

    private static int textInset(Rect bounds) {
        return Math.max(0, (bounds.height() - 8) / 2);
    }

    private Rect frame() {
        Rect text = GuiDraw.bounds(this);
        return new Rect(text.x() - GAP, text.y() - topInset, text.width() + 2 * GAP, outerHeight);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && frame().contains(mouseX, mouseY);
    }

    //? if >=1.19.3 && <1.21.4 {
    /*@Override
    protected boolean clicked(double mouseX, double mouseY) {
        return active && isMouseOver(mouseX, mouseY);
    }
    *///?}
    //? if <1.19.3 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            Rect text = GuiDraw.bounds(this);
            mouseX = Math.max(text.x(), Math.min(mouseX, text.right() - 1));
            mouseY = Math.max(text.y(), mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?}

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        Rect frame = frame();
        GuiDraw.frame(canvas, frame, isFocused() ? ACCENT : BORDER, frame.contains(mouseX, mouseY) ? HOVER : RAISED);
        if (placeholder != null && getValue().isEmpty() && !isFocused()) {
            Rect text = new Rect(frame.x() + GAP, frame.y(), getWidth(), frame.height());
            GuiDraw.label(canvas, placeholder, text, MUTED, mouseX, mouseY);
        }
    }

    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(pose), mouseX, mouseY);
        super.renderButton(pose, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(pose), mouseX, mouseY);
        super.renderWidget(pose, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >=1.20 {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(graphics), mouseX, mouseY);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }
    //?}
}
