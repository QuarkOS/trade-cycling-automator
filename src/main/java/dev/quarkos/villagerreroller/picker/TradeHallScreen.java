package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.AutomatorConfig.HallView;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.picker.HallTradeFilter.Filter;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.*;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/** Wanted and recorded trades, with a separate view of the reroll queue. */
public final class TradeHallScreen extends PanelScreen {
    private static final int TILE_WIDTH = 33;
    private static final int TILE_HEIGHT = 24;
    private static final int TILE_GAP = 3;
    private static final int ITEM_SLOT = 18;
    private static final int PAGER_WIDTH = 96;
    private static final int LIST_STATS_WIDTH = 44;
    private static final int LIST_MARK_WIDTH = 18;
    private static final int QUEUE_ROW = 24;
    private static final int REMOVE_WIDTH = 48;

    private final Screen parent;
    private final ListScroll tradeScroll = new ListScroll();
    private final ListScroll queueScroll = new ListScroll();
    private final ItemStack emerald = new ItemStack(Items.EMERALD);
    private final List<Button> filters = new ArrayList<>();
    private final List<TileWidgets> tradeButtons = new ArrayList<>();
    private final List<Button> removeButtons = new ArrayList<>();
    private Tab tab;
    private Filter filter = Filter.ALL;
    private HallView view;
    private String query = "";
    private SearchBox searchBox;
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

        int tabsWidth = Math.min(146, tab == Tab.TRADES ? inner.width() / 2 : inner.width());
        Rect tabs = new Rect(inner.x(), header.bottom() + GAP, tabsWidth, BUTTON_HEIGHT);
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
            view = AutomatorConfig.get().hallView();
            initTradeHeader(inner, tabs);
            initTrades(inner, top);
        } else {
            initQueue(inner, top);
        }
    }

    private void showTab(Tab value) {
        if (tab != value) {
            stopScrolling();
            tab = value;
            rebuildWidgets();
        }
    }

    private void initTradeHeader(Rect inner, Rect tabs) {
        Rect list = new Rect(inner.right() - BUTTON_HEIGHT, tabs.y(), BUTTON_HEIGHT, BUTTON_HEIGHT);
        Rect grid = new Rect(list.x() - GAP - BUTTON_HEIGHT, list.y(), BUTTON_HEIGHT, BUTTON_HEIGHT);
        int searchX = tabs.right() + GAP;
        Rect search = new Rect(searchX, tabs.y(), grid.x() - searchX - GAP, BUTTON_HEIGHT);
        searchBox = new SearchBox(font, search, message("hall.search"), () -> {});
        searchBox.setMaxLength(128);
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            query = value;
            resetTradeFilter();
        });
        addRenderableWidget(searchBox);
        FitButton.select(addRenderableWidget(FitButton.icon(grid, message("hall.grid_view"), FitButton.Icon.GRID,
                pressed -> showView(HallView.GRID))), view == HallView.GRID);
        FitButton.select(addRenderableWidget(FitButton.icon(list, message("hall.list_view"), FitButton.Icon.LIST,
                pressed -> showView(HallView.LIST))), view == HallView.LIST);
    }

    private void showView(HallView value) {
        if (view != value) {
            tradeScroll.stop();
            AutomatorConfig.get().setHallView(value);
            AutomatorConfig.save();
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
                    message("hall.filter." + value.name().toLowerCase(Locale.ROOT)), pressed -> {
                        filter = value;
                        resetTradeFilter();
                    }));
            filters.add(button);
        }
        int resultsTop = chips.bottom() + GAP;
        well = new Rect(chips.x(), resultsTop, chips.width(), Math.max(0, footer.y() - GAP - resultsTop));
        tradeButtons.clear();
        if (view == HallView.GRID) {
            initGrid();
        } else {
            initList();
        }
        worldButton = addRenderableWidget(FitButton.create(new Rect(details.x(), footer.y(), details.width(), BUTTON_HEIGHT),
                message("hall.view_world"), pressed -> viewWorld()));
        refreshTrades();
    }

    private void initGrid() {
        columns = Math.max(1, (well.width() - 2 * GAP + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        visibleRows = Math.max(1, (well.height() - 2 * GAP + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));

        for (int index = 0; index < columns * visibleRows; index++) {
            Rect tile = tileBounds(index);
            addTradeButtons(index, new Rect(tile.x() + 3, tile.y() + 3, ITEM_SLOT, ITEM_SLOT),
                    new Rect(tile.right() - 12, tile.y() + 1, 11, 12));
        }
        int pagerWidth = Math.min(PAGER_WIDTH, well.right() - GuiDraw.bounds(backButton).right() - GAP);
        Rect pager = new Rect(well.right() - pagerWidth, footer.y(), pagerWidth, BUTTON_HEIGHT);
        pageLabel = new Rect(pager.column(1, 4).x(), pager.y(),
                pager.column(2, 4).right() - pager.column(1, 4).x(), BUTTON_HEIGHT);
        previous = addRenderableWidget(FitButton.create(pager.column(0, 4), message("ui.previous"), pressed -> turn(-1)));
        next = addRenderableWidget(FitButton.create(pager.column(3, 4), message("ui.next"), pressed -> turn(1)));
        FitButton.hint(previous, message("ui.previous_page"));
        FitButton.hint(next, message("ui.next_page"));
    }

    private void initList() {
        Rect content = well.inset(1);
        track = new Rect(content.right() - SCROLLBAR_SPACE, content.y(), SCROLLBAR_SPACE, content.height());
        visibleRows = content.height() / ROW_HEIGHT;
        for (int index = 0; index < visibleRows; index++) {
            Rect row = listRowBounds(index);
            addTradeButtons(index, new Rect(row.x(), row.y(), row.width() - LIST_MARK_WIDTH, row.height()),
                    new Rect(row.right() - LIST_MARK_WIDTH, row.y(), LIST_MARK_WIDTH, row.height()));
        }
    }

    private void addTradeButtons(int index, Rect selectBounds, Rect markBounds) {
        Button.OnPress select = pressed -> selectTile(tileAt(index));
        FitButton button = view == HallView.GRID
                ? FitButton.slot(selectBounds, Component.empty(), ItemStack.EMPTY, null, select)
                : FitButton.row(selectBounds, Component.empty(), select);
        FitButton wanted = FitButton.mark(markBounds, Component.empty(), pressed -> toggleWanted(tileAt(index)));
        tradeButtons.add(new TileWidgets(addRenderableWidget(button), addRenderableWidget(wanted)));
    }

    private Rect tileBounds(int index) {
        return new Rect(well.x() + GAP + index % columns * (TILE_WIDTH + TILE_GAP),
                well.y() + GAP + index / columns * (TILE_HEIGHT + TILE_GAP), TILE_WIDTH, TILE_HEIGHT);
    }

    private Rect listRowBounds(int index) {
        return new Rect(well.x() + 1, track.y() + index * ROW_HEIGHT, track.x() - well.x() - 1, ROW_HEIGHT);
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
        applyTradeFilter();
    }

    private void resetTradeFilter() {
        page = 0;
        tradeScroll.setOffset(0);
        tradeScroll.stop();
        applyTradeFilter();
    }

    private void applyTradeFilter() {
        filtered = tiles.stream().filter(tile -> HallTradeFilter.matches(query, filter,
                new HallTradeFilter.Tile(tile.name().getString(), tile.profession().getString(), tile.key().tradeId(),
                        tile.wanted(), tile.available()))).toList();
        syncTradeButtons();
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + tradeButtons.size() - 1) / tradeButtons.size());
    }

    private HallTrades.Tile tileAt(int cell) {
        return filtered.get(firstVisibleTrade() + cell);
    }

    private int firstVisibleTrade() {
        return view == HallView.GRID ? page * tradeButtons.size() : tradeScroll.offset();
    }

    private void syncTradeButtons() {
        if (view == HallView.GRID) {
            int pages = pageCount();
            page = Math.min(page, pages - 1);
            previous.visible = next.visible = pages > 1;
            previous.active = page > 0;
            next.active = page + 1 < pages;
        } else {
            tradeScroll.clamp(ListScroll.maxOffset(filtered.size(), visibleRows));
        }
        for (Filter value : Filter.values()) {
            FitButton.select(filters.get(value.ordinal()), filter == value);
        }
        boolean editable = canEditPicks();
        for (int index = 0; index < tradeButtons.size(); index++) {
            TileWidgets widgets = tradeButtons.get(index);
            boolean visible = firstVisibleTrade() + index < filtered.size();
            widgets.select().visible = widgets.wanted().visible = visible;
            widgets.select().active = visible;
            widgets.wanted().active = visible && editable;
            if (visible) {
                HallTrades.Tile tile = tileAt(index);
                widgets.select().setMessage(tile.name());
                if (view == HallView.GRID) {
                    widgets.select().setItem(tile.icon(), tile.badge());
                    FitButton.hint(widgets.select(), tile.profession());
                    FitButton.select(widgets.select(), isSelected(tile));
                }
                widgets.wanted().setMessage(message(tile.wanted() ? "hall.unwant_trade" : "hall.want_trade", tile.name()));
                FitButton.select(widgets.wanted(), tile.wanted());
            }
        }
        syncWorldButton();
    }

    private boolean turn(int change) {
        int nextPage = page + change;
        if (nextPage < 0 || nextPage >= pageCount()) {
            return false;
        }
        page = nextPage;
        syncTradeButtons();
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
        syncTradeButtons();
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
        int hintLines = GuiDraw.wrap(font, message("hall.hint").getString(), info.width()).size();
        caption = new Rect(info.x(), info.bottom() + GAP, info.width(), Math.min(2, Math.max(1, hintLines)) * LINE_HEIGHT);
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
            if (query.isBlank()) {
                String title = switch (filter) {
                    case ALL -> "hall.no_trades";
                    case AVAILABLE -> "hall.no_available";
                    case MISSING -> "hall.no_missing";
                };
                GuiDraw.empty(canvas, well, message(title), message(tiles.isEmpty()
                        ? "hall.pick_hint" : "hall.filter_hint"), mouseX, mouseY);
            } else {
                GuiDraw.empty(canvas, well, message("hall.no_matches", query.strip()),
                        message("hall.search_hint"), mouseX, mouseY);
            }
        }
        for (int index = 0; index < tradeButtons.size() && tradeButtons.get(index).select().visible; index++) {
            TileWidgets widgets = tradeButtons.get(index);
            HallTrades.Tile tile = tileAt(index);
            widgets.wanted().active = browseButton.active;
            if (view == HallView.GRID) {
                GuiDraw.frame(canvas, tileBounds(index), isSelected(tile) ? ACCENT : BORDER, RAISED);
            } else {
                paintTradeRow(canvas, listRowBounds(index), tile, widgets, mouseX, mouseY);
            }
        }
        if (view == HallView.LIST) {
            GuiDraw.scrollbar(canvas, track, tradeScroll, filtered.size(), visibleRows);
        } else if (pageCount() > 1) {
            GuiDraw.label(canvas, message("ui.page", page + 1, pageCount()), pageLabel, MUTED, mouseX, mouseY);
        }
        syncWorldButton();
        paintDetails(canvas, mouseX, mouseY);
    }

    private void paintTradeRow(GuiDraw.Canvas canvas, Rect row, HallTrades.Tile tile, TileWidgets widgets,
                               int mouseX, int mouseY) {
        boolean selected = isSelected(tile);
        GuiDraw.row(canvas, row, UiStatus.IDLE, selected || widgets.select().isFocused() || row.contains(mouseX, mouseY));
        if (selected) {
            canvas.fill(row.x(), row.y(), row.x() + 2, row.bottom() - 1, ACCENT);
        }

        Rect icon = new Rect(row.x() + GAP, row.y() + (row.height() - ITEM_SLOT) / 2, ITEM_SLOT, ITEM_SLOT);
        GuiDraw.frame(canvas, icon, selected ? ACCENT : BORDER, BACKGROUND);
        canvas.item(tile.icon(), icon.x() + 1, icon.y() + 1, tile.badge());

        int statsX = row.right() - LIST_MARK_WIDTH - GAP - LIST_STATS_WIDTH;
        int nameX = icon.right() + GAP;
        Rect name = new Rect(nameX, row.y() + 3, statsX - GAP - nameX, LINE_HEIGHT);
        GuiDraw.label(canvas, tile.name(), name, TEXT, mouseX, mouseY);
        GuiDraw.label(canvas, tile.profession(), new Rect(name.x(), name.bottom(), name.width(), LINE_HEIGHT),
                MUTED, mouseX, mouseY);
        if (tile.bestPrice() > 0) {
            canvas.item(emerald, statsX, row.y() + 1);
            GuiDraw.label(canvas, Component.literal(Integer.toString(tile.bestPrice())),
                    new Rect(statsX + 17, row.y() + 3, LIST_STATS_WIDTH - 17, LINE_HEIGHT), GREEN, mouseX, mouseY);
        } else {
            GuiDraw.line(canvas, "—", new Rect(statsX, row.y() + 3, LIST_STATS_WIDTH, LINE_HEIGHT), MUTED);
        }
        GuiDraw.label(canvas, message("hall.list_known", tile.knownCount()),
                new Rect(statsX, row.y() + 16, LIST_STATS_WIDTH, LINE_HEIGHT), MUTED, mouseX, mouseY);
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
        GuiDraw.frame(canvas, new Rect(content.x(), content.y(), ITEM_SLOT, ITEM_SLOT), BORDER, RAISED);
        canvas.item(tile.icon(), content.x() + 1, content.y() + 1, tile.badge());
        int nameInset = ITEM_SLOT + GAP;
        Rect name = new Rect(content.x() + nameInset, content.y(), content.width() - nameInset, LINE_HEIGHT);
        GuiDraw.label(canvas, tile.name(), name, TEXT, mouseX, mouseY);
        Rect profession = new Rect(name.x(), name.bottom(), name.width(), LINE_HEIGHT);
        GuiDraw.label(canvas, tile.profession(), profession, MUTED, mouseX, mouseY);
        Component availability = tile.available() ? message("hall.known_count", tile.knownCount()) : message("hall.missing");
        int y = profession.bottom() + GAP;
        detailLine(canvas, availability, y, tile.available() ? GREEN : AMBER, mouseX, mouseY);
        y += LINE_HEIGHT;
        if (tile.wanted()) {
            Component target = tile.wantedMax() == TradeSession.ANY_PRICE ? message("any")
                    : message("hall.max_price", tile.wantedMax());
            detailLine(canvas, message("hall.wanted_price", target), y, ACCENT, mouseX, mouseY);
            y += LINE_HEIGHT;
        }
        int rankY = y + GAP / 2;
        List<PriceRanking.Ranked> ranks = HallHighlights.ranks();
        if (ranks.isEmpty() && tile.available()) {
            detailLine(canvas, message("hall.no_loaded"), rankY, MUTED, mouseX, mouseY);
        }
        for (int index = 0; index < Math.min(ranks.size(), HallHighlights.PREVIEW_COUNT); index++) {
            PriceRanking.Ranked rank = ranks.get(index);
            detailLine(canvas, HallHighlights.label(rank), rankY + index * LINE_HEIGHT, rank.color(), mouseX, mouseY);
        }
    }

    private void detailLine(GuiDraw.Canvas canvas, Component line, int y, int color, int mouseX, int mouseY) {
        Rect rect = new Rect(details.x() + GAP, y, details.width() - 2 * GAP, LINE_HEIGHT);
        int bottom = details.bottom() - GAP - (saveFailed ? LINE_HEIGHT : 0);
        if (rect.bottom() <= bottom) {
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
            GuiDraw.hint(canvas, hint, caption, blocker != null && TradeHall.size() > 0 ? RED : MUTED, mouseX, mouseY);
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
        String targets = GuiDraw.ellipsis(canvas.font(), targetCount.getString(), textWidth / 3);
        int targetsX = right - canvas.font().width(targets);
        int detailY = row.y() + 13;
        GuiDraw.line(canvas, status.text().getString(), new Rect(textX, detailY, targetsX - GAP - textX, LINE_HEIGHT),
                status.tone().color());
        canvas.text(targets, targetsX, detailY, MUTED);
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
        if (key == InputConstants.KEY_ESCAPE) {
            if (searchFocused()) {
                unfocusSearch();
                return true;
            }
            return tab == Tab.QUEUE && HallHighlights.clear();
        }
        if (tab == Tab.QUEUE && GuiDraw.enter(key) && getFocused() == null && startBlocker() == null) {
            onPrimary();
            return true;
        }
        return false;
    }

    private boolean searchFocused() {
        return tab == Tab.TRADES && searchBox != null && getFocused() == searchBox && searchBox.isFocused();
    }

    private void unfocusSearch() {
        //? if <1.19.3 {
        /*searchBox.setFocus(false);
        *///?}
        setFocused(null);
    }

    private ListScroll scrollbar() {
        return tab == Tab.QUEUE ? queueScroll : view == HallView.LIST ? tradeScroll : null;
    }

    private int scrolledCount() {
        return tab == Tab.QUEUE ? TradeHall.size() : filtered.size();
    }

    private boolean afterScroll(boolean handled) {
        if (handled && tab == Tab.TRADES) {
            syncTradeButtons();
        }
        return handled;
    }

    private boolean clickTrack(double mouseX, double mouseY, int button) {
        ListScroll scroll = scrollbar();
        if (scroll == null || button != InputConstants.MOUSE_BUTTON_LEFT || !track.contains(mouseX, mouseY)
                || !scroll.clickTrack(mouseY, track.y(), track.height(), scrolledCount(), visibleRows)) {
            return false;
        }
        if (searchFocused()) {
            unfocusSearch();
        }
        return afterScroll(true);
    }

    private boolean dragTrack(double mouseY) {
        ListScroll scroll = scrollbar();
        return scroll != null && afterScroll(scroll.drag(mouseY, track.y(), track.height(), scrolledCount(), visibleRows));
    }

    private void stopScrolling() {
        tradeScroll.stop();
        queueScroll.stop();
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        if (!well.contains(mouseX, mouseY) || vertical == 0) {
            return false;
        }
        ListScroll scroll = scrollbar();
        return scroll == null ? turn(vertical < 0 ? 1 : -1)
                : afterScroll(scroll.wheel(vertical, scrolledCount(), visibleRows));
    }

    //? if <1.21.9 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyAction(keyCode) || (searchFocused() ? searchBox.keyPressed(keyCode, scanCode, modifiers)
                : super.keyPressed(keyCode, scanCode, modifiers));
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
        stopScrolling();
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyAction(event.key()) || (searchFocused() ? searchBox.keyPressed(event) : super.keyPressed(event));
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
        stopScrolling();
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

    private record TileWidgets(FitButton select, FitButton wanted) {
    }

    private record SlotStatus(Component text, UiStatus tone) {
    }
}
