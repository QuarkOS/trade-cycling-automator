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
import net.minecraft.util.FormattedCharSequence;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;

/**
 * Shared window chrome. Screens paint once and the version-specific canvas
 * does the actual fill and text calls.
 */
public final class GuiDraw {
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA0A0A0;
    private static final int PANEL = 0xED191919;
    private static final int WELL = 0xDF101010;

    private GuiDraw() {
    }

    public interface Canvas {
        Font font();

        void fill(int x1, int y1, int x2, int y2, int color);

        void text(String text, int x, int y, int color);

        void scissor(int x1, int y1, int x2, int y2);

        void unscissor();

        void item(ItemStack stack, int x, int y);

        void tip(List<Component> lines, int mouseX, int mouseY);

        void scroller(int x, int y, int height);

        default void checkbox(int x, int y, boolean selected) {
            fill(x, y, x + 12, y + 12, MUTED);
            fill(x + 1, y + 1, x + 11, y + 11, WELL);
            if (selected) {
                text("✓", x + 2, y + 2, UiStatus.READY.color());
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

    public static void panel(Canvas canvas, Rect box) {
        frame(canvas, box, 0xFF515151, PANEL);
    }

    public static void well(Canvas canvas, Rect box) {
        frame(canvas, box, 0xFF393939, WELL);
    }

    private static void frame(Canvas canvas, Rect box, int border, int interior) {
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

    public static void row(Canvas canvas, Rect box, int index, UiStatus status, boolean hovered) {
        int fill = hovered ? 0xFF3B3B3B : index % 2 == 0 ? 0xFF292929 : 0xFF303030;
        canvas.fill(box.x(), box.y(), box.right(), box.bottom() - 1, fill);
        if (status != UiStatus.IDLE) {
            canvas.fill(box.x(), box.y(), box.x() + 2, box.bottom() - 1, status.color());
        }
    }

    public static void scrollbar(Canvas canvas, Rect track, ListScroll bar, int count, int visible) {
        int max = ListScroll.maxOffset(count, visible);
        if (max > 0) {
            fill(canvas, track, 0xFF222222);
            int thumb = ListScroll.thumbHeight(track.height(), count, visible);
            int top = track.y() + ListScroll.thumbTop(track.height(), thumb, bar.offset(), max);
            canvas.scroller(track.right() - 6, top, thumb);
        }
    }

    public static void empty(Canvas canvas, Rect box, Component title, Component hint, int mouseX, int mouseY) {
        int width = Math.max(0, box.width() - 2 * GAP);
        String heading = ellipsis(canvas.font(), title.getString(), width);
        boolean showHint = box.height() >= 2 * LINE_HEIGHT;
        String detail = showHint ? ellipsis(canvas.font(), hint.getString(), width) : "";
        int top = box.y() + Math.max(0, (box.height() - (showHint ? 2 : 1) * LINE_HEIGHT) / 2);
        canvas.text(heading, box.x() + (box.width() - canvas.font().width(heading)) / 2, top, TEXT);
        canvas.text(detail, box.x() + (box.width() - canvas.font().width(detail)) / 2, top + LINE_HEIGHT, MUTED);
        if (box.contains(mouseX, mouseY) && (!heading.equals(title.getString()) || !detail.equals(hint.getString()))) {
            canvas.tip(List.of(title, hint), mouseX, mouseY);
        }
    }

    //? if <1.21 {
    /*private static Identifier sprite(String path) {
        return new Identifier("minecraft", path);
    }
    *///?} else {
    private static Identifier sprite(String path) {
        return Identifier.withDefaultNamespace(path);
    }
    //?}

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
        public void item(ItemStack stack, int x, int y) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            var renderer = Minecraft.getInstance().getItemRenderer();
            //? if <1.19.4 {
            renderer.renderGuiItem(stack, x, y);
            renderer.renderGuiItemDecorations(font, stack, x, y);
            //?} else {
            renderer.renderGuiItem(pose, stack, x, y);
            renderer.renderGuiItemDecorations(pose, font, stack, x, y);
            //?}
        }

        @Override
        public void tip(List<Component> lines, int mouseX, int mouseY) {
            pose.pushPose();
            pose.translate(0, 0, 400);
            drawTip(this, lines, mouseX, mouseY);
            pose.popPose();
        }

        @Override
        public void scroller(int x, int y, int height) {
            if (height <= 0) {
                return;
            }
            fill(x, y, x + 6, y + height, 0xFFC6C6C6);
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
        public void item(ItemStack stack, int x, int y) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            //? if >=26.1 {
            /*graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
            *///?} else {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
            //?}
        }

        @Override
        public void tip(List<Component> lines, int mouseX, int mouseY) {
            if (lines.isEmpty()) {
                return;
            }
            int screenW = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth());
            int screenH = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledHeight());
            int width = Math.min(240, Math.max(8, screenW - 2 * PADDING));
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component line : lines) {
                wrapped.addAll(font.split(line, width));
            }
            if (wrapped.isEmpty()) {
                return;
            }
            // Vanilla tooltips do not shrink a list that is taller than the window.
            int maxLines = Math.max(1, (screenH - 20) / 12);
            if (wrapped.size() > maxLines) {
                wrapped = new ArrayList<>(wrapped.subList(0, Math.max(0, maxLines - 1)));
                wrapped.add(FormattedCharSequence.forward("…", net.minecraft.network.chat.Style.EMPTY));
            }
            //? if >=1.21.6 {
            graphics.setTooltipForNextFrame(font, wrapped, mouseX, mouseY);
            //?} else {
            /*graphics.renderTooltip(font, wrapped, mouseX, mouseY);
            *///?}
        }

        //? if >=1.20.2 {
        @Override
        public void checkbox(int x, int y, boolean selected) {
            Identifier sprite = sprite(selected ? "widget/checkbox_selected" : "widget/checkbox");
            //? if >=1.21.6 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 12, 12);
            //?} elif >=1.21.2 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, sprite, x, y, 12, 12);
            *///?} else {
            /*graphics.blitSprite(sprite, x, y, 12, 12);
            *///?}
        }
        //?}

        @Override
        public void scroller(int x, int y, int height) {
            if (height <= 0) {
                return;
            }
            Identifier sprite = sprite("container/villager/scroller");
            //? if >=1.21.6 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 6, height);
            //?}
            //? if >=1.21.2 && <1.21.6 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, sprite, x, y, 6, height);
            *///?}
            //? if >=1.20.2 && <1.21.2 {
            /*graphics.blitSprite(sprite, x, y, 6, height);
            *///?}
            //? if <1.20.2 {
            /*fill(x, y, x + 6, y + height, 0xFFC6C6C6);
            *///?}
        }
    }
    //?}

    private static void drawTip(Canvas canvas, List<Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int screenW = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth());
        int screenH = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        // A long trade list must stay on screen. Cap the box so it remains a
        // tooltip instead of covering the panel, and ellipsize whatever remains.
        int maxText = Math.min(240, Math.max(8, screenW - 12));
        Font font = canvas.font();
        List<String> text = new ArrayList<>();
        int width = 0;
        for (Component line : lines) {
            for (String paragraph : line.getString().split("\\R", -1)) {
                for (String value : wrap(font, paragraph, maxText)) {
                    text.add(value);
                    width = Math.max(width, font.width(value));
                }
            }
        }
        if (text.isEmpty()) {
            return;
        }
        int maxLines = Math.max(1, (screenH - 8) / LINE_HEIGHT);
        if (text.size() > maxLines) {
            String last = ellipsis(font, text.get(maxLines - 1) + "…", maxText);
            text = new ArrayList<>(text.subList(0, maxLines - 1));
            text.add(last);
            width = 0;
            for (String value : text) {
                width = Math.max(width, font.width(value));
            }
        }
        int boxW = Math.min(width + 6, screenW);
        int height = text.size() * 10 + 4;
        int x = mouseX + 8;
        int y = mouseY + 8;
        if (x + boxW > screenW) {
            x = mouseX - boxW - 4;
        }
        if (x < 0) {
            x = 0;
        }
        if (x + boxW > screenW) {
            x = Math.max(0, screenW - boxW);
        }
        if (y + height > screenH) {
            y = mouseY - height - 4;
        }
        if (y < 0) {
            y = 0;
        }
        if (y + height > screenH) {
            y = Math.max(0, screenH - height);
        }
        canvas.fill(x, y, x + boxW, y + height, 0xF0100010);
        int lineY = y + 2;
        for (String value : text) {
            canvas.text(value, x + 3, lineY, 0xFFFFFFFF);
            lineY += 10;
        }
    }

    static List<String> wrap(Font font, String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            lines.add("");
            return lines;
        }
        String rest = value;
        int guard = 0;
        while (!rest.isEmpty() && guard++ < 32) {
            if (font.width(rest) <= maxWidth) {
                lines.add(rest);
                break;
            }
            String cut = font.plainSubstrByWidth(rest, maxWidth);
            if (cut.isEmpty()) {
                cut = rest.substring(0, 1);
            }
            int space = cut.lastIndexOf(' ');
            if (space > 8 && cut.length() < rest.length()) {
                cut = cut.substring(0, space);
            }
            lines.add(cut);
            rest = rest.substring(cut.length());
            if (rest.startsWith(" ")) {
                rest = rest.substring(1);
            }
        }
        return lines;
    }
}
