package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
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
    private static final int COMPACT_PANEL_WIDTH = 144;
    private static final int WIDE_AT = 452;
    private static final int PANEL_HEIGHT = 200;
    private static final int LIST_TOP = 35;
    private static final int LIST_HEIGHT = 82;
    private static final int LIST_WIDTH = PANEL_WIDTH - 12;
    private static final int STATUS_TOP = 120;
    private static final int ODDS_SECOND = 133;
    private static final int METHOD_TOP = 146;
    private static final int QUEUE_TOP = 163;
    private static final int START_TOP = 180;
    private static final int ROW_HEIGHT = 20;
    private static final int SELECTED_NAME = 0xFF164A1A;
    private static int layoutListH = LIST_HEIGHT;
    private static int layoutStatus = STATUS_TOP;
    private static int layoutOdds = ODDS_SECOND;
    private static int layoutMethod = METHOD_TOP;
    private static int layoutQueue = QUEUE_TOP;
    private static int layoutStart = START_TOP;
    private static int merchantShift;
    private static int savedMerchantLeft = Integer.MIN_VALUE;
    private static int savedMerchantTop = Integer.MIN_VALUE;
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
    private static Button listButton;

    private TradePickerUi() {
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        panelWidgets.clear();
        list = null;
        methodButton = null;
        queueButton = null;
        hallButton = null;
        listButton = null;
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
        int naturalLeft = (screen.width - MERCHANT_WIDTH) / 2;
        int naturalTop = (screen.height - MERCHANT_HEIGHT) / 2;
        boolean wide = screen.width >= WIDE_AT;
        int panelW = wide ? PANEL_WIDTH : COMPACT_PANEL_WIDTH;
        int panelH = Math.min(PANEL_HEIGHT, Math.max(96, screen.height - 4));
        int overflow = PANEL_HEIGHT - panelH;
        int listH = LIST_HEIGHT;
        int statusH = 23;
        if (overflow > 0) {
            int cut = Math.min(overflow, listH - ROW_HEIGHT);
            listH -= cut;
            overflow -= cut;
        }
        if (overflow > 0) {
            int cut = Math.min(overflow, 11);
            statusH -= cut;
            overflow -= cut;
        }
        layoutListH = listH;
        layoutStatus = LIST_TOP + listH + 3;
        layoutOdds = layoutStatus + (statusH >= 20 ? 13 : 8);
        layoutMethod = layoutStatus + statusH + 3;
        layoutQueue = layoutMethod + 17;
        layoutStart = layoutQueue + 17;
        if (layoutStart + 16 > panelH - 2) {
            int shift = layoutStart + 16 - (panelH - 2);
            layoutMethod -= shift;
            layoutQueue -= shift;
            layoutStart -= shift;
        }
        int combined = MERCHANT_WIDTH + 3 + panelW;
        boolean sideBySide = screen.width >= combined + 2;
        int merchantLeft = naturalLeft;
        if (!wide && sideBySide) {
            merchantLeft = Math.max(2, (screen.width - combined) / 2);
        }
        int panelX;
        if (!sideBySide) {
            panelW = Math.min(panelW, Math.max(80, screen.width - 8));
            panelX = Math.max(2, screen.width - panelW - 2);
        } else {
            panelX = merchantLeft + MERCHANT_WIDTH + (wide ? 4 : 3);
        }
        if (panelX + panelW > screen.width - 2) {
            panelX = Math.max(2, screen.width - panelW - 2);
        }
        int panelY = Math.max(2, (screen.height - panelH) / 2);
        if (savedMerchantLeft == Integer.MIN_VALUE) {
            savedMerchantLeft = McBridge.merchantLeft(screen);
            savedMerchantTop = McBridge.merchantTop(screen);
        }
        McBridge.placeMerchant(screen, merchantLeft, naturalTop);
        merchantShift = merchantLeft - naturalLeft;

        var buttons = McBridge.buttons(screen);
        if (merchantShift != 0) {
            for (AbstractWidget widget : buttons) {
                FitButton.place(widget, widgetX(widget) + merchantShift, widgetY(widget));
            }
        }
        int listX = panelX + 6;
        int listY = panelY + LIST_TOP;
        int listW = Math.max(40, panelW - 12);

        PanelBackground background = new PanelBackground(panelX, panelY, panelW, panelH, screen.getMenu().getTraderLevel());
        buttons.add(background);
        panelWidgets.add(background);

        Component searchHint = Component.translatable("gui.recipebook.search_hint");
        EditBox search = new EditBox(client.font, panelX + 6, panelY + 17, panelW - 12, 14, searchHint);
        //? if >=1.19.3 {
        String hint = searchHint.getString();
        String shown = GuiDraw.ellipsis(client.font, hint, panelW - 20);
        search.setHint(Component.literal(shown));
        if (!shown.equals(hint)) {
            search.setTooltip(net.minecraft.client.gui.components.Tooltip.create(searchHint));
        }
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

        int inner = panelW - 12;
        int gap = 2;
        int share = Math.max(0, inner - gap * 2);
        int queueW = Math.max(24, share * 40 / 128);
        int hallW = Math.max(24, share * 44 / 128);
        int listBtnW = Math.max(24, inner - queueW - hallW - gap * 2);
        queueButton = FitButton.create(panelX + 6, panelY + layoutQueue, queueW, 15,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.queue"),
                pressed -> TradeHall.queueCurrent(client));
        hallButton = FitButton.create(panelX + 6 + queueW + gap, panelY + layoutQueue, hallW, 15,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall", TradeHall.size()),
                pressed -> {
                    if (!TradeHall.isActive()) {
                        McBridge.setScreen(client, new TradeHallScreen());
                    }
                });
        listButton = FitButton.create(panelX + 6 + queueW + hallW + gap * 2, panelY + layoutQueue, listBtnW, 15,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.list", VillagerRosterClient.count(client)),
                pressed -> McBridge.setScreen(client, new MyVillagersScreen(screen)));
        Button action = FitButton.create(panelX + 6, panelY + layoutStart, panelW - 12, 16, actionLabel(), button -> onAction(client));
        buttons.add(queueButton);
        panelWidgets.add(queueButton);
        buttons.add(hallButton);
        panelWidgets.add(hallButton);
        buttons.add(listButton);
        panelWidgets.add(listButton);
        buttons.add(action);
        panelWidgets.add(action);

        if (RerollMethod.showToggle()) {
            int buttonWidth = Math.max(36, panelW - 52);
            methodButton = FitButton.create(panelX + 46, panelY + layoutMethod, buttonWidth, 15, RerollMethod.label(), pressed -> {
                if (JobSiteReroller.isRunning() || TradeHall.isActive()) {
                    return;
                }
                RerollMethod.toggle();
                pressed.setMessage(RerollMethod.label());
            });
            buttons.add(methodButton);
            panelWidgets.add(methodButton);
        }

        list = new OfferList(listX, listY, listW, listH, action);
        buttons.add(list);
        panelWidgets.add(list);
    }

    private static void close(MerchantScreen screen) {
        var buttons = McBridge.buttons(screen);
        if (merchantShift != 0) {
            for (AbstractWidget widget : List.copyOf(buttons)) {
                if (!panelWidgets.contains(widget)) {
                    FitButton.place(widget, widgetX(widget) - merchantShift, widgetY(widget));
                }
            }
            merchantShift = 0;
        }
        if (savedMerchantLeft != Integer.MIN_VALUE) {
            McBridge.placeMerchant(screen, savedMerchantLeft, savedMerchantTop);
            savedMerchantLeft = Integer.MIN_VALUE;
        }
        for (AbstractWidget widget : panelWidgets) {
            buttons.remove(widget);
        }
        panelWidgets.clear();
        list = null;
        methodButton = null;
        queueButton = null;
        hallButton = null;
        listButton = null;
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
        if (listButton != null) {
            listButton.setMessage(Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".button.list",
                    VillagerRosterClient.count(Minecraft.getInstance())));
        }
    }

    private static String priceText(TradeEntry entry, boolean selected) {
        if (selected && entry.paysEmeralds) {
            return TradeSession.anyPrice(entry.id)
                    ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString()
                    : "\u2264" + TradeSession.chosenMax(entry.id);
        }
        String price = entry.priceLabel();
        if (VillagerRosterClient.owns(Minecraft.getInstance(), entry.id)) {
            price = "\u2713 " + price;
        }
        return price;
    }


    /**
     * Scrolls the open list so {@code id} is on screen. Used by the client proof
     * so the selected book and its odds are in the same frame.
     */
    public static void reveal(String id) {
        if (list == null || id == null) {
            return;
        }
        List<TradeEntry> rows = TradeSession.visible();
        int index = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (id.equals(rows.get(i).id)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        int visibleRows = Math.max(1, list.widgetHeight() / ROW_HEIGHT);
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        list.setScroll(Math.min(index, maxScroll));
    }

    public static String[] oddsLines() {
        if (JobSiteReroller.isRunning() || TradeHall.isActive()) {
            return null;
        }
        dev.quarkos.villagerreroller.odds.TradeOdds.Estimate estimate = TradeSession.estimate(JobSiteReroller.ticksPerAttempt());
        if (estimate == null) {
            return null;
        }
        if (estimate.impossible()) {
            return new String[]{Component.translatable("text." + VillagerReroller.MOD_ID + ".odds.impossible").getString(), null};
        }
        return new String[]{
                Component.translatable("text." + VillagerReroller.MOD_ID + ".odds.attempts", estimate.oneInText()).getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".odds.time", estimate.timeText()).getString()
        };
    }

    private static void syncMethodButton() {
        if (methodButton == null) {
            return;
        }
        methodButton.active = RerollMethod.canChoose() && !JobSiteReroller.isRunning() && !TradeHall.isActive();
        methodButton.setMessage(RerollMethod.label());
    }

    public static boolean hasMethodButton() {
        return methodButton != null;
    }

    public static boolean methodButtonActive() {
        return methodButton != null && methodButton.active;
    }

    public static void aimCursorAtMethodButton() {
        if (methodButton == null) {
            return;
        }
        aimedX = widgetX(methodButton) + methodButton.getWidth() / 2;
        aimedY = widgetY(methodButton) + methodButton.getHeight() / 2;
    }

    public static void clearAim() {
        aimedX = Integer.MIN_VALUE;
        aimedY = Integer.MIN_VALUE;
    }

    private static final int AIM_UNSET = Integer.MIN_VALUE;
    private static int aimedX = AIM_UNSET;
    private static int aimedY = AIM_UNSET;

    private static Component methodTip(int mouseX, int mouseY) {
        if (methodButton == null || !RerollMethod.showToggle() || RerollMethod.canChoose()) {
            return null;
        }
        int x = aimedX == AIM_UNSET ? mouseX : aimedX;
        int y = aimedY == AIM_UNSET ? mouseY : aimedY;
        if (aimedX == AIM_UNSET && !over(methodButton, x, y)) {
            return null;
        }
        return RerollMethod.unsupportedTooltip();
    }

    private static boolean over(AbstractWidget widget, int mouseX, int mouseY) {
        int left = widgetX(widget);
        int top = widgetY(widget);
        return mouseX >= left && mouseX < left + widget.getWidth() && mouseY >= top && mouseY < top + widget.getHeight();
    }

    private static int widgetX(AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.x;
        *///?} else {
        return widget.getX();
        //?}
    }

    private static int widgetY(AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.y;
        *///?} else {
        return widget.getY();
        //?}
    }

    //? if <1.20 {
    /*private static void drawMethodTip(PoseStack pose, int mouseX, int mouseY) {
        Component tip = methodTip(mouseX, mouseY);
        if (tip == null) {
            return;
        }
        int x = aimedX == AIM_UNSET ? mouseX : aimedX;
        int y = aimedY == AIM_UNSET ? mouseY : aimedY;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.font.width(tip);
        net.minecraft.client.gui.GuiComponent.fill(pose, x, y - 14, x + width + 6, y - 2, 0xF0100010);
        minecraft.font.draw(pose, tip, x + 3, y - 12, 0xFFFFFF);
    }
    *///?}
    //? if >=1.20 {
    private static void drawMethodTip(GuiGraphics graphics, int mouseX, int mouseY) {
        Component tip = methodTip(mouseX, mouseY);
        if (tip == null) {
            return;
        }
        int x = aimedX == AIM_UNSET ? mouseX : aimedX;
        int y = aimedY == AIM_UNSET ? mouseY : aimedY;
        //? if <1.21.6 {
        /*graphics.renderTooltip(Minecraft.getInstance().font, tip, x, y);
        *///?}
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(Minecraft.getInstance().font, tip, x, y);
        //?}
    }
    //?}

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
            String[] odds = oddsLines();
            if (odds != null) {
                if (odds[0] != null) {
                    minecraft.font.draw(pose, GuiDraw.ellipsis(minecraft.font, odds[0], width - 12), left() + 6, top() + layoutStatus, TEXT);
                }
                if (odds.length > 1 && odds[1] != null) {
                    minecraft.font.draw(pose, GuiDraw.ellipsis(minecraft.font, odds[1], width - 12), left() + 6, top() + layoutOdds, TEXT);
                }
            } else {
                Component status = statusLine();
                if (status != null && !status.getString().isEmpty()) {
                    String line = GuiDraw.ellipsis(minecraft.font, status.getString(), width - 12);
                    minecraft.font.draw(pose, line, left() + 6, top() + layoutStatus, TEXT);
                }
            }
            minecraft.font.draw(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"), left() + 6, top() + layoutMethod + 4, TEXT);
            if (methodButton == null) {
                minecraft.font.draw(pose, RerollMethod.label(), left() + 48, top() + layoutMethod + 4, TEXT);
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
            String[] odds = oddsLines();
            if (odds != null) {
                if (odds[0] != null) {
                    minecraft.font.draw(pose, GuiDraw.ellipsis(minecraft.font, odds[0], width - 12), getX() + 6, getY() + layoutStatus, TEXT);
                }
                if (odds.length > 1 && odds[1] != null) {
                    minecraft.font.draw(pose, GuiDraw.ellipsis(minecraft.font, odds[1], width - 12), getX() + 6, getY() + layoutOdds, TEXT);
                }
            } else {
                Component status = statusLine();
                if (status != null && !status.getString().isEmpty()) {
                    String line = GuiDraw.ellipsis(minecraft.font, status.getString(), width - 12);
                    minecraft.font.draw(pose, line, getX() + 6, getY() + layoutStatus, TEXT);
                }
            }
            minecraft.font.draw(pose, Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix"), getX() + 6, getY() + layoutMethod + 4, TEXT);
            if (methodButton == null) {
                minecraft.font.draw(pose, RerollMethod.label(), getX() + 48, getY() + layoutMethod + 4, TEXT);
            }
        }
        *///?}
        //? if >=1.20 {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiDraw.Canvas canvas = GuiDraw.of(graphics);
            GuiDraw.beveled(canvas, getX(), getY(), width, height);
            GuiDraw.recessed(canvas, getX() + 5, getY() + LIST_TOP - 1, width - 10, layoutListH + 2, 0xFF8E8E8E);
            int statusH = Math.max(12, layoutMethod - layoutStatus - 3);
            GuiDraw.recessed(canvas, getX() + 6, getY() + layoutStatus, width - 12, statusH, 0xFFB4B4B4);
            Minecraft minecraft = Minecraft.getInstance();
            String title = Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".panel.title",
                    Component.translatable("merchant.level." + villagerLevel)
            ).getString();
            int picked = TradeSession.selectionCount();
            String pickedText = picked == 0 ? "" : Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".panel.picked", picked).getString();
            int pillW = pickedText.isEmpty() ? 0 : minecraft.font.width(pickedText) + 8;
            String titleTip = GuiDraw.fit(canvas, title, getX() + 6, getY() + 6, Math.max(8, width - 12 - pillW), TEXT);
            if (picked > 0) {
                GuiDraw.pill(canvas, getX() + width - 6 - pillW, getY() + 4, pickedText, 0xFFD7ECD9, 0xFF388E3C, 0xFF1B5E20);
            }
            if (titleTip != null && mouseX >= getX() + 6 && mouseX < getX() + width - pillW && mouseY >= getY() + 4 && mouseY <= getY() + 16) {
                canvas.tip(List.of(Component.literal(title)), mouseX, mouseY);
            }
            String[] odds = oddsLines();
            String first;
            String second = null;
            if (odds != null && odds[0] != null) {
                first = odds[0];
                if (odds.length > 1) {
                    second = odds[1];
                }
            } else {
                Component status = statusLine();
                if (status != null && !status.getString().isEmpty()) {
                    first = status.getString();
                } else {
                    first = Component.translatable("text." + VillagerReroller.MOD_ID + ".odds.idle").getString();
                }
            }
            int textW = Math.max(8, width - 16);
            String firstShown = GuiDraw.fit(canvas, first, getX() + 8, getY() + layoutStatus + 3, textW, TEXT);
            String secondShown = null;
            if (second != null && statusH >= 20) {
                secondShown = GuiDraw.fit(canvas, second, getX() + 8, getY() + layoutOdds, textW, TEXT);
            }
            if (mouseX >= getX() + 6 && mouseX < getX() + width - 6 && mouseY >= getY() + layoutStatus && mouseY < getY() + layoutStatus + statusH) {
                if (firstShown != null || secondShown != null) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal(first));
                    if (second != null) {
                        lines.add(Component.literal(second));
                    }
                    canvas.tip(lines, mouseX, mouseY);
                }
            }
            String method = Component.translatable("text." + VillagerReroller.MOD_ID + ".method.prefix").getString();
            GuiDraw.fit(canvas, method, getX() + 6, getY() + layoutMethod + 4, 40, TEXT);
            if (methodButton == null) {
                String label = RerollMethod.label().getString();
                String labelTip = GuiDraw.fit(canvas, label, getX() + 46, getY() + layoutMethod + 4, Math.max(8, width - 52), TEXT);
                if (labelTip != null && mouseY >= getY() + layoutMethod && mouseY <= getY() + layoutMethod + 14) {
                    canvas.tip(List.of(Component.literal(label)), mouseX, mouseY);
                }
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

        private int widgetHeight() {
            //? if <1.19.3 {
            /*return height;
            *///?} else {
            return getHeight();
            //?}
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
            syncMethodButton();
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
                int rowRight = left() + width - 8;
                if (selected) {
                    fill(pose, left(), rowY, rowRight, rowY + ROW_HEIGHT, 0xFFC6E2C8);
                    fill(pose, left() + 1, rowY, left() + 3, rowY + ROW_HEIGHT, 0xFF2E7D32);
                    fill(pose, left(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF8FB892);
                } else {
                    fill(pose, left(), rowY, rowRight, rowY + ROW_HEIGHT - 1, ((scroll + i) % 2 == 0) ? 0xFFC0C0C0 : 0xFFB8B8B8);
                    fill(pose, left(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF9E9E9E);
                }
                fill(pose, left() + 1, rowY + 4, left() + 13, rowY + 16, 0xFF000000);
                fill(pose, left() + 2, rowY + 5, left() + 12, rowY + 15, selected ? 0xFF55FF55 : 0xFF8B8B8B);
                ItemStack icon = entry.icon;
                minecraft.getItemRenderer().renderGuiItem(icon, left() + 16, rowY + 1);
                minecraft.getItemRenderer().renderGuiItemDecorations(minecraft.font, icon, left() + 16, rowY + 1);
                String price = priceText(entry, selected);
                boolean pill = selected && entry.paysEmeralds;
                int priceWidth = minecraft.font.width(price) + (pill ? 6 : 0);
                int priceX = TradeNameLayout.priceX(left(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(left());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(pose, minecraft.font, entry.name, entry.curse ? CURSE : selected ? SELECTED_NAME : 0xFF303030, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                if (pill) {
                    fill(pose, priceX - 2, rowY + 4, priceX + minecraft.font.width(price) + 3, rowY + 15, 0xFF2E7D32);
                    fill(pose, priceX - 1, rowY + 5, priceX + minecraft.font.width(price) + 2, rowY + 14, 0xFFE6F5E7);
                }
                minecraft.font.draw(pose, price, priceX, rowY + 6, pill ? 0xFF1B5E20 : TEXT);
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
            drawMethodTip(pose, mouseX, mouseY);
        }
        //?}
        //? if >=1.19.4 {
        @Override
        public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            action.setMessage(actionLabel());
            refreshQueueButtons();
            syncMethodButton();
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
                int rowRight = getX() + width - 8;
                if (selected) {
                    fill(pose, getX(), rowY, rowRight, rowY + ROW_HEIGHT, 0xFFC6E2C8);
                    fill(pose, getX() + 1, rowY, getX() + 3, rowY + ROW_HEIGHT, 0xFF2E7D32);
                    fill(pose, getX(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF8FB892);
                } else {
                    fill(pose, getX(), rowY, rowRight, rowY + ROW_HEIGHT - 1, ((scroll + i) % 2 == 0) ? 0xFFC0C0C0 : 0xFFB8B8B8);
                    fill(pose, getX(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF9E9E9E);
                }
                fill(pose, getX() + 1, rowY + 4, getX() + 13, rowY + 16, 0xFF000000);
                fill(pose, getX() + 2, rowY + 5, getX() + 12, rowY + 15, selected ? 0xFF55FF55 : 0xFF8B8B8B);
                ItemStack icon = entry.icon;
                minecraft.getItemRenderer().renderGuiItem(pose, icon, getX() + 16, rowY + 1);
                minecraft.getItemRenderer().renderGuiItemDecorations(pose, minecraft.font, icon, getX() + 16, rowY + 1);
                String price = priceText(entry, selected);
                boolean pill = selected && entry.paysEmeralds;
                int priceWidth = minecraft.font.width(price) + (pill ? 6 : 0);
                int priceX = TradeNameLayout.priceX(getX(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(getX());
                int nameRight = TradeNameLayout.nameRight(priceX);
                drawName(pose, minecraft.font, entry.name, entry.curse ? CURSE : selected ? SELECTED_NAME : 0xFF303030, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                if (pill) {
                    fill(pose, priceX - 2, rowY + 4, priceX + minecraft.font.width(price) + 3, rowY + 15, 0xFF2E7D32);
                    fill(pose, priceX - 1, rowY + 5, priceX + minecraft.font.width(price) + 2, rowY + 14, 0xFFE6F5E7);
                }
                minecraft.font.draw(pose, price, priceX, rowY + 6, pill ? 0xFF1B5E20 : TEXT);
            }
            disableScissor();
            int travel = Math.max(1, height - 27);
            int scrollerY = getY() + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
            fill(pose, getX() + width - 6, scrollerY, getX() + width, scrollerY + 27, maxScroll == 0 ? 0xFF555555 : 0xFFC6C6C6);
            drawMethodTip(pose, mouseX, mouseY);
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
            syncMethodButton();
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
                int rowRight = getX() + width - 8;
                if (selected) {
                    graphics.fill(getX(), rowY, rowRight, rowY + ROW_HEIGHT, 0xFFC6E2C8);
                    graphics.fill(getX() + 1, rowY, getX() + 3, rowY + ROW_HEIGHT, 0xFF2E7D32);
                    graphics.fill(getX(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF8FB892);
                } else {
                    int stripe = ((scroll + i) % 2 == 0) ? 0xFFC0C0C0 : 0xFFB8B8B8;
                    graphics.fill(getX(), rowY, rowRight, rowY + ROW_HEIGHT - 1, stripe);
                    graphics.fill(getX(), rowY + ROW_HEIGHT - 1, rowRight, rowY + ROW_HEIGHT, 0xFF9E9E9E);
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
                String price = priceText(entry, selected);
                boolean pill = selected && entry.paysEmeralds;
                int priceWidth = minecraft.font.width(price) + (pill ? 6 : 0);
                int priceX = TradeNameLayout.priceX(getX(), width, priceWidth);
                int nameLeft = TradeNameLayout.nameLeft(getX());
                int nameRight = TradeNameLayout.nameRight(priceX);
                int nameColor = entry.curse ? CURSE : selected ? SELECTED_NAME : 0xFF303030;
                drawName(graphics, minecraft.font, entry.name, nameColor, nameLeft, nameRight, rowY, rowY + ROW_HEIGHT);
                if (minecraft.font.width(entry.name) > nameRight - nameLeft
                        && mouseX >= nameLeft && mouseX < nameRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    GuiDraw.of(graphics).tip(List.of(Component.literal(entry.name)), mouseX, mouseY);
                }
                if (pill) {
                    graphics.fill(priceX - 2, rowY + 4, priceX + minecraft.font.width(price) + 3, rowY + 15, 0xFF2E7D32);
                    graphics.fill(priceX - 1, rowY + 5, priceX + minecraft.font.width(price) + 2, rowY + 14, 0xFFE6F5E7);
                }
                int priceColor = pill ? 0xFF1B5E20 : TEXT;
                //? if >=26.1 {
                /*graphics.text(minecraft.font, price, priceX, rowY + 6, priceColor, false);
                *///?} else {
                graphics.drawString(minecraft.font, price, priceX, rowY + 6, priceColor, false);
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
            drawMethodTip(graphics, mouseX, mouseY);
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
