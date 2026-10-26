package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.McBridge;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
 * Queue before a hall run, and the result list after one.
 * The trade targets themselves stay on the merchant panel.
 */
public final class TradeHallScreen extends Screen {
    private static final int TEXT = 0xFF404040;
    private static final int READY = 0xFF1B5E20;
    private static final int FOUND = 0xFF1B5E20;
    private static final int SKIPPED = 0xFF8A6A00;
    private static final int FAILED = 0xFFAA0000;
    private static final int MUTED = 0xFF606060;

    private final List<Component> lines = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();

    public TradeHallScreen() {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        TradeHall.dismiss();
        super.onClose();
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();
        boolean summary = TradeHall.showingSummary();
        int row = 0;
        if (!summary && !TradeHall.isActive()) {
            for (int i = 0; i < TradeHall.size(); i++) {
                int slot = i;
                int y = top + 36 + row * 24;
                addRenderableWidget(button(left + width - 58, y, 50, 16, Component.translatable("text." + VillagerReroller.MOD_ID + ".button.remove"), pressed -> {
                    TradeHall.remove(slot);
                    if (this.minecraft != null) {
                        McBridge.setScreen(this.minecraft, new TradeHallScreen());
                    }
                }));
                row++;
            }
        }
        int y = this.height - 28;
        int x = this.width / 2 - 154;
        if (!summary) {
            Button start = button(x, y, 100, 20, Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall_start"), pressed -> {
                if (this.minecraft != null) {
                    TradeHall.start(this.minecraft);
                }
            });
            start.active = TradeHall.size() > 0;
            addRenderableWidget(start);
            addRenderableWidget(button(x + 104, y, 100, 20, Component.translatable("text." + VillagerReroller.MOD_ID + ".button.clear"), pressed -> {
                TradeHall.clear();
                if (this.minecraft != null) {
                    McBridge.setScreen(this.minecraft, new TradeHallScreen());
                }
            }));
            addRenderableWidget(button(x + 208, y, 100, 20, Component.translatable("gui.done"), pressed -> onClose()));
        } else {
            addRenderableWidget(button(this.width / 2 - 50, y, 100, 20, Component.translatable("gui.done"), pressed -> onClose()));
        }
    }

    private void prepare() {
        lines.clear();
        colors.clear();
        boolean summary = TradeHall.showingSummary();
        if (summary) {
            add(TradeHall.countsLine(), TEXT);
        } else if (TradeHall.size() == 0) {
            add(Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.empty"), TEXT);
        }
        int number = 1;
        for (TradeHall.Slot slot : TradeHall.slots()) {
            Component status = status(slot, summary);
            add(Component.literal(number + ". ").append(slot.label()).append(" — ").append(status), color(slot, summary));
            if (!slot.description().isEmpty()) {
                add(Component.literal(slot.description()), MUTED);
            } else {
                add(Component.empty(), MUTED);
            }
            number++;
        }
    }

    private Component status(TradeHall.Slot slot, boolean summary) {
        if (summary || slot.outcome() != TradeHall.Outcome.PENDING) {
            return TradeHall.outcomeLine(slot);
        }
        if (this.minecraft == null) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.pending");
        }
        String blocker = TradeHall.blocker(this.minecraft, slot.villagerId());
        if (blocker == null) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.ready");
        }
        return Component.translatable(
                "text." + VillagerReroller.MOD_ID + ".hall.skipped",
                Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.why." + blocker));
    }

    private static int color(TradeHall.Slot slot, boolean summary) {
        if (!summary && slot.outcome() == TradeHall.Outcome.PENDING) {
            return READY;
        }
        return switch (slot.outcome()) {
            case FOUND -> FOUND;
            case SKIPPED, CANCELLED -> SKIPPED;
            case FAILED -> FAILED;
            case PENDING -> READY;
        };
    }

    private void add(Component component, int color) {
        lines.add(component);
        colors.add(color);
    }

    private int panelWidth() {
        return 300;
    }

    private int panelHeight() {
        return Math.min(this.height - 40, 48 + Math.max(1, TradeHall.size()) * 24);
    }

    private String hintLine() {
        return this.font.plainSubstrByWidth(
                Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.hint").getString(),
                panelWidth() - 16);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (this.height - panelHeight()) / 2 - 8);
    }

    private Button button(int x, int y, int width, int height, Component label, Button.OnPress press) {
        //? if <1.19.3 {
        /*return new Button(x, y, width, height, label, press);
        *///?} else {
        return Button.builder(label, press).bounds(x, y, width, height).build();
        //?}
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        prepare();
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = top + panelHeight();
        fill(pose, left, top, right, bottom, 0xFF000000);
        fill(pose, left + 2, top + 2, right - 2, bottom - 2, 0xFFC6C6C6);
        Component title = TradeHall.showingSummary()
                ? Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.summary")
                : Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title");
        this.font.draw(pose, title, left + 8, top + 8, TEXT);
        this.font.draw(pose, hintLine(), left + 8, top + 20, MUTED);
        int y = top + 36;
        for (int i = 0; i < lines.size(); i++) {
            String text = this.font.plainSubstrByWidth(lines.get(i).getString(), panelWidth() - 72);
            this.font.draw(pose, text, left + 8, y, colors.get(i));
            y += 12;
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        prepare();
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xFF000000);
        graphics.fill(left + 2, top + 2, left + panelWidth() - 2, top + panelHeight() - 2, 0xFFC6C6C6);
        Component title = TradeHall.showingSummary()
                ? Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.summary")
                : Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title");
        graphics.drawString(this.font, title, left + 8, top + 8, TEXT, false);
        graphics.drawString(this.font, hintLine(), left + 8, top + 20, MUTED, false);
        int y = top + 36;
        for (int i = 0; i < lines.size(); i++) {
            String text = this.font.plainSubstrByWidth(lines.get(i).getString(), panelWidth() - 72);
            graphics.drawString(this.font, text, left + 8, y, colors.get(i), false);
            y += 12;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        prepare();
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xFF000000);
        graphics.fill(left + 2, top + 2, left + panelWidth() - 2, top + panelHeight() - 2, 0xFFC6C6C6);
        Component title = TradeHall.showingSummary()
                ? Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.summary")
                : Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.title");
        graphics.text(this.font, title, left + 8, top + 8, TEXT, false);
        graphics.text(this.font, hintLine(), left + 8, top + 20, MUTED, false);
        int y = top + 36;
        for (int i = 0; i < lines.size(); i++) {
            String text = this.font.plainSubstrByWidth(lines.get(i).getString(), panelWidth() - 72);
            graphics.text(this.font, text, left + 8, y, colors.get(i), false);
            y += 12;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    *///?}
}
