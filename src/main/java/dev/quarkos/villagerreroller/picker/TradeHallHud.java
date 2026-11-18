package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.McBridge;
import net.minecraft.client.Minecraft;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}

import java.util.ArrayList;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.UiTheme.MUTED;
import static dev.quarkos.villagerreroller.picker.UiTheme.TEXT;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;

/** Hall progress, discovery notices and the cheapest highlighted offers. */
public final class TradeHallHud {
    private static final long NOTICE_NANOS = 3_000_000_000L;
    private static ClientLevel noticeLevel;
    private static long noticeUntil;
    private static int discoveryCount;

    private TradeHallHud() {
    }

    static void showDiscovery(Minecraft client, int added) {
        if (added > 0) {
            noticeLevel = client.level;
            discoveryCount = added;
            noticeUntil = System.nanoTime() + NOTICE_NANOS;
        }
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
        Minecraft client = Minecraft.getInstance();
        boolean active = TradeHall.isActive();
        boolean discovery = noticeLevel != null && noticeLevel == client.level && System.nanoTime() < noticeUntil;
        if (!discovery) {
            noticeLevel = null;
        }
        var ranks = McBridge.currentScreen(client) instanceof TradeHallScreen
                ? List.<PriceRanking.Ranked>of() : HallHighlights.ranks();
        if (!active && !discovery && ranks.isEmpty()) {
            return;
        }
        Font font = canvas.font();
        if (font == null) {
            return;
        }
        var window = client.getWindow();
        int maxWidth = Math.min(256, window.getGuiScaledWidth() - 2 * (GAP + PADDING));
        int maxLines = (window.getGuiScaledHeight() - 2 * (GAP + PADDING)) / LINE_HEIGHT;
        if (maxWidth <= 0 || maxLines <= 0) {
            return;
        }

        List<HudLine> lines = new ArrayList<>();
        if (active) {
            addLine(lines, font, maxWidth, TradeHall.hudTitle(), TradeHall.hudStatus().color());
            addLine(lines, font, maxWidth, TradeHall.hudDetail(), MUTED);
        }
        if (discovery) {
            addLine(lines, font, maxWidth, message(discoveryCount == 1 ? "hall.discovery_one" : "hall.discovery_many",
                    discoveryCount), UiStatus.READY.color());
        }
        if (!ranks.isEmpty()) {
            addLine(lines, font, maxWidth, HallHighlights.selected().name(), TEXT);
            for (int i = 0; i < Math.min(ranks.size(), HallHighlights.PREVIEW_COUNT); i++) {
                PriceRanking.Ranked rank = ranks.get(i);
                addLine(lines, font, maxWidth, HallHighlights.label(rank), rank.color());
            }
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

    private static void addLine(List<HudLine> lines, Font font, int maxWidth, Component text, int color) {
        String line = GuiDraw.ellipsis(font, text.getString(), maxWidth);
        if (!line.isEmpty()) {
            lines.add(new HudLine(line, color));
        }
    }

    private record HudLine(String text, int color) {
    }
}
