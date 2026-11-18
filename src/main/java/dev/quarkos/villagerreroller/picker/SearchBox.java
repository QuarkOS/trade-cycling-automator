package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
//? if <1.19.3 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else {
import net.minecraft.client.gui.components.Tooltip;
//?}
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
//?}

/** Keeps inventory shortcuts from handling letter keys while the search field has focus. */
public final class SearchBox extends EditBox {
    private final Component placeholder;
    private final Runnable submit;

    public SearchBox(Font font, Rect bounds, Component placeholder, Runnable submit) {
        super(font, bounds.x(), bounds.y(), bounds.width(), bounds.height(), placeholder);
        this.placeholder = placeholder;
        this.submit = submit;
        //? if >=1.19.3 {
        String hint = placeholder.getString();
        String shown = GuiDraw.ellipsis(font, hint, bounds.width() - 2 * UiLayout.GAP);
        setHint(Component.literal(shown));
        if (!shown.equals(hint)) {
            setTooltip(Tooltip.create(placeholder));
        }
        //?}
    }

    //? if <1.19.3 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.renderButton(pose, mouseX, mouseY, partialTick);
        if (getValue().isEmpty() && !isFocused()) {
            Rect bounds = GuiDraw.bounds(this);
            Rect text = new Rect(bounds.x() + UiLayout.GAP, bounds.y(),
                    bounds.width() - 2 * UiLayout.GAP, bounds.height());
            GuiDraw.label(GuiDraw.of(pose), placeholder, text, GuiDraw.MUTED, mouseX, mouseY);
        }
    }
    *///?}

    //? if <1.21.9 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || keyCode == InputConstants.KEY_ESCAPE) {
            return false;
        }
        if (GuiDraw.enter(keyCode)) {
            submit.run();
        } else {
            super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        super.charTyped(codePoint, modifiers);
        return true;
    }
    *///?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || event.key() == InputConstants.KEY_ESCAPE) {
            return false;
        }
        if (GuiDraw.enter(event.key())) {
            submit.run();
        } else {
            super.keyPressed(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused()) {
            return false;
        }
        super.charTyped(event);
        return true;
    }
    //?}
}
