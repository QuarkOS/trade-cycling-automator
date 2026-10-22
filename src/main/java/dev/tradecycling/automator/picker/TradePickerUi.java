package dev.tradecycling.automator.picker;

import dev.tradecycling.automator.TradeCyclingAutomator;
import dev.tradecycling.automator.client.LibrarianTracker;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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
    private static final int PANEL_HEIGHT = 176;
    private static final int ROW_HEIGHT = 20;
    private static final int TEXT = 0xFF404040;
    private static final int CURSE = 0xFFAA0000;
    private static final Identifier CHECKBOX = Identifier.withDefaultNamespace("widget/checkbox");
    private static final Identifier CHECKBOX_SELECTED = Identifier.withDefaultNamespace("widget/checkbox_selected");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/villager/scroller_disabled");

    private static final List<AbstractWidget> panelWidgets = new ArrayList<>();
    private static OfferList list;

    private TradePickerUi() {
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        panelWidgets.clear();
        list = null;
        LibrarianTracker.bindNearest(client);
        int left = (screen.width - MERCHANT_WIDTH) / 2;
        int top = (screen.height - MERCHANT_HEIGHT) / 2;
        Screens.getButtons(screen).add(new ImageButton(
                left + MERCHANT_WIDTH - 24,
                top + 4,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> toggle(client, screen),
                Component.translatable("text." + TradeCyclingAutomator.MOD_ID + ".button.offers")
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
        int listY = panelY + 42;
        int listW = PANEL_WIDTH - 12;
        int listH = 90;

        var buttons = Screens.getButtons(screen);
        PanelBackground background = new PanelBackground(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, screen.getMenu().getTraderLevel());
        buttons.add(background);
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
        buttons.add(search);
        panelWidgets.add(search);

        Button action = Button.builder(actionLabel(), button -> {
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            } else {
                JobSiteReroller.start(client);
            }
            button.setMessage(actionLabel());
        }).bounds(panelX + 6, panelY + PANEL_HEIGHT - 24, PANEL_WIDTH - 12, 20).build();
        buttons.add(action);
        panelWidgets.add(action);

        list = new OfferList(listX, listY, listW, listH, action);
        buttons.add(list);
        panelWidgets.add(list);
    }

    private static void close(MerchantScreen screen) {
        var buttons = Screens.getButtons(screen);
        for (AbstractWidget widget : panelWidgets) {
            buttons.remove(widget);
        }
        panelWidgets.clear();
        list = null;
    }

    private static Component actionLabel() {
        String key = JobSiteReroller.isRunning() ? "button.stop" : "button.start";
        return Component.translatable("text." + TradeCyclingAutomator.MOD_ID + "." + key);
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
                    "text." + TradeCyclingAutomator.MOD_ID + ".panel.title",
                    Component.translatable("merchant.level." + villagerLevel)
            );
            graphics.drawString(minecraft.font, title, getX() + 6, getY() + 6, TEXT, false);
            Component status = JobSiteReroller.status();
            if (status != null && !status.getString().isEmpty()) {
                String line = minecraft.font.plainSubstrByWidth(status.getString(), width - 12);
                graphics.drawString(minecraft.font, line, getX() + 6, getY() + height - 38, TEXT, false);
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
            super(x, y, width, height, Component.translatable("text." + TradeCyclingAutomator.MOD_ID + ".panel.title", ""));
            this.action = action;
        }

        private void setScroll(int scroll) {
            this.scroll = Math.max(0, scroll);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
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
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, checkbox, getX() + 1, rowY + 4, 12, 12);
                ItemStack icon = entry.icon;
                graphics.renderItem(icon, getX() + 16, rowY + 1);
                graphics.renderItemDecorations(minecraft.font, icon, getX() + 16, rowY + 1);
                String name = minecraft.font.plainSubstrByWidth(entry.name, width - 86);
                graphics.drawString(minecraft.font, name, getX() + 36, rowY + 6, entry.curse ? CURSE : TEXT, false);
                String price = selected && entry.paysEmeralds
                        ? (TradeSession.anyPrice(entry.id) ? Component.translatable("text." + TradeCyclingAutomator.MOD_ID + ".any").getString() : "\u2264" + TradeSession.chosenMax(entry.id))
                        : entry.priceLabel();
                int priceWidth = minecraft.font.width(price);
                graphics.drawString(minecraft.font, price, getX() + width - 16 - priceWidth, rowY + 6, TEXT, false);
            }
            graphics.disableScissor();
            Identifier scroller = maxScroll == 0 ? SCROLLER_DISABLED : SCROLLER;
            int travel = Math.max(1, height - 27);
            int scrollerY = getY() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scroller, getX() + width - 6, scrollerY, 6, 27);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            if (!isMouseOver(event.x(), event.y())) {
                return false;
            }
            List<TradeEntry> rows = TradeSession.visible();
            int index = scroll + (int) ((event.y() - getY()) / ROW_HEIGHT);
            if (index < 0 || index >= rows.size()) {
                return true;
            }
            TradeEntry entry = rows.get(index);
            int priceColumn = getX() + width - 52;
            if (event.x() >= priceColumn) {
                TradeSession.cyclePrice(entry, event.button() == 1 ? -1 : 1);
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
