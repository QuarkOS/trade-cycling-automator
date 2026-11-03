package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
//?}

/**
 * Search field that keeps keybinds while it is focused.
 * Letter keys such as inventory (E), drop (Q), and the hotbar numbers are delivered to
 * {@code keyPressed} before {@code charTyped}. An edit box returns false for those keys,
 * and a container screen then closes or acts on the key. Returning true swallows them.
 * Escape still returns false so the screen can unfocus or close the way it already does.
 */
public final class SearchBox extends EditBox {
    public SearchBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    //? if <1.21.9 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || keyCode == 256) {
            return false;
        }
        super.keyPressed(keyCode, scanCode, modifiers);
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
        if (!isFocused() || event.key() == 256) {
            return false;
        }
        super.keyPressed(event);
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
