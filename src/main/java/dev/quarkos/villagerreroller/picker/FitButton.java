package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}

import java.util.List;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/** Standard button input and narration with the mod's flat controls and item slots. */
public final class FitButton extends Button {
    private final boolean primary;
    private Component hint;
    private boolean selected;
    private boolean checkbox;
    private ItemStack item;
    private String badge;

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

    static FitButton slot(Rect bounds, Component message, ItemStack icon, String badge, OnPress onPress) {
        FitButton button = new FitButton(bounds, message, onPress, false);
        button.setItem(icon, badge);
        return button;
    }

    static FitButton checkbox(Rect bounds, Component message, boolean checked, OnPress onPress) {
        FitButton button = new FitButton(bounds, message, onPress, false);
        button.checkbox = true;
        button.selected = checked;
        return button;
    }

    void setItem(ItemStack icon, String badge) {
        this.item = icon;
        this.badge = badge;
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

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        Rect bounds = GuiDraw.bounds(this);
        Component label = getMessage();
        boolean hovered = bounds.contains(mouseX, mouseY);
        boolean focused = active && isFocused();
        if (checkbox) {
            GuiDraw.frame(canvas, bounds, active && (selected || focused) ? ACCENT : BORDER,
                    active && hovered ? HOVER : BACKGROUND);
            if (selected) {
                canvas.text("✓", bounds.x() + 2, bounds.y() + 2, active ? ACCENT : DISABLED);
            }
            if (hovered) {
                canvas.tip(List.of(label), mouseX, mouseY);
            }
            return;
        }

        int fill = active && primary ? ACCENT : active && hovered ? HOVER : RAISED;
        int border = BORDER;
        if (active && (primary || focused || selected)) {
            border = primary && (hovered || focused) ? TEXT : ACCENT;
        }
        GuiDraw.frame(canvas, bounds, border, fill);

        boolean cut = false;
        if (item != null) {
            canvas.item(item, bounds.x() + (bounds.width() - 16) / 2,
                    bounds.y() + (bounds.height() - 16) / 2, badge);
        } else {
            String full = label.getString();
            String shown = GuiDraw.ellipsis(canvas.font(), full, bounds.width() - 2 * GAP);
            cut = !shown.equals(full);
            int color = !active ? DISABLED : primary ? BACKGROUND : TEXT;
            canvas.text(shown, bounds.x() + (bounds.width() - canvas.font().width(shown)) / 2,
                    bounds.y() + (bounds.height() - LINE_HEIGHT) / 2, color);
        }
        if (hovered) {
            if (cut || item != null) {
                canvas.tip(hint == null ? List.of(label) : List.of(label, hint), mouseX, mouseY);
            } else if (hint != null) {
                canvas.tip(List.of(hint), mouseX, mouseY);
            }
        }
    }

    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(pose), mouseX, mouseY);
    }
    *///?}

    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(pose), mouseX, mouseY);
    }
    *///?}

    //? if >=1.20 && <1.21.11 {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(graphics), mouseX, mouseY);
    }
    *///?}

    //? if >=1.21.11 && <26.1 {
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(graphics), mouseX, mouseY);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(GuiDraw.of(graphics), mouseX, mouseY);
    }
    *///?}
}
