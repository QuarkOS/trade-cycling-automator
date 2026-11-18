package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason;
import dev.quarkos.villagerreroller.picker.MerchantLayout.Panel;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?}
import net.minecraft.network.chat.Component;
//? if <1.20.2 {
/*import net.minecraft.resources.Identifier;
*///?}
//? if <1.21.11 {
/*import net.minecraft.Util;
*///?} else {
import net.minecraft.util.Util;
//?}
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.quarkos.villagerreroller.picker.UiTheme.MUTED;
import static dev.quarkos.villagerreroller.picker.UiTheme.TEXT;
import static dev.quarkos.villagerreroller.picker.UiTheme.ACCENT;
import static dev.quarkos.villagerreroller.picker.UiTheme.BORDER;
import static dev.quarkos.villagerreroller.picker.UiTheme.RAISED;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.MerchantLayout.MERCHANT_WIDTH;
import static dev.quarkos.villagerreroller.picker.MerchantLayout.TRADE_ROW_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;
import static dev.quarkos.villagerreroller.picker.UiLayout.SCROLLBAR_SPACE;

/** Book button and trade picker alongside the vanilla merchant screen. */
public final class TradePickerUi {
    private static final int MERCHANT_HEIGHT = 166;
    /** Set by MerchantScreenShiftMixin when the merchant background follows leftPos. */
    public static boolean merchantBackgroundFollowsLeftPos;
    private static int merchantShift;
    private static int savedMerchantLeft = Integer.MIN_VALUE;
    private static int savedMerchantTop;
    private static UUID resultVillager;
    private static boolean awaitingOffers;

    private static final List<AbstractWidget> panelWidgets = new ArrayList<>();
    private static final Map<AbstractWidget, Boolean> merchantButtons = new HashMap<>();
    private static Button offersButton;
    private static OfferList list;
    private static EditBox searchBox;
    private static Button actionButton;
    private static Button methodButton;
    private static Button queueButton;
    private static Button hallButton;
    private static Button browserButton;

    private TradePickerUi() {
    }

    public static boolean panelShown() {
        return list != null;
    }

    public static boolean compactShown() {
        return !merchantButtons.isEmpty();
    }

    public static void offerButtonTooltip(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        if (offersButton != null && offersButton.visible && GuiDraw.bounds(offersButton).contains(mouseX, mouseY)) {
            canvas.tip(List.of(offersButton.getMessage()), mouseX, mouseY);
        }
    }

    public static void tick(Minecraft client) {
        if (!awaitingOffers || busy() || !(McBridge.currentScreen(client) instanceof MerchantScreen screen)
                || screen.getMenu().getOffers().isEmpty()) {
            return;
        }
        // The offers packet can arrive after screen initialization. Refresh once with its level and XP.
        Villager villager = LibrarianTracker.current(client);
        if (villager != null && client.level != null) {
            TradeSession.rebuild(client.level, villager, screen.getMenu());
        }
        awaitingOffers = false;
        syncButtons(startBlocker(client, screen));
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        // init() already rebuilt the vanilla widgets at the centered position.
        panelWidgets.clear();
        merchantButtons.clear();
        list = null;
        searchBox = null;
        actionButton = null;
        methodButton = null;
        queueButton = null;
        hallButton = null;
        browserButton = null;
        awaitingOffers = false;
        merchantShift = 0;
        savedMerchantLeft = Integer.MIN_VALUE;
        savedMerchantTop = Integer.MIN_VALUE;
        LibrarianTracker.bindNearest(client);
        int left = (screen.width - MERCHANT_WIDTH) / 2;
        int top = (screen.height - MERCHANT_HEIGHT) / 2;
        //? if <1.20.2 {
        /*offersButton = new ImageButton(
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
                message("button.offers")
        );
        *///?} else {
        offersButton = new ImageButton(
                left + MERCHANT_WIDTH - 24,
                top + 4,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> toggle(client, screen),
                message("button.offers")
        );
        //?}
        McBridge.buttons(screen).add(offersButton);
        if (TradeSession.panelOpen() || JobSiteReroller.isRunning() || TradeHall.isActive()) {
            TradeSession.setPanelOpen(true);
            open(client, screen);
        }
    }

    private static void toggle(Minecraft client, MerchantScreen screen) {
        if (panelShown()) {
            close(screen);
            TradeSession.setPanelOpen(false);
            return;
        }
        if (!open(client, screen)) {
            if (client.player != null) {
                McBridge.actionBar(client.player, message("panel.more_space"));
            }
            return;
        }
        TradeSession.setPanelOpen(true);
    }

    private static boolean open(Minecraft client, MerchantScreen screen) {
        close(screen);
        MerchantLayout.Placement placement = MerchantLayout.place(screen.width, merchantBackgroundFollowsLeftPos);
        Panel panel = Panel.place(placement, screen.height);
        if (!panel.shown() && merchantBackgroundFollowsLeftPos) {
            panel = Panel.compact(screen.width, screen.height);
        }
        if (!panel.shown()) {
            return false;
        }
        Villager villager = LibrarianTracker.current(client);
        if (villager != null && client.level != null) {
            TradeSession.rebuild(client.level, villager, screen.getMenu());
        }
        awaitingOffers = screen.getMenu().getOffers().isEmpty();
        savedMerchantLeft = McBridge.merchantLeft(screen);
        savedMerchantTop = McBridge.merchantTop(screen);
        boolean compact = panel.back() != null;
        int merchantLeft = compact ? screen.width + GAP : placement.merchantLeft();
        merchantShift = merchantLeft - savedMerchantLeft;
        McBridge.placeMerchant(screen, merchantLeft, savedMerchantTop);

        if (compact) {
            screen.setFocused(null);
        }
        if (merchantShift != 0) {
            for (AbstractWidget widget : McBridge.buttons(screen)) {
                if (compact) {
                    merchantButtons.put(widget, widget.active);
                    widget.active = false;
                }
                Rect bounds = GuiDraw.bounds(widget);
                FitButton.place(widget, bounds.x() + merchantShift, bounds.y());
            }
        }

        add(screen, new PanelBackground(screen, panel));
        if (compact) {
            add(screen, FitButton.create(panel.back(), message("button.back"), pressed -> toggle(client, screen)));
        }
        Rect toolbar = panel.search();
        int browseWidth = Math.min(54, toolbar.width() / 2);
        Rect searchBounds = new Rect(toolbar.x(), toolbar.y(), toolbar.width() - browseWidth - GAP, toolbar.height());
        Rect browseBounds = new Rect(toolbar.right() - browseWidth, toolbar.y(), browseWidth, toolbar.height());
        SearchBox search = new SearchBox(client.font, searchBounds, Component.translatable("gui.recipebook.search_hint"),
                () -> onAction(client, screen));
        search.setValue(TradeSession.filter());
        search.setResponder(value -> {
            TradeSession.setFilter(value);
            if (list != null) {
                list.setScroll(0);
            }
        });
        searchBox = add(screen, search);
        browserButton = add(screen, FitButton.create(browseBounds, message("button.browse"),
                pressed -> McBridge.setScreen(client, new TradeBrowserScreen(screen))));
        FitButton.hint(browserButton, message("browser.title"));

        Rect navigation = panel.navigation();
        queueButton = add(screen, FitButton.create(navigation.column(0, 3), message("button.queue"),
                pressed -> TradeHall.queueCurrent(client)));
        hallButton = add(screen, FitButton.create(navigation.column(1, 3), message("button.hall", TradeHall.size()),
                pressed -> McBridge.setScreen(client, new TradeHallScreen(screen))));
        Button saved = add(screen, FitButton.create(navigation.column(2, 3), message("button.list"),
                pressed -> McBridge.setScreen(client, new MyVillagersScreen(screen))));
        FitButton.hint(hallButton, message("button.hall.tooltip"));
        FitButton.hint(saved, message("button.list.tooltip"));
        if (RerollMethod.showToggle()) {
            methodButton = add(screen, FitButton.create(panel.method(), RerollMethod.label(), pressed -> {
                RerollMethod.toggle();
                pressed.setMessage(RerollMethod.label());
            }));
        }
        actionButton = add(screen, FitButton.primary(panel.action(), actionLabel(), pressed -> onAction(client, screen)));
        list = add(screen, new OfferList(panel.trades(), () -> onAction(client, screen)));
        syncButtons(startBlocker(client, screen));
        return true;
    }

    private static <T extends AbstractWidget> T add(MerchantScreen screen, T widget) {
        McBridge.buttons(screen).add(widget);
        panelWidgets.add(widget);
        return widget;
    }

    private static void close(MerchantScreen screen) {
        var buttons = McBridge.buttons(screen);
        if (panelWidgets.contains(screen.getFocused())) {
            screen.setFocused(null);
        }
        for (AbstractWidget widget : panelWidgets) {
            buttons.remove(widget);
        }
        panelWidgets.clear();
        merchantButtons.forEach((widget, active) -> widget.active = active);
        merchantButtons.clear();
        if (merchantShift != 0) {
            for (AbstractWidget widget : buttons) {
                Rect bounds = GuiDraw.bounds(widget);
                FitButton.place(widget, bounds.x() - merchantShift, bounds.y());
            }
            merchantShift = 0;
        }
        if (savedMerchantLeft != Integer.MIN_VALUE) {
            McBridge.placeMerchant(screen, savedMerchantLeft, savedMerchantTop);
            savedMerchantLeft = Integer.MIN_VALUE;
        }
        list = null;
        searchBox = null;
        actionButton = null;
        methodButton = null;
        hallButton = null;
        queueButton = null;
        browserButton = null;
        awaitingOffers = false;
    }

    /** Pixels available to a row name, also used by the client screenshot checks. */
    public static int nameBudget(int priceWidth) {
        int width = list == null ? MerchantLayout.PANEL_WIDTH - 2 * PADDING : list.bounds.width();
        return TradeNameLayout.availableWidth(width, Math.min(priceWidth, TradeNameLayout.priceBudget(width)));
    }

    private static Component actionLabel() {
        return message(busy() ? "button.stop" : "button.start");
    }

    private static boolean busy() {
        return TradeHall.isActive() || JobSiteReroller.isRunning();
    }

    private static void onAction(Minecraft client, MerchantScreen screen) {
        if (TradeHall.isActive()) {
            TradeHall.requestStop();
        } else if (JobSiteReroller.isRunning()) {
            JobSiteReroller.stop(StopReason.STOPPED);
        } else if (startBlocker(client, screen) == null) {
            Villager villager = LibrarianTracker.current(client);
            resultVillager = villager == null ? null : villager.getUUID();
            JobSiteReroller.start(client);
        }
    }

    private static StopReason startBlocker(Minecraft client, MerchantScreen screen) {
        if (busy()) {
            return null;
        }
        if (!AutomatorConfig.get().enabled()) {
            return StopReason.DISABLED;
        }
        Villager villager = LibrarianTracker.current(client);
        if (villager == null) {
            return StopReason.NOT_VILLAGER;
        }
        if (awaitingOffers) {
            return StopReason.NO_OFFERS;
        }
        if (TradeHall.menuLocked(screen.getMenu())) {
            return StopReason.LOCKED;
        }
        if (McBridge.unemployed(villager) || McBridge.nitwit(villager)) {
            return StopReason.NO_PROFESSION;
        }
        return TradeSession.hasSelection() ? null : StopReason.NEED_TARGET;
    }

    private static void syncButtons(StopReason blocker) {
        actionButton.active = blocker == null;
        actionButton.setMessage(actionLabel());
        FitButton.hint(actionButton, blocker == null ? null
                : message(blocker == StopReason.NO_OFFERS ? "panel.waiting" : blocker.id()));

        boolean busy = busy();
        browserButton.active = !busy;
        queueButton.active = !busy && !awaitingOffers;
        FitButton.hint(queueButton, message(busy ? "ui.busy" :
                awaitingOffers ? "panel.waiting" : TradeSession.hasSelection() ? "button.queue.tooltip" : "need_target"));
        hallButton.active = !TradeHall.isActive();
        hallButton.setMessage(message("button.hall", TradeHall.size()));

        if (methodButton != null) {
            methodButton.active = RerollMethod.canChoose() && !busy;
            methodButton.setMessage(RerollMethod.label());
            Component tip = !RerollMethod.canChoose() ? RerollMethod.unsupportedTooltip()
                    : busy ? message("ui.busy")
                    : message(RerollMethod.usesTradeCycling() ? "method.trade_cycling.tooltip" : "method.job_site.tooltip");
            FitButton.hint(methodButton, tip);
        }
    }

    private static String priceText(TradeEntry entry, boolean selected) {
        if (selected && entry.paysEmeralds) {
            return TradeSession.anyPrice(entry.id) ? message("any").getString() : "≤" + TradeSession.chosenMax(entry.id);
        }
        return entry.priceLabel();
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
        int visibleRows = list.visibleCount();
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        list.setScroll(Math.min(index, maxScroll));
    }

    /**
     * Wheel over the open trade list. Merchant screens swallow {@code mouseScrolled}
     * before a child widget sees it, so the screen hooks call this first.
     */
    public static boolean scrollWheel(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY, double vertical) {
        if (!(screen instanceof MerchantScreen) || list == null) {
            return false;
        }
        return list.scrollBy(mouseX, mouseY, vertical);
    }

    /** Drags the trade-list thumb through the merchant screen and returns the new offset. */
    public static int proveDrag(MerchantScreen screen) {
        return list == null ? -1 : list.proveDrag(screen);
    }

    /** Clicks the search box and types {@code text}. False when the screen closes or the value differs. */
    public static boolean proveType(MerchantScreen screen, String text) {
        if (searchBox == null || text == null) {
            return false;
        }
        Rect bounds = GuiDraw.bounds(searchBox);
        int x = bounds.x() + GAP;
        int y = bounds.y() + GAP;
        //? if <1.21.9 {
        /*screen.mouseClicked(x, y, InputConstants.MOUSE_BUTTON_LEFT);
        for (int i = 0; i < text.length(); i++) {
            char typed = text.charAt(i);
            int key = typed >= 'a' && typed <= 'z' ? typed - 32 : typed;
            screen.keyPressed(key, 0, 0);
            screen.charTyped(typed, 0);
            if (!(McBridge.currentScreen(Minecraft.getInstance()) instanceof MerchantScreen)) {
                return false;
            }
        }
        *///?} else {
        MouseButtonInfo info = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
        screen.mouseClicked(new MouseButtonEvent(x, y, info), false);
        for (int i = 0; i < text.length(); i++) {
            char typed = text.charAt(i);
            int key = typed >= 'a' && typed <= 'z' ? typed - 32 : typed;
            screen.keyPressed(new KeyEvent(key, 0, 0));
            //? if >=26.1 {
            /*screen.charTyped(new CharacterEvent(typed));
            *///?} else {
            screen.charTyped(new CharacterEvent(typed, 0));
            //?}
            if (!(McBridge.currentScreen(Minecraft.getInstance()) instanceof MerchantScreen)) {
                return false;
            }
        }
        //?}
        return text.equals(searchBox.getValue());
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

    public static boolean hasMethodButton() {
        return methodButton != null;
    }

    public static boolean methodButtonActive() {
        return methodButton != null && methodButton.active;
    }

    private static final int AIM_UNSET = Integer.MIN_VALUE;
    private static int aimedX = AIM_UNSET;
    private static int aimedY = AIM_UNSET;

    public static void aimCursorAtMethodButton() {
        if (methodButton != null) {
            Rect bounds = GuiDraw.bounds(methodButton);
            aimedX = bounds.x() + bounds.width() / 2;
            aimedY = bounds.y() + bounds.height() / 2;
        }
    }

    public static void clearAim() {
        aimedX = AIM_UNSET;
        aimedY = AIM_UNSET;
    }

    private static final class PanelBackground extends AbstractWidget {
        private final MerchantScreen screen;
        private final Panel panel;

        private PanelBackground(MerchantScreen screen, Panel panel) {
            super(panel.bounds().x(), panel.bounds().y(), panel.bounds().width(), panel.bounds().height(), Component.empty());
            this.screen = screen;
            this.panel = panel;
            active = false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }

        //? if <1.20 {
        /*@Override
        //? if <1.19.4 {
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        //?} else {
        public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        //?}
            paint(GuiDraw.of(pose), mouseX, mouseY);
        }
        *///?} else {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            paint(GuiDraw.of(graphics), mouseX, mouseY);
        }
        //?}

        private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
            Minecraft client = Minecraft.getInstance();
            StopReason blocker = startBlocker(client, screen);
            syncButtons(blocker);
            GuiDraw.well(canvas, panel.bounds());
            GuiDraw.frame(canvas, panel.status(), BORDER, RAISED);
            int level = Math.max(1, Math.min(5, screen.getMenu().getTraderLevel()));
            Component title = message("panel.title", Component.translatable("merchant.level." + level));
            Component picked = message("panel.picked", TradeSession.selectionCount());
            Rect line = panel.title();
            int countWidth = Math.min(canvas.font().width(picked), line.width() * 2 / 5);
            Rect titleBox = new Rect(line.x(), line.y(), line.width() - countWidth - GAP, line.height());
            Rect countBox = new Rect(line.right() - countWidth, line.y(), countWidth, line.height());
            GuiDraw.label(canvas, title, titleBox, TEXT, mouseX, mouseY);
            GuiDraw.label(canvas, picked, countBox, TradeSession.hasSelection() ? UiStatus.READY.color() : MUTED, mouseX, mouseY);
            paintStatus(canvas, blocker, mouseX, mouseY);
            if (methodButton == null) {
                Component method = panel.method().y() == panel.action().y()
                        ? RerollMethod.label() : message("panel.method", RerollMethod.label());
                GuiDraw.label(canvas, method, panel.method(), MUTED, mouseX, mouseY);
            }
        }

        private void paintStatus(GuiDraw.Canvas canvas, StopReason blocker, int mouseX, int mouseY) {
            if (TradeHall.isActive()) {
                resultVillager = null;
                status(canvas, TradeHall.hudStatus(), TradeHall.hudTitle(), List.of(TradeHall.hudDetail()), mouseX, mouseY);
                return;
            }
            Villager villager = LibrarianTracker.current(Minecraft.getInstance());
            UUID villagerId = villager == null ? null : villager.getUUID();
            if (JobSiteReroller.isRunning()) {
                resultVillager = villagerId;
                Component running = JobSiteReroller.status();
                status(canvas, UiStatus.forRun(true, false, null), running == null || running.getString().isEmpty()
                        ? message("rerolling", JobSiteReroller.attempts() + 1) : running, List.of(), mouseX, mouseY);
                return;
            }
            if (blocker == StopReason.NEED_TARGET) {
                GuiDraw.empty(canvas, panel.status(), message("panel.no_picks"), message("panel.pick_hint"), mouseX, mouseY);
                return;
            }
            if (blocker == StopReason.NO_OFFERS) {
                status(canvas, UiStatus.IDLE, message("panel.waiting"), List.of(), mouseX, mouseY);
                return;
            }
            List<Component> details = new ArrayList<>();
            UiStatus tone;
            Component label;
            if (blocker != null) {
                tone = UiStatus.ERROR;
                label = message("status.blocked");
                details.add(message(blocker.id()));
            } else {
                StopReason result = villagerId != null && villagerId.equals(resultVillager) ? JobSiteReroller.stopReason() : null;
                tone = UiStatus.forRun(false, true, result);
                if (result == null) {
                    label = message("status.ready");
                } else {
                    label = message(switch (result) {
                        case FOUND -> "status.matched";
                        case STOPPED, PANIC, GUI_CLOSED, ATTEMPT_CAP, DISABLED -> "status.stopped";
                        default -> "status.failed";
                    });
                    if (result != StopReason.FOUND) {
                        details.add(message(result.id()));
                    }
                }
            }
            String[] odds = oddsLines();
            if (odds != null) {
                details.add(Component.literal(odds[0]));
                if (odds[1] != null) {
                    Component time = Component.literal(odds[1]);
                    if (details.size() + 1 >= panel.statusLines()) {
                        int oddsIndex = details.size() - 1;
                        details.set(oddsIndex, message("ui.with_detail", details.get(oddsIndex), time));
                    } else {
                        details.add(time);
                    }
                }
            }
            status(canvas, tone, label, details, mouseX, mouseY);
        }

        private void status(GuiDraw.Canvas canvas, UiStatus tone, Component title, List<Component> details,
                            int mouseX, int mouseY) {
            Rect well = panel.status();
            canvas.fill(well.x() + 1, well.y() + 1, well.x() + 3, well.bottom() - 1, tone.color());
            List<Component> lines = new ArrayList<>();
            lines.add(title);
            for (Component detail : details) {
                if (!detail.getString().isEmpty() && !detail.getString().equals(title.getString())) {
                    lines.add(detail);
                }
            }
            int visible = Math.min(lines.size(), panel.statusLines());
            int y = well.y() + (well.height() - visible * LINE_HEIGHT) / 2;
            boolean clipped = visible < lines.size();
            for (int i = 0; i < visible; i++) {
                Rect line = new Rect(well.x() + GAP + 2, y + i * LINE_HEIGHT, well.width() - 2 * GAP - 2, LINE_HEIGHT);
                clipped |= GuiDraw.line(canvas, lines.get(i).getString(), line, i == 0 ? tone.color() : MUTED);
            }
            if (clipped && well.contains(mouseX, mouseY)) {
                canvas.tip(lines, mouseX, mouseY);
            }
        }

        //? if <1.19.3 {
        /*@Override
        public void updateNarration(NarrationElementOutput output) {
        }
        *///?} else {
        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
        //?}
    }

    private static final class OfferList extends AbstractWidget {
        private final Rect bounds;
        private final Runnable submit;
        private final ListScroll bar = new ListScroll();

        private OfferList(Rect bounds, Runnable submit) {
            super(bounds.x(), bounds.y(), bounds.width(), bounds.height(), message("button.offers"));
            this.bounds = bounds;
            this.submit = submit;
        }

        private void setScroll(int scroll) {
            bar.setOffset(scroll);
        }

        private int visibleCount() {
            return bounds.height() / TRADE_ROW_HEIGHT;
        }

        private int rowCount() {
            return TradeSession.visible().size();
        }

        private Rect track() {
            return new Rect(bounds.right() - SCROLLBAR_SPACE, bounds.y(), SCROLLBAR_SPACE, bounds.height());
        }

        //? if <1.20 {
        /*@Override
        //? if <1.19.4 {
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        //?} else {
        public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        //?}
            paint(GuiDraw.of(pose), mouseX, mouseY);
        }
        *///?} else {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            paint(GuiDraw.of(graphics), mouseX, mouseY);
        }
        //?}

        private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
            GuiDraw.fill(canvas, bounds, RAISED);
            if (awaitingOffers && !busy()) {
                return;
            }
            List<TradeEntry> rows = TradeSession.visible();
            int visible = visibleCount();
            bar.clamp(ListScroll.maxOffset(rows.size(), visible));
            if (rows.isEmpty()) {
                boolean emptyCatalog = TradeSession.entries().isEmpty();
                GuiDraw.frame(canvas, bounds, BORDER, RAISED);
                GuiDraw.empty(canvas, bounds, message(emptyCatalog ? "panel.empty" : "ui.no_results"),
                        message(emptyCatalog ? "panel.empty.hint" : "ui.clear_search"), mouseX, mouseY);
            } else {
                for (int i = 0; i < visible && bar.offset() + i < rows.size(); i++) {
                    int index = bar.offset() + i;
                    Rect row = new Rect(bounds.x(), bounds.y() + i * TRADE_ROW_HEIGHT,
                            bounds.width() - SCROLLBAR_SPACE, TRADE_ROW_HEIGHT);
                    paintRow(canvas, row, rows.get(index), mouseX, mouseY);
                }
                GuiDraw.scrollbar(canvas, track(), bar, rows.size(), visible);
            }
            if (aimedX != AIM_UNSET && methodButton != null && !RerollMethod.canChoose()) {
                canvas.tip(List.of(RerollMethod.unsupportedTooltip()), aimedX, aimedY);
            }
        }

        private void paintRow(GuiDraw.Canvas canvas, Rect row, TradeEntry entry, int mouseX, int mouseY) {
            boolean selected = TradeSession.selected(entry.id);
            boolean hovered = row.contains(mouseX, mouseY);
            GuiDraw.row(canvas, row, UiStatus.IDLE, hovered);
            if (selected) {
                canvas.fill(row.x(), row.y(), row.x() + 2, row.bottom() - 1, ACCENT);
            }
            canvas.checkbox(row.x() + GAP, row.y() + GAP, selected);
            canvas.item(entry.icon, row.x() + 16, row.y() + 1);

            Font font = canvas.font();
            Price price = price(font, entry, selected);
            int nameLeft = TradeNameLayout.nameLeft(row.x());
            int nameRight = TradeNameLayout.nameRight(price.x());
            int nameWidth = nameRight - nameLeft;
            int nameColor = entry.curse ? UiStatus.ERROR.color() : TEXT;
            int textY = row.y() + (TRADE_ROW_HEIGHT - LINE_HEIGHT) / 2;
            int textWidth = font.width(entry.name);
            if (textWidth > nameWidth) {
                int offset = ScrollingNames.offset(textWidth, nameWidth, Util.getMillis());
                canvas.scissor(nameLeft, row.y(), nameRight, row.bottom());
                canvas.text(entry.name, nameLeft - offset, textY, nameColor);
                canvas.unscissor();
                if (hovered && mouseX >= nameLeft && mouseX < nameRight) {
                    canvas.tip(List.of(Component.literal(entry.name)), mouseX, mouseY);
                }
            } else {
                canvas.text(entry.name, nameLeft, textY, nameColor);
            }
            int textX = price.x();
            if (price.editable()) {
                GuiDraw.frame(canvas, new Rect(price.x(), row.y() + 3, row.right() - 2 - price.x(),
                        row.height() - 7), BORDER, RAISED);
                textX += 3;
            }
            canvas.text(price.shown(), textX, textY, price.editable() ? ACCENT : MUTED);
            if (hovered && mouseX >= price.x() - 2) {
                List<Component> tips = new ArrayList<>();
                tips.add(message("price.emeralds", price.full()));
                if (entry.paysEmeralds && entry.emeraldMin != entry.emeraldMax) {
                    tips.add(message("price.hint"));
                }
                if (VillagerRosterClient.owns(Minecraft.getInstance(), entry.id)) {
                    tips.add(message("price.owned"));
                }
                canvas.tip(tips, mouseX, mouseY);
            }
        }

        private Price price(Font font, TradeEntry entry, boolean selected) {
            String full = priceText(entry, selected);
            boolean editable = selected && entry.paysEmeralds;
            int padding = editable ? 6 : 0;
            String shown = GuiDraw.ellipsis(font, full, TradeNameLayout.priceBudget(bounds.width()) - padding);
            int x = TradeNameLayout.priceX(bounds.x(), bounds.width(), font.width(shown) + padding);
            return new Price(full, shown, x, editable);
        }

        private record Price(String full, String shown, int x, boolean editable) {
        }

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
            if (!bounds.contains(mouseX, mouseY)) {
                return false;
            }
            if (awaitingOffers && !busy()) {
                return true;
            }
            Rect track = track();
            if (track.contains(mouseX, mouseY)) {
                bar.clickTrack(mouseY, track.y(), track.height(), rowCount(), visibleCount());
                return true;
            }
            List<TradeEntry> rows = TradeSession.visible();
            bar.clamp(ListScroll.maxOffset(rows.size(), visibleCount()));
            int offset = (int) ((mouseY - bounds.y()) / TRADE_ROW_HEIGHT);
            int index = bar.offset() + offset;
            if (offset >= visibleCount() || index >= rows.size()) {
                return true;
            }
            TradeEntry entry = rows.get(index);
            Price price = price(Minecraft.getInstance().font, entry, TradeSession.selected(entry.id));
            if (mouseX >= price.x() - 2) {
                TradeSession.cyclePrice(entry, button == InputConstants.MOUSE_BUTTON_RIGHT ? -1 : 1);
            } else {
                TradeSession.toggle(entry);
            }
            if (!busy()) {
                resultVillager = null;
            }
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        //? if <1.21.9 {
        /*@Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return dragThumb(mouseY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            bar.stop();
            return false;
        }
        *///?} else {
        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return dragThumb(event.y());
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            bar.stop();
            return false;
        }
        //?}

        private boolean dragThumb(double mouseY) {
            Rect track = track();
            return bar.drag(mouseY, track.y(), track.height(), rowCount(), visibleCount());
        }

        private int proveDrag(MerchantScreen screen) {
            int max = ListScroll.maxOffset(rowCount(), visibleCount());
            if (max <= 0) {
                return -1;
            }
            Rect track = track();
            int thumb = ListScroll.thumbHeight(track.height(), rowCount(), visibleCount());
            double x = track.x() + track.width() / 2.0;
            double y0 = track.y() + thumb / 2.0;
            double y1 = track.y() + track.height() * 0.55;
            int button = InputConstants.MOUSE_BUTTON_LEFT;
            //? if <1.21.9 {
            /*screen.mouseClicked(x, y0, button);
            screen.mouseDragged(x, y1, button, 0, y1 - y0);
            screen.mouseReleased(x, y1, button);
            *///?} else {
            MouseButtonInfo info = new MouseButtonInfo(button, 0);
            screen.mouseClicked(new MouseButtonEvent(x, y0, info), false);
            screen.mouseDragged(new MouseButtonEvent(x, y1, info), 0, y1 - y0);
            screen.mouseReleased(new MouseButtonEvent(x, y1, info));
            //?}
            return bar.offset();
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
            if (!bounds.contains(mouseX, mouseY)) {
                return false;
            }
            return bar.wheel(vertical, rowCount(), visibleCount());
        }

        private boolean submit(int key) {
            if (GuiDraw.enter(key)) {
                submit.run();
                return true;
            }
            return false;
        }

        //? if <1.21.9 {
        /*@Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return submit(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        *///?} else {
        @Override
        public boolean keyPressed(KeyEvent event) {
            return submit(event.key()) || super.keyPressed(event);
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
}
