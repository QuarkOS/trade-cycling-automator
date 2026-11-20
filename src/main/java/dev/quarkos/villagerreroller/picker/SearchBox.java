package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
//?}

/** Keeps inventory shortcuts from handling letter keys while the search field has focus. */
public final class SearchBox extends ThemedEditBox {
    private final Runnable submit;

    public SearchBox(Font font, Rect bounds, Component placeholder, Runnable submit) {
        super(font, bounds, placeholder);
        this.submit = submit;
        placeholder(placeholder);
    }

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
