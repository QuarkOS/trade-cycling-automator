package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.*;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/** Wanted and recorded trades, with a separate view of the reroll queue. */
public final class TradeHallScreen extends PanelScreen {
    private static final int TILE_WIDTH = 36;
    private static final int TILE_HEIGHT = 24;
    private static final int TILE_GAP = 3;
    private static final int PAGER_WIDTH = 96;
    private static final int QUEUE_ROW = 24;
    private static final int REMOVE_WIDTH = 48;

    private final Screen parent;
    private final ListScroll queueScroll = new ListScroll();
    private final List<Button> filters = new ArrayList<>();
    private final List<TileWidgets> tradeButtons = new ArrayList<>();
    private final List<Button> removeButtons = new ArrayList<>();
    private Tab tab;
    private Filter filter = Filter.ALL;
    private HallTrades trades;
    private ClientLevel tradeLevel;
    private List<HallTrades.Tile> tiles = List.of();
    private List<HallTrades.Tile> filtered = List.of();
    private List<SlotStatus> statuses = List.of();
    private int queueStatusAge;
    private int page;
    private int columns;
    private int visibleRows;
    private int[] removeSlots;
    private boolean saveFailed;
    private Rect frame;
    private Rect header;
    private Rect well;
    private Rect footer;
    private Rect details;
    private Rect pageLabel;
    private Rect info;
    private Rect caption;
    private Rect track;
    private List<Component> hover = List.of();
    private Button browseButton;
    private Button villagersButton;
    private Button previous;
    private Button next;
    private Button worldButton;
    private Button primaryButton;
    private Button clearButton;
    private Button backButton;

    public TradeHallScreen() {
        this(null);
    }

    public TradeHallScreen(Screen parent) {
        this(parent, TradeHall.showingSummary());
    }

    TradeHallScreen(Screen parent, boolean queue) {
        super(message(TradeHall.showingSummary() ? "hall.summary" : "hall.title"));
        this.parent = parent;
        this.tab = queue ? Tab.QUEUE : Tab.TRADES;
    }

    TradeHallScreen showQueue() {
        tab = Tab.QUEUE;
        return this;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        HallHighlights.clear();
        TradeHall.dismiss();
        McBridge.setScreen(minecraft, parent);
    }

    @Override
    protected void init() {
        Rect centered = UiLayout.frame(width, height, 352, 244);
        frame = new Rect(Math.min(centered.x(), 2 * PADDING), centered.y(), centered.width(), centered.height());
        Rect inner = frame.inset(PADDING);
        int villagersWidth = Math.min(100, inner.width() / 3);
        int browseWidth = Math.min(62, inner.width() / 4);
        Rect villagers = new Rect(inner.right() - villagersWidth, inner.y(), villagersWidth, BUTTON_HEIGHT);
        Rect browse = new Rect(villagers.x() - browseWidth - GAP, inner.y(), browseWidth, BUTTON_HEIGHT);
        header = new Rect(inner.x(), inner.y(), browse.x() - inner.x() - GAP, BUTTON_HEIGHT);

        browseButton = addRenderableWidget(FitButton.create(browse, message("button.browse"),
                pressed -> McBridge.setScreen(minecraft, new TradeBrowserScreen(this))));
        FitButton.hint(browseButton, message("browser.title"));
        villagersButton = addRenderableWidget(FitButton.create(villagers,
                message("button.my_villagers", VillagerRosterClient.count(minecraft)), pressed -> openVillagers()));
        FitButton.hint(villagersButton, message("button.list.tooltip"));

        Rect tabs = new Rect(inner.x(), header.bottom() + GAP, Math.min(146, inner.width()), BUTTON_HEIGHT);
        for (Tab value : Tab.values()) {
            Button button = addRenderableWidget(FitButton.create(tabs.column(value.ordinal(), Tab.values().length),
                    message(value == Tab.TRADES ? "hall.trades" : "hall.queue"), pressed -> showTab(value)));
            FitButton.select(button, value == tab);
        }

        footer = new Rect(inner.x(), inner.bottom() - BUTTON_HEIGHT, inner.width(), BUTTON_HEIGHT);
        Rect back = new Rect(footer.x(), footer.y(), Math.min(62, footer.width() / 3), BUTTON_HEIGHT);
        backButton = addRenderableWidget(FitButton.create(back, message("button.back"), pressed -> onClose()));
        int top = tabs.bottom() + GAP;
        if (tab == Tab.TRADES) {
            initTrades(inner, top);
        } else {
            initQueue(inner, top);
        }
    }

    private void showTab(Tab value) {
        if (tab != value) {
            tab = value;
            rebuildWidgets();
        }
    }

    private void openVillagers() {
        McBridge.setScreen(minecraft, parent instanceof MyVillagersScreen ? parent : new MyVillagersScreen(this));
    }

    private void initTrades(Rect inner, int top) {
        if (trades == null || tradeLevel != minecraft.level) {
            tradeLevel = minecraft.level;
            trades = new HallTrades(tradeLevel);
        }
        int detailsWidth = Math.min(116, inner.width() * 3 / 8);
        details = new Rect(inner.right() - detailsWidth, top, detailsWidth, footer.y() - top - GAP);
        Rect chips = new Rect(inner.x(), top, details.x() - inner.x() - GAP, BUTTON_HEIGHT);
        filters.clear();
        for (Filter value : Filter.values()) {
            Button button = addRenderableWidget(FitButton.create(chips.column(value.ordinal(), Filter.values().length),
                    message(value.key), pressed -> {
                        filter = value;
                        page = 0;
                        updateTradeButtons();
                    }));
            filters.add(button);
        }
        int gridTop = chips.bottom() + GAP;
        well = new Rect(chips.x(), gridTop, chips.width(), Math.max(0, footer.y() - GAP - gridTop));
        columns = Math.max(1, (well.width() - 2 * GAP + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        visibleRows = Math.max(1, (well.height() - 2 * GAP + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));

        tradeButtons.clear();
        for (int index = 0; index < columns * visibleRows; index++) {
            int cell = index;
            Rect tile = tileBounds(index);
            FitButton icon = addRenderableWidget(FitButton.slot(new Rect(tile.x() + 1, tile.y() + 1, 21, 22),
                    Component.empty(), ItemStack.EMPTY, null, pressed -> selectTile(tileAt(cell))));
            FitButton wanted = addRenderableWidget(FitButton.checkbox(new Rect(tile.right() - 14, tile.y() + 2, 12, 12),
                    Component.empty(), false, pressed -> toggleWanted(tileAt(cell))));
            tradeButtons.add(new TileWidgets(icon, wanted));
        }
        int pagerWidth = Math.min(PAGER_WIDTH, well.right() - GuiDraw.bounds(backButton).right() - GAP);
        Rect pager = new Rect(well.right() - pagerWidth, footer.y(), pagerWidth, BUTTON_HEIGHT);
        pageLabel = new Rect(pager.column(1, 4).x(), pager.y(),
                pager.column(2, 4).right() - pager.column(1, 4).x(), BUTTON_HEIGHT);
        previous = addRenderableWidget(FitButton.create(pager.column(0, 4), message("ui.previous"), pressed -> turn(-1)));
        next = addRenderableWidget(FitButton.create(pager.column(3, 4), message("ui.next"), pressed -> turn(1)));
        FitButton.hint(previous, message("ui.previous_page"));
        FitButton.hint(next, message("ui.next_page"));
        worldButton = addRenderableWidget(FitButton.create(new Rect(details.x(), footer.y(), details.width(), BUTTON_HEIGHT),
                message("hall.view_world"), pressed -> viewWorld()));
        refreshTrades();
    }

    private Rect tileBounds(int index) {
        return new Rect(well.x() + GAP + index % columns * (TILE_WIDTH + TILE_GAP),
                well.y() + GAP + index / columns * (TILE_HEIGHT + TILE_GAP), TILE_WIDTH, TILE_HEIGHT);
    }

    private void refreshTrades() {
        tiles = trades.tiles(minecraft);
        HallTrades.Tile selected = HallHighlights.selected();
        if (selected != null) {
            HallTrades.Tile updated = tiles.stream().filter(tile -> tile.key().equals(selected.key())).findFirst().orElse(null);
            if (updated == null) {
                HallHighlights.clear();
            } else {
                HallHighlights.select(minecraft, updated);
            }
        }
        updateTradeButtons();
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + tradeButtons.size() - 1) / tradeButtons.size());
    }

    private HallTrades.Tile tileAt(int cell) {
        return filtered.get(page * tradeButtons.size() + cell);
    }

    private void updateTradeButtons() {
        filtered = tiles.stream().filter(filter::includes).toList();
        page = Math.min(page, pageCount() - 1);
        for (Filter value : Filter.values()) {
            FitButton.select(filters.get(value.ordinal()), filter == value);
        }
        for (int index = 0; index < tradeButtons.size(); index++) {
            TileWidgets widgets = tradeButtons.get(index);
            boolean visible = page * tradeButtons.size() + index < filtered.size();
            widgets.icon().visible = widgets.wanted().visible = visible;
            widgets.icon().active = widgets.wanted().active = visible;
            if (visible) {
                HallTrades.Tile tile = tileAt(index);
                widgets.icon().setMessage(tile.name());
                widgets.icon().setItem(tile.icon(), tile.badge());
                FitButton.hint(widgets.icon(), TradeIcons.professionName(tile.key().profession()));
                FitButton.select(widgets.icon(), isSelected(tile));
                widgets.wanted().setMessage(message(tile.wanted() ? "hall.unwant_trade" : "hall.want_trade", tile.name()));
                FitButton.select(widgets.wanted(), tile.wanted());
            }
        }
        previous.visible = next.visible = pageCount() > 1;
        previous.active = page > 0;
        next.active = page + 1 < pageCount();
        syncWorldButton();
    }

    private boolean turn(int change) {
        int nextPage = page + change;
        if (nextPage < 0 || nextPage >= pageCount()) {
            return false;
        }
        page = nextPage;
        updateTradeButtons();
        return true;
    }

    private static boolean isSelected(HallTrades.Tile tile) {
        HallTrades.Tile selected = HallHighlights.selected();
        return selected != null && selected.key().equals(tile.key());
    }

    private void selectTile(HallTrades.Tile tile) {
        if (isSelected(tile)) {
            HallHighlights.clear();
        } else {
            HallHighlights.select(minecraft, tile);
        }
        updateTradeButtons();
    }

    private void toggleWanted(HallTrades.Tile tile) {
        if (!canEditPicks()) {
            return;
        }
        int ceiling = tile.wanted() ? 0 : tile.defaultMax();
        HallTrades.Key key = tile.key();
        saveFailed = !PickFiles.writePick(key.profession(), key.tradeId(), ceiling);
        if (!saveFailed) {
            refreshTrades();
        }
    }

    private static boolean canEditPicks() {
        return !TradeHall.isActive() && !JobSiteReroller.isRunning();
    }

    private void syncWorldButton() {
        worldButton.visible = HallHighlights.selected() != null;
        worldButton.active = worldButton.visible && !HallHighlights.ranks().isEmpty();
        FitButton.hint(worldButton, message(worldButton.active ? "hall.view_world_hint" : "hall.no_loaded"));
    }

    private void viewWorld() {
        if (minecraft.player != null && minecraft.player.containerMenu instanceof MerchantMenu) {
            minecraft.player.closeContainer();
        }
        McBridge.setScreen(minecraft, null);
    }

    private void initQueue(Rect inner, int top) {
        info = new Rect(inner.x(), top, inner.width(), LINE_HEIGHT);
        caption = new Rect(info.x(), info.bottom() + GAP, info.width(), LINE_HEIGHT);
        int listTop = caption.bottom() + GAP;
        well = new Rect(inner.x(), listTop, inner.width(), Math.max(0, footer.y() - GAP - listTop));
        track = new Rect(well.right() - SCROLLBAR_SPACE, well.y(), SCROLLBAR_SPACE, well.height());
        visibleRows = well.height() / QUEUE_ROW;

        removeButtons.clear();
        removeSlots = new int[visibleRows];
        Arrays.fill(removeSlots, -1);
        for (int i = 0; i < visibleRows; i++) {
            int row = i;
            Button button = addRenderableWidget(FitButton.create(new Rect(0, 0, REMOVE_WIDTH, BUTTON_HEIGHT),
                    message("button.remove"), pressed -> {
                        if (removeSlots[row] >= 0) {
                            TradeHall.remove(removeSlots[row]);
                            Arrays.fill(removeSlots, -1);
                            refreshQueue();
                        }
                    }));
            button.visible = false;
            button.active = false;
            removeButtons.add(button);
        }
        clearButton = addRenderableWidget(FitButton.create(footer.column(1, 3), message("button.clear"), pressed -> {
            TradeHall.clear();
            Arrays.fill(removeSlots, -1);
            queueScroll.setOffset(0);
            refreshQueue();
        }));
        primaryButton = addRenderableWidget(FitButton.primary(footer.column(2, 3), actionLabel(), pressed -> onPrimary()));
        refreshQueue();
    }

    private void refreshQueue() {
        boolean summary = TradeHall.showingSummary();
        boolean active = TradeHall.isActive();
        statuses = TradeHall.slots().stream().map(slot -> status(slot, summary, active)).toList();
        queueStatusAge = 0;
        syncQueueButtons(startBlocker());
    }

    @Override
    public void tick() {
        super.tick();
        if (tab == Tab.QUEUE && (++queueStatusAge >= 10 || statuses.size() != TradeHall.size())) {
            refreshQueue();
        }
    }

    private void syncQueueButtons(Component blocker) {
        primaryButton.setMessage(actionLabel());
        primaryButton.active = blocker == null;
        FitButton.hint(primaryButton, blocker);
        boolean summary = TradeHall.showingSummary();
        backButton.visible = backButton.active = !summary;
        clearButton.visible = !summary && !TradeHall.isActive();
        clearButton.active = clearButton.visible && TradeHall.size() > 0;
    }

    private Component actionLabel() {
        if (TradeHall.showingSummary()) {
            return Component.translatable("gui.done");
        }
        return message(TradeHall.isActive() ? "button.stop" : "button.hall_start");
    }

    private Component startBlocker() {
        if (TradeHall.showingSummary() || TradeHall.isActive()) {
            return null;
        }
        if (TradeHall.size() == 0) {
            return message("hall.empty");
        }
        if (!AutomatorConfig.get().enabled()) {
            return message("disabled");
        }
        return JobSiteReroller.isRunning() ? message("ui.busy") : null;
    }

    private void onPrimary() {
        if (TradeHall.showingSummary()) {
            onClose();
        } else if (TradeHall.isActive()) {
            TradeHall.requestStop();
        } else if (startBlocker() == null) {
            HallHighlights.clear();
            TradeHall.start(minecraft);
        }
    }

    @Override
    protected List<Component> paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        hover = List.of();
        browseButton.active = canEditPicks();
        villagersButton.setMessage(message("button.my_villagers", VillagerRosterClient.count(minecraft)));
        GuiDraw.panel(canvas, frame);
        GuiDraw.label(canvas, message(TradeHall.showingSummary() ? "hall.summary" : "hall.title"),
                header, TEXT, mouseX, mouseY);
        GuiDraw.well(canvas, well);
        if (tab == Tab.TRADES) {
            paintTrades(canvas, mouseX, mouseY);
        } else {
            paintQueue(canvas, mouseX, mouseY);
        }
        return hover;
    }

    private void paintTrades(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        if (filtered.isEmpty()) {
            GuiDraw.empty(canvas, well, message(filter.emptyKey), message(tiles.isEmpty()
                    ? "hall.pick_hint" : "hall.filter_hint"), mouseX, mouseY);
        }
        for (int index = 0; index < tradeButtons.size() && tradeButtons.get(index).icon().visible; index++) {
            HallTrades.Tile tile = tileAt(index);
            tradeButtons.get(index).wanted().active = browseButton.active;
            Rect box = tileBounds(index);
            GuiDraw.frame(canvas, box, isSelected(tile) ? ACCENT : BORDER, RAISED);
            int statusColor = tile.available() ? GREEN : AMBER;
            canvas.fill(box.right() - 12, box.bottom() - 6, box.right() - 4, box.bottom() - 4, statusColor);
        }
        if (pageCount() > 1) {
            GuiDraw.label(canvas, message("ui.page", page + 1, pageCount()), pageLabel, MUTED, mouseX, mouseY);
        }
        syncWorldButton();
        paintDetails(canvas, mouseX, mouseY);
    }

    private void paintDetails(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.well(canvas, details);
        HallTrades.Tile tile = HallHighlights.selected();
        if (saveFailed) {
            Rect error = new Rect(details.x() + GAP, details.bottom() - LINE_HEIGHT - GAP,
                    details.width() - 2 * GAP, LINE_HEIGHT);
            GuiDraw.label(canvas, message("ui.save_failed"), error, RED, mouseX, mouseY);
        }
        if (tile == null) {
            GuiDraw.empty(canvas, details, message("hall.details"), message("hall.select_trade"), mouseX, mouseY);
            return;
        }
        Rect content = details.inset(GAP);
        canvas.item(tile.icon(), content.x(), content.y(), tile.badge());
        Rect name = new Rect(content.x() + 18, content.y(), content.width() - 18, 18);
        GuiDraw.label(canvas, tile.name(), name, TEXT, mouseX, mouseY);
        detailLine(canvas, TradeIcons.professionName(tile.key().profession()), 22, MUTED, mouseX, mouseY);
        Component availability = tile.available() ? message("hall.known_count", tile.knownCount()) : message("hall.missing");
        detailLine(canvas, availability, 35, tile.available() ? GREEN : AMBER, mouseX, mouseY);
        if (tile.wanted()) {
            Component target = tile.wantedMax() == TradeSession.ANY_PRICE ? message("any")
                    : message("hall.max_price", tile.wantedMax());
            detailLine(canvas, message("hall.wanted_price", target), 48, ACCENT, mouseX, mouseY);
        }
        int rankY = 64;
        List<PriceRanking.Ranked> ranks = HallHighlights.ranks();
        if (ranks.isEmpty() && tile.available()) {
            detailLine(canvas, message("hall.no_loaded"), rankY, MUTED, mouseX, mouseY);
        }
        for (int index = 0; index < Math.min(ranks.size(), HallHighlights.PREVIEW_COUNT); index++) {
            PriceRanking.Ranked rank = ranks.get(index);
            detailLine(canvas, HallHighlights.label(rank), rankY + index * 13, rank.color(), mouseX, mouseY);
        }
    }

    private void detailLine(GuiDraw.Canvas canvas, Component line, int y, int color, int mouseX, int mouseY) {
        Rect rect = new Rect(details.x() + GAP, details.y() + GAP + y, details.width() - 2 * GAP, LINE_HEIGHT);
        if (rect.bottom() <= details.bottom() - GAP) {
            GuiDraw.label(canvas, line, rect, color, mouseX, mouseY);
        }
    }

    private void paintQueue(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        if (statuses.size() != TradeHall.size()) {
            refreshQueue();
        }
        boolean summary = TradeHall.showingSummary();
        boolean running = TradeHall.isActive();
        Component blocker = startBlocker();
        syncQueueButtons(blocker);
        List<TradeHall.Slot> slots = TradeHall.slots();
        paintQueueStatus(canvas, summary, running, blocker, mouseX, mouseY);
        queueScroll.clamp(ListScroll.maxOffset(slots.size(), visibleRows));
        for (Button button : removeButtons) {
            button.visible = button.active = false;
        }
        if (slots.isEmpty()) {
            GuiDraw.empty(canvas, well, message("hall.empty_title"), message("hall.empty"), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < visibleRows && queueScroll.offset() + i < slots.size(); i++) {
            int index = queueScroll.offset() + i;
            paintSlot(canvas, slots.get(index), statuses.get(index), index, i, !summary && !running, mouseX, mouseY);
        }
        GuiDraw.scrollbar(canvas, track, queueScroll, slots.size(), visibleRows);
    }

    private void paintQueueStatus(GuiDraw.Canvas canvas, boolean summary, boolean running, Component blocker, int mouseX, int mouseY) {
        if (summary) {
            GuiDraw.label(canvas, message("hall.results"), info, TEXT, mouseX, mouseY);
            GuiDraw.label(canvas, TradeHall.countsLine(), caption, MUTED, mouseX, mouseY);
        } else if (running) {
            GuiDraw.label(canvas, TradeHall.hudTitle(), info, TradeHall.hudStatus().color(), mouseX, mouseY);
            GuiDraw.label(canvas, TradeHall.hudDetail(), caption, MUTED, mouseX, mouseY);
        } else {
            long ready = statuses.stream().filter(status -> status.tone() == UiStatus.READY).count();
            GuiDraw.label(canvas, message("hall.ready_count", ready, TradeHall.size()), info,
                    ready > 0 ? GREEN : MUTED, mouseX, mouseY);
            Component hint = blocker == null || TradeHall.size() == 0 ? message("hall.hint") : blocker;
            GuiDraw.label(canvas, hint, caption, blocker != null && TradeHall.size() > 0 ? RED : MUTED, mouseX, mouseY);
        }
    }

    private void paintSlot(GuiDraw.Canvas canvas, TradeHall.Slot slot, SlotStatus status, int index, int visible,
                           boolean removable, int mouseX, int mouseY) {
        Rect row = new Rect(well.x(), well.y() + visible * QUEUE_ROW, well.width() - SCROLLBAR_SPACE, QUEUE_ROW);
        boolean hovered = row.contains(mouseX, mouseY);
        GuiDraw.row(canvas, row, status.tone(), hovered);
        int textX = row.x() + PADDING;
        int right = row.right() - GAP;
        if (removable) {
            Button button = removeButtons.get(visible);
            removeSlots[visible] = index;
            right -= REMOVE_WIDTH;
            FitButton.place(button, right, row.y() + 2);
            button.visible = button.active = true;
            right -= GAP;
        }
        Component identity = message("ui.with_detail", message("hall.position", index + 1, slot.label()), slot.place());
        Component targetCount = Component.literal(slot.description());
        int textWidth = right - textX;
        GuiDraw.line(canvas, identity.getString(), new Rect(textX, row.y() + 2, textWidth, LINE_HEIGHT), TEXT);
        int statusWidth = Math.min(canvas.font().width(status.text()), textWidth * 2 / 3);
        GuiDraw.line(canvas, status.text().getString(), new Rect(textX, row.y() + 13, statusWidth, LINE_HEIGHT), status.tone().color());
        int picksX = textX + statusWidth + GAP;
        GuiDraw.line(canvas, targetCount.getString(), new Rect(picksX, row.y() + 13, right - picksX, LINE_HEIGHT), MUTED);
        if (hovered && mouseX < right) {
            hover = List.of(identity, status.text(), targetCount);
        }
    }

    private SlotStatus status(TradeHall.Slot slot, boolean summary, boolean running) {
        if ((summary || running) && slot.outcome() != TradeHall.Outcome.PENDING) {
            return new SlotStatus(TradeHall.outcomeLine(slot), UiStatus.forOutcome(slot.outcome()));
        }
        if (running) {
            return TradeHall.isCurrent(slot) ? new SlotStatus(TradeHall.hudDetail(), TradeHall.hudStatus())
                    : new SlotStatus(message("hall.pending"), UiStatus.RUNNING);
        }
        String blocker = slot.isIn(minecraft.level) ? TradeHall.blocker(minecraft, slot.villagerId()) : "villager_gone";
        return blocker == null ? new SlotStatus(message("hall.ready"), UiStatus.READY)
                : new SlotStatus(message("hall.skipped", message("hall.why." + blocker)), UiStatus.RUNNING);
    }

    private boolean keyAction(int key) {
        if (key == InputConstants.KEY_ESCAPE && HallHighlights.clear()) {
            if (tab == Tab.TRADES) {
                updateTradeButtons();
            }
            return true;
        }
        if (tab == Tab.QUEUE && GuiDraw.enter(key) && getFocused() == null && startBlocker() == null) {
            onPrimary();
            return true;
        }
        return false;
    }

    private boolean clickTrack(double mouseX, double mouseY, int button) {
        return tab == Tab.QUEUE && button == InputConstants.MOUSE_BUTTON_LEFT && track.contains(mouseX, mouseY)
                && queueScroll.clickTrack(mouseY, track.y(), track.height(), TradeHall.size(), visibleRows);
    }

    private boolean dragTrack(double mouseY) {
        return tab == Tab.QUEUE && queueScroll.drag(mouseY, track.y(), track.height(), TradeHall.size(), visibleRows);
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        if (!well.contains(mouseX, mouseY) || vertical == 0) {
            return false;
        }
        return tab == Tab.QUEUE ? queueScroll.wheel(vertical, TradeHall.size(), visibleRows) : turn(vertical < 0 ? 1 : -1);
    }

    //? if <1.21.9 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyAction(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return clickTrack(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return dragTrack(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        queueScroll.stop();
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyAction(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return clickTrack(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return dragTrack(event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        queueScroll.stop();
        return super.mouseReleased(event);
    }
    //?}

    //? if <1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollBy(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return scrollBy(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    //?}

    private enum Tab { TRADES, QUEUE }

    private enum Filter {
        ALL("hall.filter.all", "hall.no_trades"),
        AVAILABLE("hall.filter.available", "hall.no_available"),
        MISSING("hall.filter.missing", "hall.no_missing");

        private final String key;
        private final String emptyKey;

        Filter(String key, String emptyKey) {
            this.key = key;
            this.emptyKey = emptyKey;
        }

        private boolean includes(HallTrades.Tile tile) {
            return switch (this) {
                case ALL -> true;
                case AVAILABLE -> tile.available();
                case MISSING -> tile.wanted() && !tile.available();
            };
        }
    }

    private record TileWidgets(FitButton icon, FitButton wanted) {
    }

    private record SlotStatus(Component text, UiStatus tone) {
    }
}
