package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import net.minecraft.client.Minecraft;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}

import java.util.ArrayList;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.GuiDraw.MUTED;
import static dev.quarkos.villagerreroller.picker.GuiDraw.TEXT;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;

/**
 * Progress overlay while a hall run is moving from one job site to the next.
 * It only changes where the player is looking. It does not walk them.
 */
public final class TradeHallHud {
    private TradeHallHud() {
    }

    //? if fabric {
    public static void register() {
        //? if <1.20.2 {
        /*net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((pose, tickDelta) -> draw(pose));
        *///?}
        //? if >=1.20.2 && <26.1 {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, tickCounter) -> draw(graphics));
        //?}
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(VillagerReroller.MOD_ID, "hall"),
                (graphics, delta) -> draw(graphics));
        *///?}
    }
    //?}

    //? if <1.20 {
    /*public static void draw(PoseStack pose) {
        paint(GuiDraw.of(pose));
    }
    *///?} else {
    public static void draw(GuiGraphics graphics) {
        paint(GuiDraw.of(graphics));
    }
    //?}

    private static void paint(GuiDraw.Canvas canvas) {
        if (!TradeHall.isActive()) {
            return;
        }
        Font font = canvas.font();
        if (font == null) {
            return;
        }
        var window = Minecraft.getInstance().getWindow();
        int maxWidth = Math.min(256, window.getGuiScaledWidth() - 2 * (GAP + PADDING));
        int maxLines = (window.getGuiScaledHeight() - 2 * (GAP + PADDING)) / LINE_HEIGHT;
        if (maxWidth <= 0 || maxLines <= 0) {
            return;
        }

        List<HudLine> lines = new ArrayList<>();
        addLines(lines, font, maxWidth, message("hall.title"), TEXT);
        addLines(lines, font, maxWidth, TradeHall.hudTitle(), TradeHall.hudStatus().color());
        Component detail = TradeHall.hudDetail();
        addLines(lines, font, maxWidth, detail, MUTED);
        Component counts = TradeHall.countsLine();
        if (!counts.equals(detail)) {
            addLines(lines, font, maxWidth, counts, MUTED);
        }

        int rows = Math.min(lines.size(), maxLines);
        int width = 0;
        for (int i = 0; i < rows; i++) {
            width = Math.max(width, font.width(lines.get(i).text()));
        }
        Rect panel = new Rect(GAP, GAP, width + 2 * PADDING, rows * LINE_HEIGHT + 2 * PADDING);
        GuiDraw.panel(canvas, panel);
        for (int i = 0; i < rows; i++) {
            HudLine line = lines.get(i);
            Rect bounds = new Rect(panel.x() + PADDING, panel.y() + PADDING + i * LINE_HEIGHT, width, LINE_HEIGHT);
            GuiDraw.line(canvas, line.text(), bounds, line.color());
        }
    }

    private static void addLines(List<HudLine> lines, Font font, int maxWidth, Component text, int color) {
        for (String line : GuiDraw.wrap(font, text.getString(), maxWidth)) {
            if (!line.isEmpty()) {
                lines.add(new HudLine(line, color));
            }
        }
    }

    private record HudLine(String text, int color) {
    }
}
