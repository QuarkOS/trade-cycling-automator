package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.text.ScrollingNames;
import dev.quarkos.villagerreroller.text.TradeNameLayout;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
/*import net.minecraft.world.entity.npc.Villager;
*///?} else {
import net.minecraft.world.entity.npc.villager.Villager;
//?}
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
    private static final int PANEL_HEIGHT = 188;
    private static final int LIST_TOP = 42;
    private static final int LIST_HEIGHT = 80;
    private static final int LIST_WIDTH = PANEL_WIDTH - 12;
    private static final int STATUS_TOP = 126;
    private static final int METHOD_TOP = 140;
    private static final int START_TOP = 164;
    private static final int ROW_HEIGHT = 20;
    private static final int TEXT = 0xFF404040;
    private static final int CURSE = 0xFFAA0000;
    private static final Identifier CHECKBOX = Identifier.withDefaultNamespace("widget/checkbox");
    private static final Identifier CHECKBOX_SELECTED = Identifier.withDefaultNamespace("widget/checkbox_selected");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/villager/scroller_disabled");

    private static final List<AbstractWidget> panelWidgets = new ArrayList<>();
    private static OfferList list;
    private static Button methodButton;

    private TradePickerUi() {
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        panelWidgets.clear();
        list = null;
        methodButton = null;
        LibrarianTracker.bindNearest(client);
        int left = (screen.width - MERCHANT_WIDTH) / 2;
        int top = (screen.height - MERCHANT_HEIGHT) / 2;
        addWidget(screen, new ImageButton(
                left + MERCHANT_WIDTH - 24,
                top + 4,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> toggle(client, screen),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.offers")
        ));
        if (TradeSession.panelOpen() || JobSiteReroller.isRunning()) {
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

        PanelBackground background = new PanelBackground(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, screen.getMenu().getTraderLevel());
        addWidget(screen, background);
        panelWidgets.add(background);

        EditBox search = new EditBox(client.font, panelX + 6, panelY + 20, PANEL_WIDTH - 12, 16, Component.translatable("gui.recipebook.search_hint"));
        search.setHint(Component.translatable("gui.recipebook.search_hint"));
        search.setValue(TradeSession.filter());
        search.setResponder(value -> {
            TradeSession.setFilter(value);
            if (list != null) {
                list.setScroll(0);
            }
        });
        addWidget(screen, search);
        panelWidgets.add(search);

        Button action = Button.builder(actionLabel(), button -> {
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            } else {
                JobSiteReroller.start(client);
            }
            button.setMessage(actionLabel());
        }).bounds(panelX + 6, panelY + START_TOP, PANEL_WIDTH - 12, 20).build();
        addWidget(screen, action);
        panelWidgets.add(action);

        if (RerollMethod.canChoose()) {
            int buttonWidth = Math.max(72, client.font.width(Component.translatable("text." + VillagerReroller.MOD_ID + ".method.trade_cycling").getString()) + 8);
            methodButton = Button.builder(RerollMethod.label(), pressed -> {
                if (JobSiteReroller.isRunning()) {
                    return;
                }
                RerollMethod.toggle();
                pressed.setMessage(RerollMethod.label());
            }).bounds(panelX + PANEL_WIDTH - 6 - buttonWidth, panelY + METHOD_TOP, buttonWidth, 20).build();
            addWidget(screen, methodButton);
            panelWidgets.add(methodButton);
        }

        list = new OfferList(listX, listY, listW, listH, action);
        addWidget(screen, list);
        panelWidgets.add(list);
    }

    /**
     * 1.21.11 keeps Fabric's button list. Older Trade Cycling builds add their
     * cycle button to the renderable and child lists only, which makes that
     * button list throw when the panel inserts the next widget.
     */
    private static void addWidget(MerchantScreen screen, AbstractWidget widget) {
        //? if >=1.21.11 {
        Screens.getButtons(screen).add(widget);
        //?} else {
        /*screen.addRenderableWidget(widget);
        *///?}
    }

    private static void removePanelWidget(MerchantScreen screen, AbstractWidget widget) {
        //? if >=1.21.11 {
        Screens.getButtons(screen).remove(widget);
        //?} else {
        /*screen.removeWidget(widget);
        *///?}
    }

    private static void close(MerchantScreen screen) {
        for (AbstractWidget widget : panelWidgets) {
            removePanelWidget(screen, widget);
        }
        panelWidgets.clear();
        list = null;
        methodButton = null;
    }

    /**
     * Pixels available for a row name once the price column is reserved.
     * The gametest uses the same budget when it waits for a mid-scroll frame.
     */
    public static int nameBudget(int priceWidth) {
        return TradeNameLayout.availableWidth(LIST_WIDTH, priceWidth);
    }

    private static Component actionLabel() {
        String key = JobSiteReroller.isRunning() ? "button.stop" : "button.start";
        return Component.translatable("text." + VillagerReroller.MOD_ID + "." + key);
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
            graphics.drawString(minecraft.font, title, getX() + 6, getY() + 6, TEXT, false);
            Component status = JobSiteReroller.status();
            if (status != null && !status.getString().isEmpty()) {
                String line = minecraft.font.plainSubstrByWidth(status.getString(), width - 12);
                graphics.drawString(minecraft.font, line, getX() + 6, getY() + STATUS_TOP, TEXT, false);
            }
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"),
                    getX() + 6,
                    getY() + METHOD_TOP + 6,
                    TEXT,
                    false
            );
            if (methodButton == null) {
                graphics.drawString(minecraft.font, RerollMethod.label(), getX() + 48, getY() + METHOD_TOP + 6, TEXT, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
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

        private static void drawName(GuiGraphics graphics, Font font, String text, int color, int left, int right, int rowTop, int rowBottom) {
            int available = right - left;
            if (available <= 0) {
                return;
            }
            int textWidth = font.width(text);
            int top = rowTop + 6;
            if (textWidth <= available) {
                graphics.drawString(font, text, left, top, color, false);
                return;
            }
            //? if >=1.21.11 {
            int offset = ScrollingNames.offset(textWidth, available, net.minecraft.util.Util.getMillis());
            //?} else {
            /*int offset = ScrollingNames.offset(textWidth, available, net.minecraft.Util.getMillis());
            *///?}
            // right is exclusive and already stops PRICE_GAP pixels before the price.
            graphics.enableScissor(left, rowTop, right, rowBottom);
            graphics.drawString(font, text, left - offset, top, color, false);
            graphics.disableScissor();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
            if (methodButton != null) {
                methodButton.active = !JobSiteReroller.isRunning();
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
                //?} else {
                /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, checkbox, getX() + 1, rowY + 4, 12, 12);
                *///?}
                ItemStack icon = entry.icon;
                graphics.renderItem(icon, getX() + 16, rowY + 1);
                graphics.renderItemDecorations(minecraft.font, icon, getX() + 16, rowY + 1);
                String price = selected && entry.paysEmeralds
                        ? (TradeSession.anyPrice(entry.id) ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString() : "\u2264" + TradeSession.chosenMax(entry.id))
                        : entry.priceLabel();
                int priceWidth = minecraft.font.width(price);
                int priceX = TradeNameLayout.priceX(getX(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(getX());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(graphics, minecraft.font, entry.name, entry.curse ? CURSE : TEXT, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                graphics.drawString(minecraft.font, price, priceX, rowY + 6, TEXT, false);
            }
            graphics.disableScissor();
            Identifier scroller = maxScroll == 0 ? SCROLLER_DISABLED : SCROLLER;
            int travel = Math.max(1, height - 27);
            int scrollerY = getY() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            //? if >=1.21.6 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scroller, getX() + width - 6, scrollerY, 6, 27);
            //?} else {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, scroller, getX() + width - 6, scrollerY, 6, 27);
            *///?}
        }

        @Override
        //? if >=1.21.9 {
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            double mouseX = event.x();
            double mouseY = event.y();
            int button = event.button();
        //?} else {
        /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
        *///?}
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            List<TradeEntry> rows = TradeSession.visible();
            int index = scroll + (int) ((mouseY - getY()) / ROW_HEIGHT);
            if (index < 0 || index >= rows.size()) {
                return true;
            }
            TradeEntry entry = rows.get(index);
            int priceColumn = getX() + width - 52;
            if (mouseX >= priceColumn) {
                TradeSession.cyclePrice(entry, button == 1 ? -1 : 1);
            } else {
                TradeSession.toggle(entry);
            }
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            int visibleRows = Math.max(1, height / ROW_HEIGHT);
            int maxScroll = Math.max(0, TradeSession.visible().size() - visibleRows);
            scroll = Mth.clamp(scroll - (int) Math.signum(vertical), 0, maxScroll);
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
