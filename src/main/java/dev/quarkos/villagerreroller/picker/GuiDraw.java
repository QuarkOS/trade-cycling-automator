package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
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

/**
 * Shared window chrome. Screens paint once and the version-specific canvas
 * does the actual fill and text calls.
 */
public final class GuiDraw {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int TEXT = 0xFF202020;
    public static final int BODY = 0xFF303030;
    public static final int MUTED = 0xFF505050;

    private GuiDraw() {
    }

    public interface Canvas {
        Font font();

        void fill(int x1, int y1, int x2, int y2, int color);

        void text(String text, int x, int y, int color);

        void text(Component text, int x, int y, int color);

        void scissor(int x1, int y1, int x2, int y2);

        void unscissor();

        void item(ItemStack stack, int x, int y);

        void tip(List<Component> lines, int mouseX, int mouseY);

        void scroller(int x, int y, boolean disabled);
    }

    public static String ellipsis(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String dots = "\u2026";
        int budget = maxWidth - font.width(dots);
        if (budget <= 0) {
            return dots;
        }
        String cut = font.plainSubstrByWidth(text, budget);
        return cut + dots;
    }

    /** Draws the string, and returns it when the caller should show a tooltip. */
    public static String fit(Canvas canvas, String text, int x, int y, int maxWidth, int color) {
        String shown = ellipsis(canvas.font(), text, maxWidth);
        if (!shown.isEmpty()) {
            canvas.text(shown, x, y, color);
        }
        return shown.equals(text) ? null : text;
    }

    public static void beveled(Canvas canvas, int x, int y, int width, int height) {
        canvas.fill(x, y, x + width, y + height, 0xFF000000);
        canvas.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        canvas.fill(x + 1, y + 1, x + width - 1, y + 3, 0xFFFFFFFF);
        canvas.fill(x + 1, y + 1, x + 3, y + height - 1, 0xFFFFFFFF);
        canvas.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
        canvas.fill(x + width - 3, y + 1, x + width - 1, y + height - 1, 0xFF555555);
    }

    public static void recessed(Canvas canvas, int x, int y, int width, int height, int fill) {
        canvas.fill(x, y, x + width, y + height, 0xFF373737);
        canvas.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        canvas.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFFFFFFFF);
        canvas.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
    }

    /** @return the pill width, including its border */
    public static int pill(Canvas canvas, int x, int y, String text, int fill, int border, int textColor) {
        int width = canvas.font().width(text) + 6;
        int height = 11;
        canvas.fill(x, y, x + width, y + height, border);
        canvas.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        canvas.text(text, x + 3, y + 2, textColor);
        return width;
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
        public void text(Component text, int x, int y, int color) {
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
            drawTip(this, lines, mouseX, mouseY);
        }

        @Override
        public void scroller(int x, int y, boolean disabled) {
            fill(x, y, x + 6, y + 27, disabled ? 0xFF555555 : 0xFFC6C6C6);
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
        public void text(Component text, int x, int y, int color) {
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
            drawTip(this, lines, mouseX, mouseY);
        }

        @Override
        public void scroller(int x, int y, boolean disabled) {
            Identifier sprite = sprite(disabled
                    ? "container/villager/scroller_disabled"
                    : "container/villager/scroller");
            //? if >=1.21.6 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 6, 27);
            //?}
            //? if >=1.21.2 && <1.21.6 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, sprite, x, y, 6, 27);
            *///?}
            //? if >=1.20.2 && <1.21.2 {
            /*graphics.blitSprite(sprite, x, y, 6, 27);
            *///?}
            //? if <1.20.2 {
            /*fill(x, y, x + 6, y + 27, disabled ? 0xFF555555 : 0xFFC6C6C6);
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
            for (String value : wrap(font, line.getString(), maxText)) {
                text.add(value);
                width = Math.max(width, font.width(value));
            }
        }
        if (text.isEmpty()) {
            return;
        }
        int maxLines = Math.max(1, Math.min(8, (screenH - 8) / 10));
        if (text.size() > maxLines) {
            String last = ellipsis(font, text.get(maxLines - 1), maxText);
            text = new ArrayList<>(text.subList(0, maxLines - 1));
            text.add(last);
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

    private static List<String> wrap(Font font, String value, int maxWidth) {
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
