package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.*;
import static dev.quarkos.villagerreroller.picker.UiTheme.*;

/** Inventory slots for the vanilla job blocks, items, and offer variants. */
public final class TradeBrowserScreen extends PanelScreen {
    private static final int SLOT = 18;
    private static final int MIN_ITEM_PANEL_WIDTH = 272;
    private static final int MAX_ITEM_PANEL_WIDTH = 352;
    private static final int MIN_ITEM_SLOT = 24;
    private static final int MAX_ITEM_SLOT = 28;
    private static final int ITEM_COLUMNS = 8;
    private static final int BOOK_ROW = 22;
    private static final int PRICE_WIDTH = 142;

    private final Screen parent;
    private final int[] pages = new int[Step.values().length];
    private final List<FitButton> slots = new ArrayList<>();
    private final List<Choice> pageChoices = new ArrayList<>();
    private Step step = Step.JOB;
    private List<TradeIcons.Job> jobs;
    private TradeIcons.Job job;
    private List<ItemGroup> items = List.of();
    private ItemGroup item;
    private TradeEntry trade;
    private Map<String, Map<String, Integer>> picked;
    private String query = "";
    private boolean anyPrice;
    private int price;
    private boolean saveFailed;
    private Rect frame;
    private Rect breadcrumb;
    private Rect prompt;
    private Rect body;
    private Rect footer;
    private Rect grid;
    private int slotSize;
    private int slotStep;
    private int columns;
    private int rows;
    private int pageCount;
    private int resultCount;
    private List<BookRow> bookRows = List.of();
    private Button previous;
    private Button next;
    private Button minus;
    private Button plus;
    private Button any;
    private Button add;

    public TradeBrowserScreen(Screen parent) {
        super(message("browser.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int frameHeight = switch (step) {
            case JOB -> 132;
            case ITEM -> items.size() <= ITEM_COLUMNS ? 140 : 170;
            case LEVEL -> 214;
            case PRICE -> 158;
        };
        int frameWidth = step == Step.ITEM
                ? Math.min(MAX_ITEM_PANEL_WIDTH, Math.max(MIN_ITEM_PANEL_WIDTH, width * 2 / 3)) : 280;
        frame = UiLayout.frame(width, height, frameWidth, frameHeight);
        Rect inner = frame.inset(PADDING);
        breadcrumb = new Rect(inner.x(), inner.y() + LINE_HEIGHT + GAP, inner.width(), LINE_HEIGHT);
        int promptY = breadcrumb.bottom() + GAP;
        prompt = new Rect(inner.x(), promptY, inner.width(), BUTTON_HEIGHT);
        footer = new Rect(inner.x(), inner.bottom() - BUTTON_HEIGHT, inner.width(), BUTTON_HEIGHT);
        int bodyY = prompt.bottom() + GAP;
        body = new Rect(inner.x(), bodyY, inner.width(), Math.max(0, footer.y() - GAP - bodyY));

        picked = ProfessionPicks.load(PickFiles.configFile()).snapshot();
        addRenderableWidget(FitButton.create(footer.column(0, 4), message("button.back"), pressed -> onClose()));
        if (step == Step.PRICE) {
            addPriceControls();
            return;
        }
        if (jobs == null) {
            jobs = TradeIcons.jobs(minecraft.level);
        }
        addGrid();
    }

    private void addGrid() {
        boolean books = showingBooks();
        boolean itemGrid = step == Step.ITEM;
        if (books) {
            int searchWidth = Math.min(110, prompt.width() / 2);
            Rect search = new Rect(prompt.right() - searchWidth, prompt.y(), searchWidth, prompt.height());
            prompt = new Rect(prompt.x(), prompt.y(), prompt.width() - searchWidth - GAP, prompt.height());
            SearchBox searchBox = new SearchBox(font, search, message("browser.search"), () -> {});
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                pages[Step.LEVEL.ordinal()] = 0;
                updateSlots();
            });
            addRenderableWidget(searchBox);
        }

        Rect pageButtons = footer.column(3, 4);
        previous = addRenderableWidget(FitButton.create(pageButtons.column(0, 2), message("ui.previous"), pressed -> turn(-1)));
        next = addRenderableWidget(FitButton.create(pageButtons.column(1, 2), message("ui.next"), pressed -> turn(1)));
        FitButton.hint(previous, message("ui.previous_page"));
        FitButton.hint(next, message("ui.next_page"));

        int columnBudget = books ? body.width() / 2 : body.width();
        int spacing = itemGrid ? GAP : 1;
        int extraWidth = Math.max(0, frame.width() - MIN_ITEM_PANEL_WIDTH);
        slotSize = itemGrid ? MIN_ITEM_SLOT + extraWidth * (MAX_ITEM_SLOT - MIN_ITEM_SLOT)
                / (MAX_ITEM_PANEL_WIDTH - MIN_ITEM_PANEL_WIDTH) : SLOT;
        slotStep = slotSize + spacing;
        int maxColumns = books ? 5 : switch (step) {
            case JOB -> 7;
            case ITEM -> ITEM_COLUMNS;
            default -> 12;
        };
        int availableWidth = itemGrid ? columnBudget - GAP : columnBudget;
        columns = Math.max(1, Math.min(maxColumns, availableWidth / slotStep));
        int rowHeight = books ? BOOK_ROW : slotStep;
        rows = Math.max(1, (body.height() - (itemGrid ? GAP : 0)) / rowHeight);
        int gridWidth = columns * slotStep - spacing;
        int x = books ? body.right() - gridWidth - GAP : body.x() + (body.width() - gridWidth) / 2;
        int y = body.y();
        if (itemGrid) {
            int usedRows = Math.max(1, Math.min(rows, (items.size() + columns - 1) / columns));
            y += (body.height() - (usedRows * rowHeight - spacing)) / 2;
        }
        grid = new Rect(x, y, gridWidth, rows * rowHeight);
        slots.clear();
        for (int index = 0; index < rows * columns; index++) {
            int cell = index;
            FitButton slot = addRenderableWidget(FitButton.slot(slotBounds(index), Component.empty(), ItemStack.EMPTY,
                    null, pressed -> pageChoices.get(cell).action().run()));
            slots.add(slot);
        }
        updateSlots();
    }

    private Rect slotBounds(int index) {
        int rowHeight = showingBooks() ? BOOK_ROW : slotStep;
        int topInset = step == Step.ITEM ? 0 : 1;
        return new Rect(grid.x() + index % columns * slotStep, grid.y() + index / columns * rowHeight + topInset,
                slotSize, slotSize);
    }

    private void updateSlots() {
        pageChoices.clear();
        if (showingBooks()) {
            List<BookRow> matches = matchingBooks();
            resultCount = matches.size();
            int start = firstOnPage(resultCount, rows);
            bookRows = matches.subList(start, Math.min(start + rows, resultCount));
            for (BookRow row : bookRows) {
                for (TradeEntry entry : row.entries()) {
                    pageChoices.add(choice(entry));
                }
                for (int i = row.entries().size(); i < columns; i++) {
                    pageChoices.add(null);
                }
            }
        } else {
            List<Choice> choices = switch (step) {
                case JOB -> jobs.stream().map(job -> new Choice(job.name(), job.icon(), null, null,
                        !picked.getOrDefault(job.id().toString(), Map.of()).isEmpty(), () -> chooseJob(job))).toList();
                case ITEM -> items.stream().map(group -> new Choice(group.name(), group.icon(), null,
                        group.entries().size() == 1 ? priceTip(group.entries().get(0)) : null,
                        group.entries().stream().anyMatch(entry -> picksForJob().containsKey(entry.id)),
                        () -> chooseItem(group))).toList();
                case LEVEL -> item.entries().stream().map(this::choice).toList();
                case PRICE -> List.of();
            };
            resultCount = choices.size();
            int start = firstOnPage(resultCount, slots.size());
            pageChoices.addAll(choices.subList(start, Math.min(start + slots.size(), resultCount)));
        }

        for (int i = 0; i < slots.size(); i++) {
            FitButton slot = slots.get(i);
            Choice choice = i < pageChoices.size() ? pageChoices.get(i) : null;
            slot.visible = choice != null;
            slot.active = slot.visible;
            if (choice != null) {
                slot.setMessage(choice.name());
                slot.setItem(choice.icon(), choice.badge());
                FitButton.hint(slot, choice.hint());
                FitButton.select(slot, choice.picked());
            }
        }
        previous.visible = next.visible = pageCount > 1;
        previous.active = page() > 0;
        next.active = page() + 1 < pageCount;
    }

    private Choice choice(TradeEntry entry) {
        return new Choice(Component.literal(entry.name), entry.icon, TradeIcons.levelBadge(TradeIcons.bookLevel(entry.id)),
                priceTip(entry), picksForJob().containsKey(entry.id), () -> chooseTrade(entry));
    }

    private static Component priceTip(TradeEntry entry) {
        return entry.paysEmeralds ? message("browser.buy_price", entry.priceLabel())
                : message("browser.sell_price", entry.emeraldMin);
    }

    private boolean showingBooks() {
        return step == Step.LEVEL && item.icon().is(Items.ENCHANTED_BOOK);
    }

    private List<BookRow> matchingBooks() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        Map<String, List<TradeEntry>> groups = new LinkedHashMap<>();
        for (TradeEntry entry : item.entries()) {
            int split = entry.id.lastIndexOf(':');
            String id = entry.id.startsWith("enchant:") ? entry.id.substring(0, split) : entry.id;
            if (entry.name.toLowerCase(Locale.ROOT).contains(needle)) {
                groups.computeIfAbsent(id, ignored -> new ArrayList<>()).add(entry);
            }
        }
        List<BookRow> result = new ArrayList<>();
        for (List<TradeEntry> group : groups.values()) {
            group.sort(Comparator.comparingInt(entry -> TradeIcons.bookLevel(entry.id)));
            TradeEntry first = group.get(0);
            String suffix = " " + Component.translatable("enchantment.level." + TradeIcons.bookLevel(first.id)).getString();
            String name = first.name.endsWith(suffix) ? first.name.substring(0, first.name.length() - suffix.length()) : first.name;
            for (int i = 0; i < group.size(); i += columns) {
                result.add(new BookRow(Component.literal(name), group.subList(i, Math.min(i + columns, group.size()))));
            }
        }
        return result;
    }

    private int firstOnPage(int count, int capacity) {
        pageCount = Math.max(1, (count + capacity - 1) / capacity);
        pages[step.ordinal()] = Math.min(page(), pageCount - 1);
        return page() * capacity;
    }

    private int page() {
        return pages[step.ordinal()];
    }

    private boolean turn(int change) {
        int nextPage = page() + change;
        if (nextPage < 0 || nextPage >= pageCount) {
            return false;
        }
        pages[step.ordinal()] = nextPage;
        updateSlots();
        return true;
    }

    private Map<String, Integer> picksForJob() {
        return picked.getOrDefault(job.id().toString(), Map.of());
    }

    private void chooseJob(TradeIcons.Job choice) {
        job = choice;
        Map<String, List<TradeEntry>> groups = new LinkedHashMap<>();
        for (TradeEntry entry : TradeCatalog.browse(minecraft.level, job.id())) {
            groups.computeIfAbsent(TradeEntry.itemId(entry.icon.getItem()), id -> new ArrayList<>()).add(entry);
        }
        List<ItemGroup> offers = new ArrayList<>();
        for (List<TradeEntry> entries : groups.values()) {
            entries.sort(Comparator.comparing((TradeEntry entry) -> entry.name).thenComparing(entry -> entry.id));
            TradeEntry first = entries.get(0);
            ItemStack icon = entries.size() == 1 ? first.icon : new ItemStack(first.icon.getItem());
            Component name = entries.size() == 1 && !icon.is(Items.ENCHANTED_BOOK)
                    ? Component.literal(first.name) : icon.getHoverName();
            offers.add(new ItemGroup(name, icon, entries));
        }
        offers.sort(Comparator.comparing(offer -> offer.name().getString()));
        items = offers;
        pages[Step.ITEM.ordinal()] = 0;
        show(Step.ITEM);
    }

    private void chooseItem(ItemGroup choice) {
        item = choice;
        pages[Step.LEVEL.ordinal()] = 0;
        query = "";
        if (item.entries().size() == 1) {
            chooseTrade(item.entries().get(0));
        } else {
            show(Step.LEVEL);
        }
    }

    private void chooseTrade(TradeEntry choice) {
        trade = choice;
        int saved = ProfessionPicks.fitPrice(picksForJob().getOrDefault(trade.id, 0),
                trade.emeraldMin, trade.emeraldMax, trade.paysEmeralds);
        anyPrice = !trade.paysEmeralds || saved == TradeSession.ANY_PRICE;
        price = saved == 0 || anyPrice ? trade.emeraldMax : saved;
        saveFailed = false;
        show(Step.PRICE);
    }

    private void addPriceControls() {
        add = addRenderableWidget(FitButton.primary(footer.column(3, 4), message("browser.add"), pressed -> savePick()));
        if (trade.paysEmeralds) {
            Rect controls = priceControls();
            minus = addRenderableWidget(FitButton.create(new Rect(controls.x(), controls.y(), BUTTON_HEIGHT, BUTTON_HEIGHT),
                    Component.literal("−"), pressed -> adjust(-1)));
            plus = addRenderableWidget(FitButton.create(new Rect(controls.x() + 66, controls.y(), BUTTON_HEIGHT, BUTTON_HEIGHT),
                    Component.literal("+"), pressed -> adjust(1)));
            any = addRenderableWidget(FitButton.create(new Rect(controls.x() + 90, controls.y(), controls.width() - 90, BUTTON_HEIGHT),
                    message("any"), pressed -> {
                        anyPrice = !anyPrice;
                        updatePriceControls();
                    }));
            FitButton.hint(minus, message("browser.shift_price"));
            FitButton.hint(plus, message("browser.shift_price"));
        }
        updatePriceControls();
    }

    private Rect priceControls() {
        return new Rect(body.right() - PRICE_WIDTH, body.y() + 28, PRICE_WIDTH, BUTTON_HEIGHT);
    }

    private void adjust(int direction) {
        //? if >=1.21.9 {
        boolean shift = minecraft.hasShiftDown();
        //?} else {
        /*boolean shift = hasShiftDown();
        *///?}
        price = Math.max(trade.emeraldMin, Math.min(trade.emeraldMax, price + (shift ? 5 : 1) * direction));
        updatePriceControls();
    }

    private int ceiling() {
        return anyPrice ? TradeSession.ANY_PRICE : price;
    }

    private void updatePriceControls() {
        int saved = picksForJob().getOrDefault(trade.id, 0);
        add.active = saved != ceiling();
        add.setMessage(message(!add.active ? "browser.added" : saved > 0 ? "browser.save_price" : "browser.add"));
        if (trade.paysEmeralds) {
            minus.active = !anyPrice && price > trade.emeraldMin;
            plus.active = !anyPrice && price < trade.emeraldMax;
            FitButton.select(any, anyPrice);
        }
    }

    private void savePick() {
        saveFailed = !PickFiles.writePick(job.id().toString(), trade.id, ceiling());
        if (!saveFailed) {
            picked = ProfessionPicks.load(PickFiles.configFile()).snapshot();
            updatePriceControls();
        }
    }

    private void show(Step next) {
        step = next;
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        switch (step) {
            case JOB -> McBridge.setScreen(minecraft, parent);
            case ITEM -> show(Step.JOB);
            case LEVEL -> show(Step.ITEM);
            case PRICE -> show(item.entries().size() == 1 ? Step.ITEM : Step.LEVEL);
        }
    }

    @Override
    protected List<Component> paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.panel(canvas, frame);
        Rect titleBox = new Rect(breadcrumb.x(), frame.y() + PADDING, breadcrumb.width(), LINE_HEIGHT);
        GuiDraw.label(canvas, title, titleBox, TEXT, mouseX, mouseY);
        GuiDraw.label(canvas, breadcrumbText(), breadcrumb, MUTED, mouseX, mouseY);
        String promptKey = switch (step) {
            case JOB -> "browser.choose_job";
            case ITEM -> "browser.choose_item";
            case LEVEL -> showingBooks() ? "browser.choose_level" : "browser.choose_offer";
            case PRICE -> trade.paysEmeralds ? "browser.max_price" : "browser.payout";
        };
        GuiDraw.label(canvas, message(promptKey), prompt, TEXT, mouseX, mouseY);
        if (step == Step.PRICE) {
            paintPrice(canvas, mouseX, mouseY);
        } else {
            paintGrid(canvas, mouseX, mouseY);
        }
        return List.of();
    }

    private Component breadcrumbText() {
        if (step == Step.JOB) {
            return message("browser.path_jobs");
        }
        Component name = job.name();
        if (step == Step.LEVEL || step == Step.PRICE) {
            name = message("browser.crumb", name, item.name());
        }
        if (step == Step.PRICE && !trade.name.equals(item.name().getString())) {
            name = message("browser.crumb", name, trade.name);
        }
        return name;
    }

    private void paintGrid(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.well(canvas, body);
        if (resultCount == 0) {
            GuiDraw.empty(canvas, body, message("ui.no_results"),
                    message(showingBooks() ? "ui.clear_search" : "browser.empty_catalog"), mouseX, mouseY);
            return;
        }
        if (showingBooks()) {
            for (int i = 0; i < bookRows.size(); i++) {
                Rect row = new Rect(body.x() + GAP, grid.y() + i * BOOK_ROW, grid.x() - body.x() - 2 * GAP, BOOK_ROW);
                GuiDraw.label(canvas, bookRows.get(i).name(), row, TEXT, mouseX, mouseY);
            }
        } else {
            int used = Math.min(slots.size(), ((pageChoices.size() + columns - 1) / columns) * columns);
            for (int i = pageChoices.size(); i < used; i++) {
                GuiDraw.frame(canvas, slotBounds(i), BORDER, BACKGROUND);
            }
        }
        if (pageCount > 1) {
            GuiDraw.label(canvas, message("ui.page", page() + 1, pageCount), footer.column(2, 4), MUTED, mouseX, mouseY);
        }
    }

    private void paintPrice(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.well(canvas, body);
        Rect icon = new Rect(body.x() + GAP, body.y() + GAP, SLOT, SLOT);
        GuiDraw.frame(canvas, icon, BORDER, RAISED);
        canvas.item(trade.icon, icon.x() + 1, icon.y() + 1, TradeIcons.levelBadge(TradeIcons.bookLevel(trade.id)));
        Rect name = new Rect(icon.right() + GAP, icon.y(), body.right() - icon.right() - 2 * GAP, SLOT);
        GuiDraw.label(canvas, Component.literal(trade.name), name, TEXT, mouseX, mouseY);

        Rect controls = priceControls();
        int priceX = trade.paysEmeralds ? controls.x() + BUTTON_HEIGHT + GAP : body.x() + GAP;
        canvas.item(new ItemStack(Items.EMERALD), priceX, controls.y() + 2);
        String value = (trade.paysEmeralds && anyPrice ? message("any") : Component.literal(Integer.toString(price))).getString();
        int valueWidth = trade.paysEmeralds ? controls.x() + 66 - priceX - 17 : controls.width();
        GuiDraw.line(canvas, value, new Rect(priceX + 17, controls.y(), valueWidth, controls.height()), TEXT);
        if (trade.paysEmeralds) {
            Rect range = new Rect(body.x() + GAP, controls.y(), controls.x() - body.x() - 2 * GAP, controls.height());
            GuiDraw.label(canvas, message("browser.vanilla_price", trade.priceLabel()), range, MUTED, mouseX, mouseY);
        }
        if (saveFailed) {
            GuiDraw.label(canvas, message("ui.save_failed"), footer.column(1, 4), RED, mouseX, mouseY);
        }
    }

    private boolean scroll(double x, double y, double delta) {
        return step != Step.PRICE && body.contains(x, y) && delta != 0 && turn(delta < 0 ? 1 : -1);
    }

    //? if <1.20.2 {
    /*@Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return scroll(x, y, delta) || super.mouseScrolled(x, y, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return scroll(x, y, vertical) || super.mouseScrolled(x, y, horizontal, vertical);
    }
    //?}

    private enum Step {
        JOB, ITEM, LEVEL, PRICE
    }

    private record ItemGroup(Component name, ItemStack icon, List<TradeEntry> entries) {
    }

    private record BookRow(Component name, List<TradeEntry> entries) {
    }

    private record Choice(Component name, ItemStack icon, String badge, Component hint, boolean picked, Runnable action) {
    }
}
