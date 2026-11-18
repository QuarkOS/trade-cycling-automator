package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import dev.quarkos.villagerreroller.text.TextFit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/**
 * Shared window chrome. Screens paint once and the version-specific canvas
 * does the actual fill and text calls.
 */
public final class GuiDraw {
    private static Tooltip tooltip;

    private GuiDraw() {
    }

    public interface Canvas {
        Font font();

        void fill(int x1, int y1, int x2, int y2, int color);

        void text(String text, int x, int y, int color);

        void scissor(int x1, int y1, int x2, int y2);

        void unscissor();

        void item(ItemStack stack, int x, int y, String badge);

        default void item(ItemStack stack, int x, int y) {
            item(stack, x, y, null);
        }

        default void tip(List<Component> lines, int mouseX, int mouseY) {
            if (!lines.isEmpty()) {
                tooltip = new Tooltip(lines, mouseX, mouseY);
            }
        }

        default void checkbox(int x, int y, boolean selected) {
            frame(this, new Rect(x, y, 12, 12), selected ? ACCENT : BORDER, BACKGROUND);
            if (selected) {
                text("✓", x + 2, y + 2, ACCENT);
            }
        }
    }

    public static Component message(String key, Object... args) {
        return Component.translatable("text." + VillagerReroller.MOD_ID + "." + key, args);
    }

    public static boolean enter(int keyCode) {
        return keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER;
    }

    public static Rect bounds(AbstractWidget widget) {
        //? if <1.19.3 {
        /*return new Rect(widget.x, widget.y, widget.getWidth(), widget.getHeight());
        *///?} else {
        return new Rect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
        //?}
    }

    public static String ellipsis(Font font, String text, int maxWidth) {
        return TextFit.ellipsis(text, maxWidth, font::width, font::plainSubstrByWidth);
    }

    /** Draws one line in its box and reports whether any of the text was shortened. */
    public static boolean line(Canvas canvas, String text, Rect box, int color) {
        String shown = ellipsis(canvas.font(), text, box.width());
        if (!shown.isEmpty()) {
            canvas.text(shown, box.x(), box.y() + (box.height() - LINE_HEIGHT) / 2, color);
        }
        return !shown.equals(text);
    }

    public static void label(Canvas canvas, Component label, Rect box, int color, int mouseX, int mouseY) {
        if (line(canvas, label.getString(), box, color) && box.contains(mouseX, mouseY)) {
            canvas.tip(List.of(label), mouseX, mouseY);
        }
    }

    public static void hint(Canvas canvas, Component hint, Rect box, int color, int mouseX, int mouseY) {
        List<String> lines = wrap(canvas.font(), hint.getString(), box.width());
        int shown = Math.min(lines.size(), box.height() / LINE_HEIGHT);
        for (int i = 0; i < shown; i++) {
            canvas.text(lines.get(i), box.x(), box.y() + i * LINE_HEIGHT, color);
        }
        if (shown < lines.size() && box.contains(mouseX, mouseY)) {
            canvas.tip(List.of(hint), mouseX, mouseY);
        }
    }

    public static void panel(Canvas canvas, Rect box) {
        frame(canvas, box, BORDER, PANEL);
    }

    public static void well(Canvas canvas, Rect box) {
        frame(canvas, box, BORDER, BACKGROUND);
    }

    public static void frame(Canvas canvas, Rect box, int border, int interior) {
        if (box.width() < 2 || box.height() < 2) {
            return;
        }
        canvas.fill(box.x(), box.y(), box.right(), box.y() + 1, border);
        canvas.fill(box.x(), box.bottom() - 1, box.right(), box.bottom(), border);
        canvas.fill(box.x(), box.y() + 1, box.x() + 1, box.bottom() - 1, border);
        canvas.fill(box.right() - 1, box.y() + 1, box.right(), box.bottom() - 1, border);
        fill(canvas, box.inset(1), interior);
    }

    public static void fill(Canvas canvas, Rect box, int color) {
        canvas.fill(box.x(), box.y(), box.right(), box.bottom(), color);
    }

    public static void row(Canvas canvas, Rect box, UiStatus status, boolean hovered) {
        canvas.fill(box.x(), box.y(), box.right(), box.bottom() - 1, hovered ? HOVER : RAISED);
        canvas.fill(box.x(), box.bottom() - 1, box.right(), box.bottom(), DIVIDER);
        if (status != UiStatus.IDLE) {
            canvas.fill(box.x(), box.y(), box.x() + 2, box.bottom() - 1, status.color());
        }
    }

    public static void scrollbar(Canvas canvas, Rect track, ListScroll bar, int count, int visible) {
        int max = ListScroll.maxOffset(count, visible);
        if (max > 0) {
            fill(canvas, track, BACKGROUND);
            int thumb = ListScroll.thumbHeight(track.height(), count, visible);
            int top = track.y() + ListScroll.thumbTop(track.height(), thumb, bar.offset(), max);
            canvas.fill(track.x() + 2, top, track.right() - 1, top + thumb, MUTED);
        }
    }

    public static void empty(Canvas canvas, Rect box, Component title, Component hint, int mouseX, int mouseY) {
        Font font = canvas.font();
        int width = Math.max(0, box.width() - 2 * GAP);
        String heading = ellipsis(font, title.getString(), width);
        List<String> detail = wrap(font, hint.getString(), width);
        int capacity = Math.max(0, box.height() / LINE_HEIGHT - 1);
        int shown = Math.min(detail.size(), capacity);
        int top = box.y() + (box.height() - (shown + 1) * LINE_HEIGHT) / 2;
        if (box.height() >= LINE_HEIGHT) {
            canvas.text(heading, box.x() + (box.width() - font.width(heading)) / 2, top, TEXT);
        }
        for (int i = 0; i < shown; i++) {
            String line = detail.get(i);
            canvas.text(line, box.x() + (box.width() - font.width(line)) / 2, top + (i + 1) * LINE_HEIGHT, MUTED);
        }
        if (box.contains(mouseX, mouseY) && (!heading.equals(title.getString()) || shown < detail.size())) {
            canvas.tip(List.of(title, hint), mouseX, mouseY);
        }
    }

    public static void beginTooltips() {
        tooltip = null;
    }

    //? if <1.20 {
    /*public static void drawTooltip(PoseStack pose) {
        Tooltip pending = tooltip;
        tooltip = null;
        if (pending != null) {
            pose.pushPose();
            pose.translate(0, 0, 400);
            drawTip(of(pose), pending);
            pose.popPose();
        }
    }
    *///?} else {
    public static void drawTooltip(GuiGraphics graphics) {
        Tooltip pending = tooltip;
        tooltip = null;
        if (pending != null) {
            //? if >=1.21.6 {
            graphics.nextStratum();
            drawTip(of(graphics), pending);
            //?} else {
            /*graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            drawTip(of(graphics), pending);
            graphics.pose().popPose();
            *///?}
        }
    }
    //?}

    private record Tooltip(List<Component> lines, int x, int y) {
    }

    //? if <1.20 {
    /*public static Canvas of(PoseStack pose) {
        return new PoseCanvas(pose);
    }

    private static final class PoseCanvas implements Canvas {
        private final PoseStack pose;
        private final Font font;

        private PoseCanvas(PoseStack pose) {
            this.pose = pose;
            this.font = Minecraft.getInstance().font;
        }

        @Override
        public Font font() {
            return font;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            net.minecraft.client.gui.screens.Screen.fill(pose, x1, y1, x2, y2, color);
        }

        @Override
        public void text(String text, int x, int y, int color) {
            font.draw(pose, text, x, y, color);
        }

        @Override
        public void scissor(int x1, int y1, int x2, int y2) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int windowHeight = Minecraft.getInstance().getWindow().getHeight();
            com.mojang.blaze3d.systems.RenderSystem.enableScissor(
                    (int) (x1 * scale),
                    (int) (windowHeight - (double) y2 * scale),
                    Math.max(0, (int) ((x2 - x1) * scale)),
                    Math.max(0, (int) ((y2 - y1) * scale)));
        }

        @Override
        public void unscissor() {
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        }

        @Override
        public void item(ItemStack stack, int x, int y, String badge) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            var renderer = Minecraft.getInstance().getItemRenderer();
            //? if <1.19.4 {
            renderer.renderGuiItem(stack, x, y);
            //?} else {
            renderer.renderGuiItem(pose, stack, x, y);
            //?}
            if (badge == null) {
                //? if <1.19.4 {
                renderer.renderGuiItemDecorations(font, stack, x, y, null);
                //?} else {
                renderer.renderGuiItemDecorations(pose, font, stack, x, y, null);
                //?}
                return;
            }
            pose.pushPose();
            pose.translate(x + 16, y + 12, 200);
            pose.scale(0.5F, 0.5F, 1);
            font.drawShadow(pose, badge, -font.width(badge), 0, TEXT);
            pose.popPose();
        }

    }
    *///?}

    //? if >=1.20 {
    public static Canvas of(GuiGraphics graphics) {
        return new GraphicsCanvas(graphics);
    }

    private static final class GraphicsCanvas implements Canvas {
        private final GuiGraphics graphics;
        private final Font font;

        private GraphicsCanvas(GuiGraphics graphics) {
            this.graphics = graphics;
            this.font = Minecraft.getInstance().font;
        }

        @Override
        public Font font() {
            return font;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            graphics.fill(x1, y1, x2, y2, color);
        }

        @Override
        public void text(String text, int x, int y, int color) {
            //? if >=26.1 {
            /*graphics.text(font, text, x, y, color, false);
            *///?} else {
            graphics.drawString(font, text, x, y, color, false);
            //?}
        }

        @Override
        public void scissor(int x1, int y1, int x2, int y2) {
            graphics.enableScissor(x1, y1, x2, y2);
        }

        @Override
        public void unscissor() {
            graphics.disableScissor();
        }

        @Override
        public void item(ItemStack stack, int x, int y, String badge) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            //? if >=26.1 {
            /*graphics.item(stack, x, y);
            *///?} else {
            graphics.renderItem(stack, x, y);
            //?}
            if (badge == null) {
                //? if >=26.1 {
                /*graphics.itemDecorations(font, stack, x, y);
                *///?} else {
                graphics.renderItemDecorations(font, stack, x, y);
                //?}
                return;
            }
            //? if >=1.21.6 {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 16, y + 12);
            graphics.pose().scale(0.5F);
            //? if >=26.1 {
            /*graphics.text(font, badge, -font.width(badge), 0, TEXT, true);
            *///?} else {
            graphics.drawString(font, badge, -font.width(badge), 0, TEXT, true);
            //?}
            graphics.pose().popMatrix();
            //?} else {
            /*graphics.pose().pushPose();
            graphics.pose().translate(x + 16, y + 12, 200);
            graphics.pose().scale(0.5F, 0.5F, 1);
            graphics.drawString(font, badge, -font.width(badge), 0, TEXT, true);
            graphics.pose().popPose();
            *///?}
        }

    }
    //?}

    private static void drawTip(Canvas canvas, Tooltip tip) {
        int screenW = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth());
        int screenH = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        int maxText = Math.min(240, Math.max(8, screenW - 12));
        Font font = canvas.font();
        int maxLines = Math.min(3, Math.max(1, (screenH - 8) / LINE_HEIGHT));
        List<String> text = shortTip(font, tip.lines(), maxText, maxLines);
        if (text.isEmpty()) {
            return;
        }
        int width = text.stream().mapToInt(font::width).max().orElse(0);
        int boxW = Math.min(width + 6, screenW);
        int height = text.size() * LINE_HEIGHT + 4;
        int x = tip.x() + 8;
        int y = tip.y() + 8;
        if (x + boxW > screenW) {
            x = tip.x() - boxW - 4;
        }
        x = Math.max(0, Math.min(x, screenW - boxW));
        if (y + height > screenH) {
            y = tip.y() - height - 4;
        }
        y = Math.max(0, Math.min(y, screenH - height));
        well(canvas, new Rect(x, y, boxW, height));
        for (int i = 0; i < text.size(); i++) {
            canvas.text(text.get(i), x + 3, y + 2 + i * LINE_HEIGHT, i == 0 ? TEXT : MUTED);
        }
    }

    private static List<String> shortTip(Font font, List<Component> lines, int width, int maxLines) {
        List<String> shown = new ArrayList<>(maxLines);
        for (Component line : lines) {
            for (String paragraph : line.getString().split("\\R")) {
                for (String wrapped : wrap(font, paragraph, width)) {
                    if (shown.size() == maxLines) {
                        int last = maxLines - 1;
                        shown.set(last, ellipsis(font, shown.get(last) + "…", width));
                        return shown;
                    }
                    if (!wrapped.isEmpty()) {
                        shown.add(wrapped);
                    }
                }
            }
        }
        return shown;
    }

    static List<String> wrap(Font font, String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (value.isBlank() || maxWidth <= 0) {
            return lines;
        }
        String rest = value.strip();
        while (!rest.isEmpty()) {
            if (font.width(rest) <= maxWidth) {
                lines.add(rest);
                break;
            }
            String cut = font.plainSubstrByWidth(rest, maxWidth);
            if (cut.isEmpty()) {
                break;
            }
            if (cut.length() < rest.length() && rest.charAt(cut.length()) != ' ') {
                int space = cut.lastIndexOf(' ');
                if (space > 0) {
                    cut = cut.substring(0, space);
                }
            }
            lines.add(cut.stripTrailing());
            rest = rest.substring(cut.length()).stripLeading();
        }
        return lines;
    }
}
