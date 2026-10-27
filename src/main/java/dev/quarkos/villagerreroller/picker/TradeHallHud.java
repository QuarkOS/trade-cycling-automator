package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import net.minecraft.client.Minecraft;
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

    private static List<Component> lines() {
        if (!TradeHall.isActive()) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title"));
        lines.add(TradeHall.hudTitle());
        Component detail = TradeHall.hudDetail();
        if (!detail.getString().isEmpty() && !detail.getString().equals(lines.get(1).getString())) {
            lines.add(detail);
        }
        Component counts = TradeHall.countsLine();
        if (!counts.getString().equals(lines.get(lines.size() - 1).getString())) {
            lines.add(counts);
        }
        return lines;
    }

    //? if <1.20 {
    /*public static void draw(PoseStack pose) {
        List<Component> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (font == null) {
            return;
        }
        int width = 8;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int left = 4;
        int top = 4;
        int right = left + width + 8;
        int bottom = top + lines.size() * 11 + 6;
        net.minecraft.client.gui.GuiComponent.fill(pose, left, top, right, bottom, 0xC0101010);
        int y = top + 4;
        for (Component line : lines) {
            font.drawShadow(pose, line, left + 4, y, 0xFFFFFF);
            y += 11;
        }
    }
    *///?}

    //? if >=1.20 {
    public static void draw(GuiGraphics graphics) {
        List<Component> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (font == null) {
            return;
        }
        int width = 8;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int left = 4;
        int top = 4;
        graphics.fill(left, top, left + width + 8, top + lines.size() * 11 + 6, 0xC0101010);
        int y = top + 4;
        for (Component line : lines) {
            //? if >=26.1 {
            /*graphics.text(font, line, left + 4, y, 0xFFFFFF, true);
            *///?} else {
            graphics.drawString(font, line, left + 4, y, 0xFFFFFF, true);
            //?}
            y += 11;
        }
    }
    //?}
}
