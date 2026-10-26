package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.text.ScrollingNames;
import dev.quarkos.villagerreroller.text.TradeNameLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
//? if <1.21.11 {
/*import net.minecraft.Util;
*///?} else {
import net.minecraft.util.Util;
//?}
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Book button and trade list added to the vanilla merchant screen.
 * The button uses the recipe-book sprite. Rows use the villager scroller and checkbox sprites.
 */
public final class TradePickerUi {
    private static final int MERCHANT_WIDTH = 276;
    private static final int MERCHANT_HEIGHT = 166;
    private static final int PANEL_WIDTH = 168;
    private static final int PANEL_HEIGHT = 208;
    private static final int LIST_TOP = 42;
    private static final int LIST_HEIGHT = 80;
    private static final int LIST_WIDTH = PANEL_WIDTH - 12;
    private static final int STATUS_TOP = 126;
    private static final int METHOD_TOP = 140;
    private static final int QUEUE_TOP = 162;
    private static final int START_TOP = 184;
    private static final int ROW_HEIGHT = 20;
    private static final int TEXT = 0xFF404040;
    private static final int CURSE = 0xFFAA0000;
    //? if <1.21 {
    /*private static Identifier sprite(String path) {
        return new Identifier("minecraft", path);
    }
    *///?} else {
    private static Identifier sprite(String path) {
        return Identifier.withDefaultNamespace(path);
    }
    //?}
    private static final Identifier CHECKBOX = sprite("widget/checkbox");
    private static final Identifier CHECKBOX_SELECTED = sprite("widget/checkbox_selected");
    private static final Identifier SCROLLER = sprite("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED = sprite("container/villager/scroller_disabled");

    private static final List<AbstractWidget> panelWidgets = new ArrayList<>();
    private static OfferList list;
    private static Button methodButton;
    private static Button queueButton;
    private static Button hallButton;

    private TradePickerUi() {
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        panelWidgets.clear();
        list = null;
        methodButton = null;
        queueButton = null;
        hallButton = null;
        LibrarianTracker.bindNearest(client);
        int left = (screen.width - MERCHANT_WIDTH) / 2;
        int top = (screen.height - MERCHANT_HEIGHT) / 2;
        //? if <1.20.2 {
        /*dev.quarkos.villagerreroller.client.McBridge.buttons(screen).add(new ImageButton(
                left + MERCHANT_WIDTH - 24,
                top + 4,
                20,
                18,
                0,
                0,
                19,
                new Identifier("minecraft", "textures/gui/recipe_button.png"),
                256,
                256,
                button -> toggle(client, screen),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.offers")
        ));
        *///?} else {
        dev.quarkos.villagerreroller.client.McBridge.buttons(screen).add(new ImageButton(
                left + MERCHANT_WIDTH - 24,
                top + 4,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> toggle(client, screen),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.offers")
        ));
        //?}
        if (TradeSession.panelOpen() || JobSiteReroller.isRunning() || TradeHall.isActive()) {
            TradeSession.setPanelOpen(true);
            open(client, screen);
        }
    }

    private static void toggle(Minecraft client, MerchantScreen screen) {
        if (TradeSession.panelOpen()) {
            close(screen);
            TradeSession.setPanelOpen(false);
        } else {
            TradeSession.setPanelOpen(true);
            open(client, screen);
        }
    }

    private static void open(Minecraft client, MerchantScreen screen) {
        close(screen);
        Villager villager = LibrarianTracker.current(client);
        if (villager != null && client.level != null) {
            TradeSession.rebuild(client.level, villager, screen.getMenu());
        }
        int left = (screen.width - MERCHANT_WIDTH) / 2;
        int top = (screen.height - MERCHANT_HEIGHT) / 2;
        int panelX = left + MERCHANT_WIDTH + 4;
        int panelY = top;
        if (panelX + PANEL_WIDTH > screen.width - 2) {
            panelX = Math.max(2, screen.width - PANEL_WIDTH - 2);
        }
        int listX = panelX + 6;
        int listY = panelY + LIST_TOP;
        int listW = LIST_WIDTH;
        int listH = LIST_HEIGHT;

        var buttons = dev.quarkos.villagerreroller.client.McBridge.buttons(screen);
        PanelBackground background = new PanelBackground(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, screen.getMenu().getTraderLevel());
        buttons.add(background);
        panelWidgets.add(background);

        EditBox search = new EditBox(client.font, panelX + 6, panelY + 20, PANEL_WIDTH - 12, 16, Component.translatable("gui.recipebook.search_hint"));
        //? if >=1.19.3 {
        search.setHint(Component.translatable("gui.recipebook.search_hint"));
        //?}
        search.setValue(TradeSession.filter());
        search.setResponder(value -> {
            TradeSession.setFilter(value);
            if (list != null) {
                list.setScroll(0);
            }
        });
        buttons.add(search);
        panelWidgets.add(search);

        int half = (PANEL_WIDTH - 18) / 2;
        //? if <1.19.3 {
        /*queueButton = new Button(panelX + 6, panelY + QUEUE_TOP, half, 20, Component.translatable("text." + VillagerReroller.MOD_ID + ".button.queue"), pressed -> TradeHall.queueCurrent(client));
        hallButton = new Button(panelX + 12 + half, panelY + QUEUE_TOP, half, 20, Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall", TradeHall.size()), pressed -> {
            if (!TradeHall.isActive()) {
                dev.quarkos.villagerreroller.client.McBridge.setScreen(client, new TradeHallScreen());
            }
        });
        Button action = new Button(panelX + 6, panelY + START_TOP, PANEL_WIDTH - 12, 20, actionLabel(), button -> onAction(client));
        *///?}
        //? if >=1.19.3 {
        queueButton = Button.builder(Component.translatable("text." + VillagerReroller.MOD_ID + ".button.queue"), pressed -> TradeHall.queueCurrent(client))
                .bounds(panelX + 6, panelY + QUEUE_TOP, half, 20)
                .build();
        hallButton = Button.builder(Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall", TradeHall.size()), pressed -> {
            if (!TradeHall.isActive()) {
                dev.quarkos.villagerreroller.client.McBridge.setScreen(client, new TradeHallScreen());
            }
        }).bounds(panelX + 12 + half, panelY + QUEUE_TOP, half, 20).build();
        Button action = Button.builder(actionLabel(), button -> onAction(client))
                .bounds(panelX + 6, panelY + START_TOP, PANEL_WIDTH - 12, 20)
                .build();
        //?}
        buttons.add(queueButton);
        panelWidgets.add(queueButton);
        buttons.add(hallButton);
        panelWidgets.add(hallButton);
        buttons.add(action);
        panelWidgets.add(action);

        if (RerollMethod.canChoose()) {
            int buttonWidth = Math.max(72, client.font.width(Component.translatable("text." + VillagerReroller.MOD_ID + ".method.trade_cycling").getString()) + 8);
            //? if <1.19.3 {
            /*methodButton = new Button(panelX + PANEL_WIDTH - 6 - buttonWidth, panelY + METHOD_TOP, buttonWidth, 20, RerollMethod.label(), pressed -> {
                if (JobSiteReroller.isRunning() || TradeHall.isActive()) {
                    return;
                }
                RerollMethod.toggle();
                pressed.setMessage(RerollMethod.label());
            });
            *///?}
            //? if >=1.19.3 {
            methodButton = Button.builder(RerollMethod.label(), pressed -> {
                if (JobSiteReroller.isRunning() || TradeHall.isActive()) {
                    return;
                }
                RerollMethod.toggle();
                pressed.setMessage(RerollMethod.label());
            }).bounds(panelX + PANEL_WIDTH - 6 - buttonWidth, panelY + METHOD_TOP, buttonWidth, 20).build();
            //?}
            buttons.add(methodButton);
            panelWidgets.add(methodButton);
        }

        list = new OfferList(listX, listY, listW, listH, action);
        buttons.add(list);
        panelWidgets.add(list);
    }

    private static void close(MerchantScreen screen) {
        var buttons = dev.quarkos.villagerreroller.client.McBridge.buttons(screen);
        for (AbstractWidget widget : panelWidgets) {
            buttons.remove(widget);
        }
        panelWidgets.clear();
        list = null;
        methodButton = null;
        queueButton = null;
        hallButton = null;
    }

    /**
     * Pixels available for a row name once the price column is reserved.
     * The gametest uses the same budget when it waits for a mid-scroll frame.
     */
    public static int nameBudget(int priceWidth) {
        return TradeNameLayout.availableWidth(LIST_WIDTH, priceWidth);
    }

    private static void onAction(Minecraft client) {
        if (TradeHall.isActive()) {
            TradeHall.requestStop();
            return;
        }
        if (JobSiteReroller.isRunning()) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            return;
        }
        JobSiteReroller.start(client);
    }

    private static void refreshQueueButtons() {
        if (queueButton != null) {
            queueButton.active = !JobSiteReroller.isRunning() && !TradeHall.isActive();
            queueButton.setMessage(Component.translatable("text." + VillagerReroller.MOD_ID + ".button.queue"));
        }
        if (hallButton != null) {
            hallButton.active = !TradeHall.isActive();
            hallButton.setMessage(Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall", TradeHall.size()));
        }
    }

    private static Component actionLabel() {
        String key = JobSiteReroller.isRunning() || TradeHall.isActive() ? "button.stop" : "button.start";
        return Component.translatable("text." + VillagerReroller.MOD_ID + "." + key);
    }

    private static Component statusLine() {
        if (!TradeHall.isActive()) {
            return JobSiteReroller.status();
        }
        Component hall = TradeHall.hudTitle();
        Component reroll = JobSiteReroller.status();
        if (reroll == null || reroll.getString().isEmpty()) {
            return hall;
        }
        return hall.copy().append(" · ").append(reroll);
    }

    private static final class PanelBackground extends AbstractWidget {
        private final int villagerLevel;

        private PanelBackground(int x, int y, int width, int height, int villagerLevel) {
            super(x, y, width, height, Component.empty());
            this.villagerLevel = Math.max(1, Math.min(5, villagerLevel));
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }

        private int left() {
            //? if <1.19.3 {
            /*return x;
            *///?}
            //? if >=1.19.3 {
            return getX();
            //?}
        }

        private int top() {
            //? if <1.19.3 {
            /*return y;
            *///?}
            //? if >=1.19.3 {
            return getY();
            //?}
        }

        //? if <1.19.4 {
        /*@Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            fill(pose, left(), top(), left() + width, top() + height, 0xFF000000);
            fill(pose, left() + 2, top() + 2, left() + width - 2, top() + height - 2, 0xFFC6C6C6);
            fill(pose, left() + 2, top() + 2, left() + width - 2, top() + 3, 0xFFFFFFFF);
            fill(pose, left() + 2, top() + 2, left() + 3, top() + height - 2, 0xFFFFFFFF);
            fill(pose, left() + 3, top() + height - 3, left() + width - 2, top() + height - 2, 0xFF555555);
            fill(pose, left() + width - 3, top() + 3, left() + width - 2, top() + height - 2, 0xFF555555);
            Minecraft minecraft = Minecraft.getInstance();
            Component title = Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".panel.title",
                    Component.translatable("merchant.level." + villagerLevel)
            );
            minecraft.font.draw(pose, title, left() + 6, top() + 6, TEXT);
            Component status = statusLine();
            if (status != null && !status.getString().isEmpty()) {
                String line = minecraft.font.plainSubstrByWidth(status.getString(), width - 12);
                minecraft.font.draw(pose, line, left() + 6, top() + STATUS_TOP, TEXT);
            }
            minecraft.font.draw(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"), left() + 6, top() + METHOD_TOP + 6, TEXT);
            if (methodButton == null) {
                minecraft.font.draw(pose, RerollMethod.label(), left() + 48, top() + METHOD_TOP + 6, TEXT);
            }
        }
        *///?}
        //? if >=1.19.4 && <1.20 {
        /*@Override
        public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            fill(pose, getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            fill(pose, getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFFC6C6C6);
            fill(pose, getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, 0xFFFFFFFF);
            fill(pose, getX() + 2, getY() + 2, getX() + 3, getY() + height - 2, 0xFFFFFFFF);
            fill(pose, getX() + 3, getY() + height - 3, getX() + width - 2, getY() + height - 2, 0xFF555555);
            fill(pose, getX() + width - 3, getY() + 3, getX() + width - 2, getY() + height - 2, 0xFF555555);
            Minecraft minecraft = Minecraft.getInstance();
            Component title = Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".panel.title",
                    Component.translatable("merchant.level." + villagerLevel)
            );
            minecraft.font.draw(pose, title, getX() + 6, getY() + 6, TEXT);
            Component status = statusLine();
            if (status != null && !status.getString().isEmpty()) {
                String line = minecraft.font.plainSubstrByWidth(status.getString(), width - 12);
                minecraft.font.draw(pose, line, getX() + 6, getY() + STATUS_TOP, TEXT);
            }
            minecraft.font.draw(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"), getX() + 6, getY() + METHOD_TOP + 6, TEXT);
            if (methodButton == null) {
                minecraft.font.draw(pose, RerollMethod.label(), getX() + 48, getY() + METHOD_TOP + 6, TEXT);
            }
        }
        *///?}
        //? if >=1.20 {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFFC6C6C6);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, 0xFFFFFFFF);
            graphics.fill(getX() + 2, getY() + 2, getX() + 3, getY() + height - 2, 0xFFFFFFFF);
            graphics.fill(getX() + 3, getY() + height - 3, getX() + width - 2, getY() + height - 2, 0xFF555555);
            graphics.fill(getX() + width - 3, getY() + 3, getX() + width - 2, getY() + height - 2, 0xFF555555);
            Minecraft minecraft = Minecraft.getInstance();
            Component title = Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".panel.title",
                    Component.translatable("merchant.level." + villagerLevel)
            );
            //? if >=26.1 {
            /*graphics.text(minecraft.font, title, getX() + 6, getY() + 6, TEXT, false);
            *///?} else {
            graphics.drawString(minecraft.font, title, getX() + 6, getY() + 6, TEXT, false);
            //?}
            Component status = statusLine();
            if (status != null && !status.getString().isEmpty()) {
                String line = minecraft.font.plainSubstrByWidth(status.getString(), width - 12);
                //? if >=26.1 {
                /*graphics.text(minecraft.font, line, getX() + 6, getY() + STATUS_TOP, TEXT, false);
                *///?} else {
                graphics.drawString(minecraft.font, line, getX() + 6, getY() + STATUS_TOP, TEXT, false);
                //?}
            }
            //? if >=26.1 {
            /*graphics.text(minecraft.font, Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"), getX() + 6, getY() + METHOD_TOP + 6, TEXT, false);
            *///?} else {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"),
                    getX() + 6,
                    getY() + METHOD_TOP + 6,
                    TEXT,
                    false
            );
            //?}
            if (methodButton == null) {
                //? if >=26.1 {
                /*graphics.text(minecraft.font, RerollMethod.label(), getX() + 48, getY() + METHOD_TOP + 6, TEXT, false);
                *///?} else {
                graphics.drawString(minecraft.font, RerollMethod.label(), getX() + 48, getY() + METHOD_TOP + 6, TEXT, false);
                //?}
            }
        }
        //?}

        //? if <1.19.3 {
        /*@Override
        public void updateNarration(NarrationElementOutput output) {
        }
        *///?}
        //? if >=1.19.3 {
        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
        //?}
    }

    private static final class OfferList extends AbstractWidget {
        private final Button action;
        private int scroll;

        private OfferList(int x, int y, int width, int height, Button action) {
            super(x, y, width, height, Component.translatable("text." + VillagerReroller.MOD_ID + ".panel.title", ""));
            this.action = action;
        }

        private void setScroll(int scroll) {
            this.scroll = Math.max(0, scroll);
        }

        private int left() {
            //? if <1.19.3 {
            /*return x;
            *///?}
            //? if >=1.19.3 {
            return getX();
            //?}
        }

        private int top() {
            //? if <1.19.3 {
            /*return y;
            *///?}
            //? if >=1.19.3 {
            return getY();
            //?}
        }

        //? if <1.20 {
        /*private void drawName(PoseStack pose, Font font, String text, int color, int left, int right, int rowTop, int rowBottom) {
            int available = right - left;
            if (available <= 0) {
                return;
            }
            int textWidth = font.width(text);
            int top = rowTop + 6;
            if (textWidth <= available) {
                font.draw(pose, text, left, top, color);
                return;
            }
            int offset = ScrollingNames.offset(textWidth, available, Util.getMillis());
            //? if <1.19.1 {
            clip(left, rowTop, right, rowBottom);
            //?}
            //? if >=1.19.1 {
            enableScissor(left, rowTop, right, rowBottom);
            //?}
            font.draw(pose, text, left - offset, top, color);
            //? if <1.19.1 {
            unclip();
            //?}
            //? if >=1.19.1 {
            disableScissor();
            //?}
        }

        //? if <1.19.1 {
        private void clip(int x1, int y1, int x2, int y2) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int windowHeight = Minecraft.getInstance().getWindow().getHeight();
            com.mojang.blaze3d.systems.RenderSystem.enableScissor(
                    (int) (x1 * scale),
                    (int) (windowHeight - y2 * scale),
                    (int) ((x2 - x1) * scale),
                    (int) ((y2 - y1) * scale));
        }

        private void unclip() {
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        }
        //?}

        //? if <1.19.4 {
        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
            refreshQueueButtons();
            if (methodButton != null) {
                methodButton.active = !JobSiteReroller.isRunning() && !TradeHall.isActive();
                methodButton.setMessage(RerollMethod.label());
            }
            List<TradeEntry> rows = TradeSession.visible();
            int visibleRows = Math.max(1, height / ROW_HEIGHT);
            int maxScroll = Math.max(0, rows.size() - visibleRows);
            scroll = Mth.clamp(scroll, 0, maxScroll);
            Minecraft minecraft = Minecraft.getInstance();
            //? if <1.19.1 {
            clip(left(), top(), left() + width - 8, top() + height);
            //?}
            //? if >=1.19.1 {
            enableScissor(left(), top(), left() + width - 8, top() + height);
            //?}
            for (int i = 0; i < visibleRows && scroll + i < rows.size(); i++) {
                TradeEntry entry = rows.get(scroll + i);
                int rowY = top() + i * ROW_HEIGHT;
                boolean selected = TradeSession.selected(entry.id);
                if (selected) {
                    fill(pose, left(), rowY, left() + width - 10, rowY + ROW_HEIGHT - 1, 0xFFB0B0B0);
                }
                fill(pose, left() + 1, rowY + 4, left() + 13, rowY + 16, 0xFF000000);
                fill(pose, left() + 2, rowY + 5, left() + 12, rowY + 15, selected ? 0xFF55FF55 : 0xFF8B8B8B);
                ItemStack icon = entry.icon;
                minecraft.getItemRenderer().renderGuiItem(icon, left() + 16, rowY + 1);
                minecraft.getItemRenderer().renderGuiItemDecorations(minecraft.font, icon, left() + 16, rowY + 1);
                String price = selected && entry.paysEmeralds
                        ? (TradeSession.anyPrice(entry.id) ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString() : "\u2264" + TradeSession.chosenMax(entry.id))
                        : entry.priceLabel();
                int priceWidth = minecraft.font.width(price);
                int priceX = TradeNameLayout.priceX(left(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(left());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(pose, minecraft.font, entry.name, entry.curse ? CURSE : TEXT, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                minecraft.font.draw(pose, price, priceX, rowY + 6, TEXT);
            }
            //? if <1.19.1 {
            unclip();
            //?}
            //? if >=1.19.1 {
            disableScissor();
            //?}
            int travel = Math.max(1, height - 27);
            int scrollerY = top() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            fill(pose, left() + width - 6, scrollerY, left() + width, scrollerY + 27, maxScroll == 0 ? 0xFF555555 : 0xFFC6C6C6);
        }
        //?}
        //? if >=1.19.4 {
        @Override
        public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
            refreshQueueButtons();
            if (methodButton != null) {
                methodButton.active = !JobSiteReroller.isRunning() && !TradeHall.isActive();
                methodButton.setMessage(RerollMethod.label());
            }
            List<TradeEntry> rows = TradeSession.visible();
            int visibleRows = Math.max(1, height / ROW_HEIGHT);
            int maxScroll = Math.max(0, rows.size() - visibleRows);
            scroll = Mth.clamp(scroll, 0, maxScroll);
            Minecraft minecraft = Minecraft.getInstance();
            enableScissor(getX(), getY(), getX() + width - 8, getY() + height);
            for (int i = 0; i < visibleRows && scroll + i < rows.size(); i++) {
                TradeEntry entry = rows.get(scroll + i);
                int rowY = getY() + i * ROW_HEIGHT;
                boolean selected = TradeSession.selected(entry.id);
                if (selected) {
                    fill(pose, getX(), rowY, getX() + width - 10, rowY + ROW_HEIGHT - 1, 0xFFB0B0B0);
                }
                fill(pose, getX() + 1, rowY + 4, getX() + 13, rowY + 16, 0xFF000000);
                fill(pose, getX() + 2, rowY + 5, getX() + 12, rowY + 15, selected ? 0xFF55FF55 : 0xFF8B8B8B);
                ItemStack icon = entry.icon;
                minecraft.getItemRenderer().renderGuiItem(pose, icon, getX() + 16, rowY + 1);
                minecraft.getItemRenderer().renderGuiItemDecorations(pose, minecraft.font, icon, getX() + 16, rowY + 1);
                String price = selected && entry.paysEmeralds
                        ? (TradeSession.anyPrice(entry.id) ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString() : "\u2264" + TradeSession.chosenMax(entry.id))
                        : entry.priceLabel();
                int priceWidth = minecraft.font.width(price);
                int priceX = TradeNameLayout.priceX(getX(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(getX());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(pose, minecraft.font, entry.name, entry.curse ? CURSE : TEXT, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                minecraft.font.draw(pose, price, priceX, rowY + 6, TEXT);
            }
            disableScissor();
            int travel = Math.max(1, height - 27);
            int scrollerY = getY() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            fill(pose, getX() + width - 6, scrollerY, getX() + width, scrollerY + 27, maxScroll == 0 ? 0xFF555555 : 0xFFC6C6C6);
        }
        //?}
        *///?}
        //? if >=1.20 {
        private static void drawName(GuiGraphics graphics, Font font, String text, int color, int left, int right, int rowTop, int rowBottom) {
            int available = right - left;
            if (available <= 0) {
                return;
            }
            int textWidth = font.width(text);
            int top = rowTop + 6;
            if (textWidth <= available) {
                //? if >=26.1 {
                /*graphics.text(font, text, left, top, color, false);
                *///?} else {
                graphics.drawString(font, text, left, top, color, false);
                //?}
                return;
            }
            int offset = ScrollingNames.offset(textWidth, available, Util.getMillis());
            // right is exclusive and already stops PRICE_GAP pixels before the price.
            graphics.enableScissor(left, rowTop, right, rowBottom);
            //? if >=26.1 {
            /*graphics.text(font, text, left - offset, top, color, false);
            *///?} else {
            graphics.drawString(font, text, left - offset, top, color, false);
            //?}
            graphics.disableScissor();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
            refreshQueueButtons();
            if (methodButton != null) {
                methodButton.active = !JobSiteReroller.isRunning() && !TradeHall.isActive();
                methodButton.setMessage(RerollMethod.label());
            }
            List<TradeEntry> rows = TradeSession.visible();
            int visibleRows = Math.max(1, height / ROW_HEIGHT);
            int maxScroll = Math.max(0, rows.size() - visibleRows);
            scroll = Mth.clamp(scroll, 0, maxScroll);
            Minecraft minecraft = Minecraft.getInstance();
            graphics.enableScissor(getX(), getY(), getX() + width - 8, getY() + height);
            for (int i = 0; i < visibleRows && scroll + i < rows.size(); i++) {
                TradeEntry entry = rows.get(scroll + i);
                int rowY = getY() + i * ROW_HEIGHT;
                boolean selected = TradeSession.selected(entry.id);
                if (selected) {
                    graphics.fill(getX(), rowY, getX() + width - 10, rowY + ROW_HEIGHT - 1, 0xFFB0B0B0);
                }
                Identifier checkbox = selected ? CHECKBOX_SELECTED : CHECKBOX;
                //? if >=1.21.6 {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, checkbox, getX() + 1, rowY + 4, 12, 12);
                //?}
                //? if >=1.21.2 && <1.21.6 {
                /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, checkbox, getX() + 1, rowY + 4, 12, 12);
                *///?}
                //? if >=1.20.2 && <1.21.2 {
                /*graphics.blitSprite(checkbox, getX() + 1, rowY + 4, 12, 12);
                *///?}
                //? if <1.20.2 {
                /*graphics.fill(getX() + 1, rowY + 4, getX() + 13, rowY + 16, 0xFF000000);
                graphics.fill(getX() + 2, rowY + 5, getX() + 12, rowY + 15, selected ? 0xFF55FF55 : 0xFF8B8B8B);
                *///?}
                ItemStack icon = entry.icon;
                //? if >=26.1 {
                /*graphics.item(icon, getX() + 16, rowY + 1);
                graphics.itemDecorations(minecraft.font, icon, getX() + 16, rowY + 1);
                *///?} else {
                graphics.renderItem(icon, getX() + 16, rowY + 1);
                graphics.renderItemDecorations(minecraft.font, icon, getX() + 16, rowY + 1);
                //?}
                String price = selected && entry.paysEmeralds
                        ? (TradeSession.anyPrice(entry.id) ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString() : "\u2264" + TradeSession.chosenMax(entry.id))
                        : entry.priceLabel();
                int priceWidth = minecraft.font.width(price);
                int priceX = TradeNameLayout.priceX(getX(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(getX());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(graphics, minecraft.font, entry.name, entry.curse ? CURSE : TEXT, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                //? if >=26.1 {
                /*graphics.text(minecraft.font, price, priceX, rowY + 6, TEXT, false);
                *///?} else {
                graphics.drawString(minecraft.font, price, priceX, rowY + 6, TEXT, false);
                //?}
            }
            graphics.disableScissor();
            Identifier scroller = maxScroll == 0 ? SCROLLER_DISABLED : SCROLLER;
            int travel = Math.max(1, height - 27);
            int scrollerY = getY() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            //? if >=1.21.6 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scroller, getX() + width - 6, scrollerY, 6, 27);
            //?}
            //? if >=1.21.2 && <1.21.6 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, scroller, getX() + width - 6, scrollerY, 6, 27);
            *///?}
            //? if >=1.20.2 && <1.21.2 {
            /*graphics.blitSprite(scroller, getX() + width - 6, scrollerY, 6, 27);
            *///?}
            //? if <1.20.2 {
            /*graphics.fill(getX() + width - 6, scrollerY, getX() + width, scrollerY + 27, maxScroll == 0 ? 0xFF555555 : 0xFFC6C6C6);
            *///?}
        }
        //?}

        //? if <1.21.9 {
        /*@Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return clickRow(mouseX, mouseY, button);
        }
        *///?} else {
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            return clickRow(event.x(), event.y(), event.button());
        }
        //?}

        private boolean clickRow(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            List<TradeEntry> rows = TradeSession.visible();
            int index = scroll + (int) ((mouseY - top()) / ROW_HEIGHT);
            if (index < 0 || index >= rows.size()) {
                return true;
            }
            TradeEntry entry = rows.get(index);
            int priceColumn = left() + width - 52;
            if (mouseX >= priceColumn) {
                TradeSession.cyclePrice(entry, button == 1 ? -1 : 1);
            } else {
                TradeSession.toggle(entry);
            }
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        //? if <1.20.2 {
        /*@Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return scrollBy(mouseX, mouseY, delta);
        }
        *///?} else {
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            return scrollBy(mouseX, mouseY, vertical);
        }
        //?}

        private boolean scrollBy(double mouseX, double mouseY, double vertical) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            int visibleRows = Math.max(1, height / ROW_HEIGHT);
            int maxScroll = Math.max(0, TradeSession.visible().size() - visibleRows);
            scroll = Mth.clamp(scroll - (int) Math.signum(vertical), 0, maxScroll);
            return true;
        }

        //? if <1.19.3 {
        /*@Override
        public void updateNarration(NarrationElementOutput output) {
        }
        *///?}
        //? if >=1.19.3 {
        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
        //?}
    }
}
