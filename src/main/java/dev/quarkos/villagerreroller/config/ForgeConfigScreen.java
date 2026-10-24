//? if forge {
/*
package dev.quarkos.villagerreroller.config;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
import com.mojang.blaze3d.vertex.PoseStack;
//?}

import java.util.Optional;

// Vanilla widgets for the Forge mod-list Config button. Writes villager_reroller.json.
public final class ForgeConfigScreen extends Screen {
    private final Screen parent;
    private final String[] enchantments;
    private int enchantIndex;
    private int level;
    private boolean enabled;
    private boolean panic;
    private Button enabledButton;
    private Button enchantmentButton;
    private Button levelButton;
    private Button statusButton;
    private EditBox priceBox;
    private EditBox attemptsBox;

    public ForgeConfigScreen(Screen parent) {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.title"));
        this.parent = parent;
        AutomatorConfig config = AutomatorConfig.get();
        config.ensureListedEnchantment();
        this.enchantments = LibrarianBookPrices.get().enchantments().stream()
                .map(LibrarianBookPrices.BookEnchantment::id)
                .toArray(String[]::new);
        this.enchantIndex = indexOf(config.enchantment());
        this.level = clamp(config.level(), 1, 5);
        this.enabled = config.enabled();
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = 32;
        //? if <1.19.3 {
        enabledButton = addRenderableWidget(new Button(x, y, 200, 20, enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            refreshStatus();
        }));
        //?}
        //? if >=1.19.3 {
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            refreshStatus();
        }).bounds(x, y, 200, 20).build());
        //?}
        y += 24;
        //? if <1.19.3 {
        enchantmentButton = addRenderableWidget(new Button(x, y, 200, 20, enchantmentLabel(), button -> {
            if (enchantments.length == 0) {
                return;
            }
            enchantIndex = (enchantIndex + 1) % enchantments.length;
            button.setMessage(enchantmentLabel());
            refreshStatus();
        }));
        //?}
        //? if >=1.19.3 {
        enchantmentButton = addRenderableWidget(Button.builder(enchantmentLabel(), button -> {
            if (enchantments.length == 0) {
                return;
            }
            enchantIndex = (enchantIndex + 1) % enchantments.length;
            button.setMessage(enchantmentLabel());
            refreshStatus();
        }).bounds(x, y, 200, 20).build());
        //?}
        y += 24;
        //? if <1.19.3 {
        levelButton = addRenderableWidget(new Button(x, y, 200, 20, levelLabel(), button -> {
            level = level >= 5 ? 1 : level + 1;
            button.setMessage(levelLabel());
            refreshStatus();
        }));
        //?}
        //? if >=1.19.3 {
        levelButton = addRenderableWidget(Button.builder(levelLabel(), button -> {
            level = level >= 5 ? 1 : level + 1;
            button.setMessage(levelLabel());
            refreshStatus();
        }).bounds(x, y, 200, 20).build());
        //?}
        y += 36;
        priceBox = new EditBox(this.font, x, y, 200, 20, priceLabel());
        priceBox.setMaxLength(3);
        priceBox.setValue(Integer.toString(AutomatorConfig.get().maxPrice()));
        //? if >=1.19.3 {
        priceBox.setHint(priceLabel());
        //?}
        priceBox.setResponder(value -> refreshStatus());
        addRenderableWidget(priceBox);
        y += 36;
        attemptsBox = new EditBox(this.font, x, y, 200, 20, attemptsLabel());
        attemptsBox.setMaxLength(5);
        attemptsBox.setValue(Integer.toString(AutomatorConfig.get().maxAttempts()));
        //? if >=1.19.3 {
        attemptsBox.setHint(attemptsLabel());
        //?}
        addRenderableWidget(attemptsBox);
        y += 28;
        //? if <1.19.3 {
        statusButton = addRenderableWidget(new Button(x, y, 200, 20, Component.literal("..."), button -> {
        }));
        //?}
        //? if >=1.19.3 {
        statusButton = addRenderableWidget(Button.builder(Component.literal("..."), button -> {
        }).bounds(x, y, 200, 20).build());
        //?}
        statusButton.active = false;
        y += 24;
        //? if <1.19.3 {
        addRenderableWidget(new Button(x, y, 200, 20, Component.translatable("text." + VillagerReroller.MOD_ID + ".config.panic"), button -> {
            panic = !panic;
            button.setMessage(Component.translatable(panic
                    ? "text." + VillagerReroller.MOD_ID + ".config.panic.on"
                    : "text." + VillagerReroller.MOD_ID + ".config.panic"));
        }));
        addRenderableWidget(new Button(x, this.height - 28, 200, 20, Component.translatable("gui.done"), button -> saveAndClose()));
        //?}
        //? if >=1.19.3 {
        addRenderableWidget(Button.builder(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.panic"), button -> {
            panic = !panic;
            button.setMessage(Component.translatable(panic
                    ? "text." + VillagerReroller.MOD_ID + ".config.panic.on"
                    : "text." + VillagerReroller.MOD_ID + ".config.panic"));
        }).bounds(x, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(x, this.height - 28, 200, 20)
                .build());
        //?}
        refreshStatus();
    }

    private void saveAndClose() {
        AutomatorConfig config = AutomatorConfig.get();
        if (enchantments.length > 0) {
            config.setEnchantment(enchantments[enchantIndex]);
        }
        config.setLevel(level);
        config.setMaxPrice(parse(priceBox.getValue(), config.maxPrice()));
        config.setMaxAttempts(parse(attemptsBox.getValue(), config.maxAttempts()));
        config.setEnabled(enabled);
        if (!enabled) {
            CycleAutomator.stop(CycleAutomator.StopReason.DISABLED);
        }
        if (panic) {
            CycleAutomator.stop(CycleAutomator.StopReason.PANIC);
        }
        Optional<LibrarianBookPrices.Problem> problem = config.problem();
        if (problem.isPresent()) {
            statusButton.setMessage(problemText(problem.get()));
            return;
        }
        AutomatorConfig.save();
        if (this.minecraft != null) {
            McBridge.setScreen(this.minecraft, parent);
        }
    }

    private void refreshStatus() {
        if (statusButton == null || enchantments.length == 0) {
            return;
        }
        Optional<LibrarianBookPrices.Problem> problem = LibrarianBookPrices.get().validate(
                enchantments[enchantIndex],
                level,
                parse(priceBox == null ? "10" : priceBox.getValue(), 10));
        statusButton.setMessage(problem.map(this::problemText).orElseGet(() -> Component.translatable(
                "text." + VillagerReroller.MOD_ID + ".config.valid")));
    }

    private Component problemText(LibrarianBookPrices.Problem problem) {
        return Component.translatable(
                "text." + VillagerReroller.MOD_ID + ".invalid." + problem.kind().name().toLowerCase(),
                problem.limit());
    }

    private Component enabledLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enabled")
                .append(": ")
                .append(Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private Component enchantmentLabel() {
        if (enchantments.length == 0) {
            return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enchantment");
        }
        String id = enchantments[enchantIndex];
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enchantment")
                .append(": ")
                .append(Component.translatable("enchantment." + id.replace(':', '.')));
    }

    private Component levelLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.level")
                .append(": ")
                .append(Integer.toString(level));
    }

    private int indexOf(String id) {
        for (int i = 0; i < enchantments.length; i++) {
            if (enchantments[i].equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private static int parse(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Component priceLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_price");
    }

    private static Component attemptsLabel() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_attempts");
    }

    private static Component priceTooltip() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_price.tooltip");
    }

    private static Component attemptsTooltip() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_attempts.tooltip");
    }

    // The edit box message is narration-only. Draw the Cloth Config names above the fields.
    //? if <1.20 {
    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.render(pose, mouseX, mouseY, partialTick);
        drawFieldLabel(pose, priceBox, priceLabel());
        drawFieldLabel(pose, attemptsBox, attemptsLabel());
        Component tip = hoveredTip(mouseX, mouseY);
        if (tip != null) {
            renderTooltip(pose, tip, mouseX, mouseY);
        }
    }

    private void drawFieldLabel(PoseStack pose, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        this.font.drawShadow(pose, label, boxX(box), boxY(box) - 12, 0xFFFFFF);
    }
    //?}
    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFieldLabel(graphics, priceBox, priceLabel());
        drawFieldLabel(graphics, attemptsBox, attemptsLabel());
        Component tip = hoveredTip(mouseX, mouseY);
        if (tip != null) {
            //? if <1.21.6 {
            graphics.renderTooltip(this.font, tip, mouseX, mouseY);
            //?}
            //? if >=1.21.6 {
            graphics.setTooltipForNextFrame(this.font, tip, mouseX, mouseY);
            //?}
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        graphics.drawString(this.font, label, boxX(box), boxY(box) - 12, 0xFFFFFFFF, true);
    }
    //?}
    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawFieldLabel(graphics, priceBox, priceLabel());
        drawFieldLabel(graphics, attemptsBox, attemptsLabel());
        Component tip = hoveredTip(mouseX, mouseY);
        if (tip != null) {
            graphics.setTooltipForNextFrame(this.font, tip, mouseX, mouseY);
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        graphics.text(this.font, label, boxX(box), boxY(box) - 12, 0xFFFFFFFF, true);
    }
    //?}

    private Component hoveredTip(int mouseX, int mouseY) {
        if (overField(priceBox, mouseX, mouseY)) {
            return priceTooltip();
        }
        if (overField(attemptsBox, mouseX, mouseY)) {
            return attemptsTooltip();
        }
        return null;
    }

    private boolean overField(EditBox box, int mouseX, int mouseY) {
        if (box == null) {
            return false;
        }
        int left = boxX(box);
        int top = boxY(box) - 12;
        return mouseX >= left && mouseX < left + boxW(box) && mouseY >= top && mouseY < boxY(box) + boxH(box);
    }

    private int boxX(EditBox box) {
        //? if <1.19.3 {
        return box.x;
        //?}
        //? if >=1.19.3 {
        return box.getX();
        //?}
    }

    private int boxY(EditBox box) {
        //? if <1.19.3 {
        return box.y;
        //?}
        //? if >=1.19.3 {
        return box.getY();
        //?}
    }

    private static int boxW(EditBox box) {
        return box.getWidth();
    }

    private static int boxH(EditBox box) {
        return box.getHeight();
    }
}
*///?}
