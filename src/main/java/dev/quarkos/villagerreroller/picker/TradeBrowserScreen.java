package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.net.ServerRerollClient;
import dev.quarkos.villagerreroller.odds.TradeOdds;
import dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import dev.quarkos.villagerreroller.text.TradeNameLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
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
    private static final int PICK_ROW = 18;
    private static final int PICK_HEADER = 16;

    private final Screen parent;
    private final Villager villager;
    private final MerchantScreen merchant;
    private final int[] pages = new int[Step.values().length];
    private final List<FitButton> slots = new ArrayList<>();
    private final List<Choice> pageChoices = new ArrayList<>();
    private Step step = Step.JOB;
    private List<TradeIcons.Job> jobs;
    private TradeIcons.Job job;
    private List<ItemGroup> items = List.of();
    private ItemGroup item;
    private TradeEntry trade;
    private TradeCatalog.BrowserCatalog catalog;
    private Map<String, Map<String, Integer>> picked;
    private Map<String, Integer> targets = Map.of();
    private TradeOdds.Pool pool = TradeOdds.Pool.EMPTY;
    private TradeOdds.Estimate estimate;
    private int estimatePace;
    private List<String> pickIds = List.of();
    private int pickPage;
    private int picksPerPage;
    private String query = "";
    private boolean anyPrice;
    private int price;
    private boolean saveFailed;
    private Rect frame;
    private Rect breadcrumb;
    private Rect prompt;
    private Rect body;
    private Rect footer;
    private Rect rollBounds;
    private Rect pageLabel;
    private Rect grid;
    private Rect pickList;
    private Rect status;
    private Rect navigation;
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
    private Button roll;
    private Button queue;
    private Button hall;
    private Button method;
    private Component runHint;
    private StopReason jobSiteBlocker;
    private int jobSiteCheckTicks;

    public TradeBrowserScreen(Screen parent) {
        this(parent, null);
    }

    TradeBrowserScreen(Screen parent, Villager villager) {
        super(message("browser.title"));
        this.parent = parent;
        this.merchant = parent instanceof MerchantScreen screen ? screen : null;
        this.villager = villager;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static boolean busy() {
        return JobSiteReroller.isRunning() || TradeHall.isActive();
    }

    /** While the browser is over a live merchant, local rerolls still use that menu. */
    static MerchantScreen activeMerchant(Minecraft client) {
        Screen screen = McBridge.currentScreen(client);
        if (screen instanceof MerchantScreen merchant) {
            return merchant;
        }
        return screen instanceof TradeBrowserScreen browser && browser.hasMerchant() ? browser.merchant : null;
    }

    @Override
    protected void init() {
        if (jobs == null) {
            jobs = TradeIcons.jobs(minecraft.level);
            if (villager != null) {
                String profession = McBridge.professionId(villager);
                jobs.stream().filter(choice -> choice.id().toString().equals(profession)).findFirst().ifPresent(choice -> {
                    selectJob(choice);
                    step = Step.ITEM;
                });
            }
        }
        reloadPicks();
        layout();

        previous = next = minus = plus = any = add = roll = queue = method = null;
        runHint = null;
        addRenderableWidget(FitButton.create(footer.column(0, 4), message("button.back"), pressed -> onClose()));
        if (step == Step.PRICE) {
            addPriceControls();
        } else {
            addGrid();
            if (villager != null && job != null) {
                roll = addRenderableWidget(FitButton.primary(rollBounds, message("browser.start"), pressed -> onRoll()));
            }
        }
        if (job != null) {
            addPickControls();
        }
        addNavigation();
        jobSiteCheckTicks = 0;
        updateControls();
    }

    private void layout() {
        int pickHeight = Math.min(3, pickIds.size()) * PICK_ROW;
        int frameHeight = switch (step) {
            case JOB -> 152;
            case ITEM -> (items.size() <= ITEM_COLUMNS ? 198 : 228) + pickHeight;
            case LEVEL -> 260 + pickHeight;
            case PRICE -> 224 + pickHeight;
        };
        int frameWidth = step == Step.ITEM
                ? Math.min(MAX_ITEM_PANEL_WIDTH, Math.max(MIN_ITEM_PANEL_WIDTH, width * 2 / 3)) : 304;
        frame = UiLayout.frame(width, height, frameWidth, frameHeight);
        Rect inner = frame.inset(PADDING);
        breadcrumb = new Rect(inner.x(), inner.y() + LINE_HEIGHT + GAP, inner.width(), job == null ? LINE_HEIGHT : SLOT);
        prompt = new Rect(inner.x(), breadcrumb.bottom() + 2, inner.width(), showingBooks() ? BUTTON_HEIGHT : 14);
        footer = new Rect(inner.x(), inner.bottom() - BUTTON_HEIGHT, inner.width(), BUTTON_HEIGHT);
        int rollWidth = Math.min(106, footer.width() / 3);
        rollBounds = new Rect(footer.right() - rollWidth, footer.y(), rollWidth, footer.height());
        navigation = new Rect(inner.x(), footer.y() - BUTTON_HEIGHT - GAP, inner.width(), BUTTON_HEIGHT);
        int bodyY = prompt.bottom() + GAP;
        int bodyBottom = navigation.y() - GAP;
        if (job != null) {
            status = new Rect(inner.x(), bodyBottom - 2 * LINE_HEIGHT, inner.width(), 2 * LINE_HEIGHT);
            int room = status.y() - 2 * GAP - PICK_HEADER - bodyY - 48;
            picksPerPage = Math.min(Math.max(1, pickIds.size()), Math.max(1, Math.min(3, room / PICK_ROW)));
            int visiblePicksHeight = PICK_HEADER + (pickIds.isEmpty() ? 0 : picksPerPage * PICK_ROW);
            pickList = new Rect(inner.x(), status.y() - GAP - visiblePicksHeight, inner.width(), visiblePicksHeight);
            bodyBottom = pickList.y() - GAP;
        }
        body = new Rect(inner.x(), bodyY, inner.width(), Math.max(0, bodyBottom - bodyY));
    }

    private void addNavigation() {
        if (job != null && RerollMethod.showToggle()) {
            method = addRenderableWidget(FitButton.create(methodBounds(), RerollMethod.label(), pressed -> {
                RerollMethod.toggle();
                updateControls();
            }));
        }
        int x = methodBounds().right() + GAP;
        Rect links = new Rect(x, navigation.y(), navigation.right() - x, navigation.height());
        if (villager != null && job != null) {
            queue = addRenderableWidget(FitButton.create(links.column(0, 3), message("button.queue"), pressed -> {
                if (queueHint() == null) {
                    useTargets();
                    TradeHall.queueCurrent(minecraft, merchant, villager);
                    updateControls();
                }
            }));
        }
        hall = addRenderableWidget(FitButton.create(links.column(1, 3), message("button.hall", TradeHall.size()),
                pressed -> McBridge.setScreen(minecraft, parent instanceof TradeHallScreen ? parent : new TradeHallScreen(this))));
        FitButton.hint(hall, message("button.hall.tooltip"));
        Button saved = addRenderableWidget(FitButton.create(links.column(2, 3), message("button.list"),
                pressed -> McBridge.setScreen(minecraft, new MyVillagersScreen(this))));
        FitButton.hint(saved, message("button.list.tooltip"));
    }

    private Rect methodBounds() {
        return new Rect(navigation.x(), navigation.y(), navigation.width() * 3 / 8, navigation.height());
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

        int right = (villager == null ? footer.right() : rollBounds.x()) - GAP;
        Rect pageButtons = new Rect(right - 2 * BUTTON_HEIGHT - GAP, footer.y(), 2 * BUTTON_HEIGHT + GAP, footer.height());
        int labelX = footer.column(0, 4).right() + GAP;
        pageLabel = new Rect(labelX, footer.y(), pageButtons.x() - labelX - GAP, footer.height());
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
        rows = Math.max(1, (body.height() + (itemGrid ? spacing - 2 : 0)) / rowHeight);
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
                        !picked.getOrDefault(job.id().toString(), Map.of()).isEmpty(), false, () -> chooseJob(job))).toList();
                case ITEM -> items.stream().map(group -> new Choice(group.name(), group.icon(), null,
                        group.eligibility().canReroll()
                                ? group.entries().size() == 1 ? priceTip(group.entries().get(0)) : null
                                : ineligibleHint(group.eligibility()),
                        group.entries().stream().anyMatch(entry -> picksForJob().containsKey(entry.id)),
                        !group.eligibility().canReroll(), () -> chooseItem(group))).toList();
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
                slot.setLocked(choice.locked());
                FitButton.hint(slot, choice.hint());
                FitButton.select(slot, choice.picked());
            }
        }
        previous.visible = next.visible = pageCount > 1;
        previous.active = page() > 0;
        next.active = page() + 1 < pageCount;
    }

    private Choice choice(TradeEntry entry) {
        RerollEligibility eligibility = eligibility(entry.id);
        return new Choice(Component.literal(entry.name), entry.icon, TradeIcons.levelBadge(TradeIcons.bookLevel(entry.id)),
                eligibility.canReroll() ? priceTip(entry) : ineligibleHint(eligibility),
                picksForJob().containsKey(entry.id), !eligibility.canReroll(), () -> chooseTrade(entry));
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
        return job == null ? Map.of() : picked.getOrDefault(job.id().toString(), Map.of());
    }

    private void chooseJob(TradeIcons.Job choice) {
        selectJob(choice);
        show(Step.ITEM);
    }

    private void selectJob(TradeIcons.Job choice) {
        job = choice;
        saveFailed = false;
        catalog = TradeCatalog.browse(minecraft.level, job.id(), villager);
        pool = TradePools.from(catalog.novice(), catalog.offerCounts(), catalog.allowDuplicates());
        Map<String, List<TradeEntry>> groups = new LinkedHashMap<>();
        for (TradeCatalog.Offer offer : catalog.offers().values()) {
            TradeEntry entry = offer.entry();
            groups.computeIfAbsent(TradeEntry.itemId(entry.icon.getItem()), id -> new ArrayList<>()).add(entry);
        }
        List<ItemGroup> offers = new ArrayList<>();
        for (List<TradeEntry> entries : groups.values()) {
            entries.sort(Comparator.comparing((TradeEntry entry) -> entry.name).thenComparing(entry -> entry.id));
            TradeEntry first = entries.get(0);
            ItemStack icon = entries.size() == 1 ? first.icon : new ItemStack(first.icon.getItem());
            Component name = entries.size() == 1 && !icon.is(Items.ENCHANTED_BOOK)
                    ? Component.literal(first.name) : icon.getHoverName();
            RerollEligibility eligibility = entries.stream().map(entry -> eligibility(entry.id))
                    .min(Comparator.naturalOrder()).orElseThrow();
            offers.add(new ItemGroup(name, icon, entries, eligibility));
        }
        offers.sort(Comparator.comparing(offer -> offer.name().getString()));
        items = offers;
        pages[Step.ITEM.ordinal()] = 0;
        pickPage = 0;
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
        int saved = catalog.offers().get(trade.id).rule().fitPrice(picksForJob().getOrDefault(trade.id, 0));
        anyPrice = !trade.paysEmeralds || saved == TradeSession.ANY_PRICE;
        price = saved == 0 || anyPrice ? trade.emeraldMax : saved;
        saveFailed = false;
        show(Step.PRICE);
    }

    private void addPriceControls() {
        if (villager == null) {
            add = addRenderableWidget(FitButton.primary(rollBounds, message("browser.add"), pressed -> savePrice(false)));
        } else {
            int addWidth = footer.column(0, 4).width();
            Rect addBounds = new Rect(rollBounds.x() - GAP - addWidth, footer.y(), addWidth, footer.height());
            add = addRenderableWidget(FitButton.create(addBounds, message("browser.add"), pressed -> savePrice(false)));
            roll = addRenderableWidget(FitButton.primary(rollBounds, message("browser.add_roll"), pressed -> onRoll()));
        }
        if (trade.paysEmeralds && eligibility(trade.id).canReroll()) {
            Rect controls = priceControls();
            minus = addRenderableWidget(FitButton.create(new Rect(controls.x(), controls.y(), BUTTON_HEIGHT, BUTTON_HEIGHT),
                    Component.literal("−"), pressed -> adjust(-1)));
            plus = addRenderableWidget(FitButton.create(new Rect(controls.x() + 66, controls.y(), BUTTON_HEIGHT, BUTTON_HEIGHT),
                    Component.literal("+"), pressed -> adjust(1)));
            any = addRenderableWidget(FitButton.create(new Rect(controls.x() + 90, controls.y(), controls.width() - 90, BUTTON_HEIGHT),
                    message("any"), pressed -> {
                        anyPrice = !anyPrice;
                        updateControls();
                    }));
            FitButton.hint(minus, message("browser.shift_price"));
            FitButton.hint(plus, message("browser.shift_price"));
        }
    }

    private Rect priceControls() {
        return new Rect(body.right() - PRICE_WIDTH, body.y() + 23, PRICE_WIDTH, BUTTON_HEIGHT);
    }

    private void adjust(int direction) {
        //? if >=1.21.9 {
        boolean shift = minecraft.hasShiftDown();
        //?} else {
        /*boolean shift = hasShiftDown();
        *///?}
        price = Math.max(trade.emeraldMin, Math.min(trade.emeraldMax, price + (shift ? 5 : 1) * direction));
        updateControls();
    }

    private int ceiling() {
        return anyPrice ? TradeSession.ANY_PRICE : price;
    }

    private void reloadPicks() {
        picked = ProfessionPicks.load(PickFiles.configFile()).snapshot();
        pickIds = picksForJob().keySet().stream()
                .sorted(Comparator.comparing((String id) -> !eligibility(id).canReroll())
                        .thenComparing(this::pickName).thenComparing(Comparator.naturalOrder())).toList();
        targets = job == null ? Map.of() : catalog.targets(picksForJob());
        estimate = null;
        estimatePace = 0;
        if (villager != null && job != null && !busy()) {
            useTargets();
        }
    }

    private void useTargets() {
        TradeSession.load(job.id().toString(), catalog, picksForJob());
    }

    private RerollEligibility eligibility(String id) {
        TradeCatalog.Offer offer = catalog.offers().get(id);
        return offer == null ? RerollEligibility.UNAVAILABLE : offer.eligibility();
    }

    private static Component ineligibleHint(RerollEligibility eligibility) {
        return eligibility == RerollEligibility.UNAVAILABLE ? message("browser.unavailable")
                : message("browser.level_only", Component.translatable(eligibility.levelKey()));
    }

    private String pickName(String id) {
        TradeCatalog.Offer offer = catalog.offers().get(id);
        return offer == null ? VillagerRoster.prettyTrade(id) : offer.entry().name;
    }

    private void addPickControls() {
        int lastPage = Math.max(0, (pickIds.size() - 1) / picksPerPage);
        pickPage = Math.min(pickPage, lastPage);
        if (lastPage > 0) {
            int right = pickList.right();
            int y = pickList.y();
            Button back = addRenderableWidget(FitButton.create(new Rect(right - 2 * PICK_HEADER - GAP, y, PICK_HEADER, PICK_HEADER),
                    Component.literal("«"), pressed -> changePickPage(-1)));
            Button forward = addRenderableWidget(FitButton.create(new Rect(right - PICK_HEADER, y, PICK_HEADER, PICK_HEADER),
                    Component.literal("»"), pressed -> changePickPage(1)));
            back.active = pickPage > 0;
            forward.active = pickPage < lastPage;
            FitButton.hint(back, message("browser.previous_picks"));
            FitButton.hint(forward, message("browser.next_picks"));
        }
        for (int i = 0; i < picksPerPage && firstPick() + i < pickIds.size(); i++) {
            String id = pickIds.get(firstPick() + i);
            Rect row = pickRow(i);
            Button remove = addRenderableWidget(FitButton.create(new Rect(row.right() - PICK_ROW, row.y(), PICK_ROW, PICK_ROW),
                    Component.literal("×"), pressed -> writePick(id, 0)));
            FitButton.hint(remove, message("browser.remove_pick", pickName(id)));
        }
    }

    private int firstPick() {
        return pickPage * picksPerPage;
    }

    private Rect pickRow(int i) {
        return new Rect(pickList.x(), pickList.y() + PICK_HEADER + i * PICK_ROW, pickList.width(), PICK_ROW);
    }

    private boolean changePickPage(int direction) {
        int nextPage = pickPage + direction;
        if (nextPage < 0 || nextPage * picksPerPage >= pickIds.size()) {
            return false;
        }
        pickPage = nextPage;
        rebuildWidgets();
        return true;
    }

    private StopReason merchantBlocker() {
        if (!hasMerchant() || !villager.isAlive() || LibrarianTracker.current(minecraft) != villager) {
            return StopReason.VILLAGER_GONE;
        }
        if (!McBridge.withinEntity(minecraft.player, villager)) {
            return StopReason.TOO_FAR;
        }
        if (McBridge.unemployed(villager) || McBridge.nitwit(villager)) {
            return StopReason.NO_PROFESSION;
        }
        return merchant.getMenu().getOffers().isEmpty() ? StopReason.NO_OFFERS : null;
    }

    private boolean hasMerchant() {
        return merchant != null && minecraft.player != null && minecraft.player.containerMenu == merchant.getMenu();
    }

    private Component queueHint() {
        if (busy()) {
            return message("ui.busy");
        }
        StopReason blocker = merchantBlocker();
        return blocker != null ? blockerHint(blocker) : targets.isEmpty() ? targetHint() : null;
    }

    private Component targetHint() {
        return message(pickIds.isEmpty() ? "need_target" : "novice_targets_only");
    }

    private Component startHint(boolean includePrice) {
        if (!AutomatorConfig.get().enabled()) {
            return message("disabled");
        }
        StopReason blocker = merchantBlocker();
        if (blocker != null) {
            return blockerHint(blocker);
        }
        if (TradeHall.menuLocked(merchant.getMenu())) {
            return message("locked");
        }
        if (!includePrice && targets.isEmpty()) {
            return targetHint();
        }
        if (RerollMethod.usesTradeCycling() || ServerRerollClient.available() || matchesCurrentOffers(includePrice)) {
            return null;
        }
        if (jobSiteCheckTicks <= 0) {
            BlockPos site = JobSiteFinder.find(minecraft, villager);
            jobSiteBlocker = site == null ? StopReason.NO_JOB_SITE
                    : McBridge.withinBlock(minecraft.player, site) ? null : StopReason.OUT_OF_REACH;
            jobSiteCheckTicks = 10;
        }
        return jobSiteBlocker == null ? null : blockerHint(jobSiteBlocker);
    }

    private boolean matchesCurrentOffers(boolean includePrice) {
        Map<String, Integer> check = targets;
        if (includePrice) {
            check = new LinkedHashMap<>(targets);
            check.put(trade.id, ceiling());
        }
        return TradeSession.matches(merchant.getMenu().getOffers(), catalog.novice(), check);
    }

    private static Component blockerHint(StopReason blocker) {
        return switch (blocker) {
            case NO_OFFERS -> message("panel.waiting");
            case NO_JOB_SITE, OUT_OF_REACH -> message("browser.no_job_in_reach");
            default -> message(blocker.id());
        };
    }

    @Override
    public void tick() {
        if (jobSiteCheckTicks > 0) {
            jobSiteCheckTicks--;
        }
        updateControls();
    }

    private void updateControls() {
        boolean running = busy();
        boolean priceStep = step == Step.PRICE;
        RerollEligibility eligibility = priceStep ? eligibility(trade.id) : RerollEligibility.NOVICE;
        Component priceHint = eligibility.canReroll() ? null : ineligibleHint(eligibility);
        if (add != null) {
            boolean unchanged = picksForJob().getOrDefault(trade.id, 0) == ceiling();
            add.active = priceHint == null && !unchanged;
            add.setMessage(message(unchanged && priceHint == null ? "browser.added" : "browser.add"));
            FitButton.hint(add, priceHint);
            if (minus != null) {
                minus.active = !anyPrice && price > trade.emeraldMin;
                plus.active = !anyPrice && price < trade.emeraldMax;
                FitButton.select(any, anyPrice);
            }
        }
        if (roll != null) {
            runHint = running ? null : priceHint != null ? priceHint : startHint(priceStep);
            roll.active = runHint == null;
            roll.setMessage(message(running ? "button.stop" : priceStep ? "browser.add_roll" : "browser.start"));
            FitButton.hint(roll, runHint);
        }
        if (queue != null) {
            Component hint = queueHint();
            queue.active = hint == null;
            FitButton.hint(queue, hint == null ? message("button.queue.tooltip") : hint);
        }
        hall.active = !running || parent instanceof TradeHallScreen;
        hall.setMessage(message("button.hall", TradeHall.size()));
        if (method != null) {
            method.active = RerollMethod.canChoose() && !running;
            method.setMessage(RerollMethod.label());
            FitButton.hint(method, !RerollMethod.canChoose() ? RerollMethod.unsupportedTooltip() : running ? message("ui.busy")
                    : message(RerollMethod.usesTradeCycling() ? "method.trade_cycling.tooltip" : "method.job_site.tooltip"));
        }
        int pace = JobSiteReroller.ticksPerAttempt();
        if (pace != estimatePace) {
            estimatePace = pace;
            estimate = targets.isEmpty() || pool.listings().isEmpty() ? null : TradeOdds.estimate(pool, targets, pace);
        }
    }

    private void onRoll() {
        if (busy()) {
            JobSiteReroller.onToggleKey();
            return;
        }
        jobSiteCheckTicks = 0;
        updateControls();
        if (runHint != null) {
            return;
        }
        if (step == Step.PRICE) {
            savePrice(true);
        } else {
            startRolling();
        }
    }

    private void savePrice(boolean start) {
        if (!eligibility(trade.id).canReroll()) {
            return;
        }
        if (picksForJob().getOrDefault(trade.id, 0) != ceiling() && !writePick(trade.id, ceiling())) {
            return;
        }
        if (start) {
            startRolling();
        }
    }

    private boolean writePick(String id, int ceiling) {
        saveFailed = !PickFiles.writePick(job.id().toString(), id, ceiling);
        if (!saveFailed) {
            rebuildWidgets();
        }
        return !saveFailed;
    }

    private void startRolling() {
        if (busy() || startHint(false) != null) {
            return;
        }
        useTargets();
        McBridge.setScreen(minecraft, merchant);
        JobSiteReroller.start(minecraft);
    }

    private void show(Step next) {
        step = next;
        rebuildWidgets();
    }

    private void closeToParent() {
        McBridge.setScreen(minecraft, merchant == null || hasMerchant() ? parent : null);
    }

    @Override
    public void onClose() {
        switch (step) {
            case JOB -> closeToParent();
            case ITEM -> {
                if (villager != null) {
                    closeToParent();
                } else {
                    job = null;
                    show(Step.JOB);
                }
            }
            case LEVEL -> show(Step.ITEM);
            case PRICE -> show(item.entries().size() == 1 ? Step.ITEM : Step.LEVEL);
        }
    }

    @Override
    protected List<Component> paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.panel(canvas, frame);
        Rect titleBox = new Rect(breadcrumb.x(), frame.y() + PADDING, breadcrumb.width(), LINE_HEIGHT);
        GuiDraw.label(canvas, title, titleBox, TEXT, mouseX, mouseY);
        Rect path = breadcrumb;
        if (job != null) {
            canvas.item(job.icon(), breadcrumb.x(), breadcrumb.y() + 1);
            if (new Rect(breadcrumb.x(), breadcrumb.y(), SLOT, SLOT).contains(mouseX, mouseY)) {
                canvas.tip(List.of(job.icon().getHoverName()), mouseX, mouseY);
            }
            path = new Rect(breadcrumb.x() + SLOT + GAP, breadcrumb.y(), breadcrumb.width() - SLOT - GAP, breadcrumb.height());
        }
        GuiDraw.label(canvas, breadcrumbText(), path, MUTED, mouseX, mouseY);
        String promptKey = switch (step) {
            case JOB -> "browser.choose_job";
            case ITEM -> "browser.choose_item";
            case LEVEL -> showingBooks() ? "browser.choose_level" : "browser.choose_offer";
            case PRICE -> !eligibility(trade.id).canReroll() ? "browser.offer_details"
                    : trade.paysEmeralds ? "browser.max_price" : "browser.payout";
        };
        GuiDraw.label(canvas, message(promptKey), prompt, TEXT, mouseX, mouseY);
        if (step == Step.PRICE) {
            paintPrice(canvas, mouseX, mouseY);
        } else {
            paintGrid(canvas, mouseX, mouseY);
        }
        if (job != null) {
            paintPicks(canvas, mouseX, mouseY);
            paintStatus(canvas, mouseX, mouseY);
            if (method == null) {
                GuiDraw.label(canvas, message("panel.method", RerollMethod.label()), methodBounds(), MUTED, mouseX, mouseY);
            }
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
            GuiDraw.label(canvas, message("ui.page", page() + 1, pageCount), pageLabel, MUTED, mouseX, mouseY);
        }
    }

    private void paintPrice(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        GuiDraw.well(canvas, body);
        Rect icon = new Rect(body.x() + GAP, body.y() + GAP, SLOT, SLOT);
        GuiDraw.frame(canvas, icon, BORDER, RAISED);
        RerollEligibility eligibility = eligibility(trade.id);
        GuiDraw.tradeIcon(canvas, trade.icon, icon, TradeIcons.levelBadge(TradeIcons.bookLevel(trade.id)), !eligibility.canReroll());
        Rect name = new Rect(icon.right() + GAP, icon.y(), body.right() - icon.right() - 2 * GAP, SLOT);
        GuiDraw.label(canvas, Component.literal(trade.name), name, eligibility.canReroll() ? TEXT : MUTED, mouseX, mouseY);
        if (!eligibility.canReroll()) {
            Rect detail = new Rect(body.x() + GAP, icon.bottom() + GAP, body.width() - 2 * GAP, 2 * LINE_HEIGHT);
            GuiDraw.hint(canvas, ineligibleHint(eligibility), detail, MUTED, mouseX, mouseY);
            return;
        }

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
    }

    private void paintPicks(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        Rect heading = new Rect(pickList.x(), pickList.y(), pickList.width() - 2 * PICK_HEADER - GAP, PICK_HEADER);
        Component caption = message(pickIds.isEmpty() ? "panel.no_picks" : "browser.picks", pickIds.size());
        GuiDraw.label(canvas, caption, heading, pickIds.isEmpty() ? MUTED : TEXT, mouseX, mouseY);
        for (int i = 0; i < picksPerPage && firstPick() + i < pickIds.size(); i++) {
            String id = pickIds.get(firstPick() + i);
            Rect row = pickRow(i);
            boolean eligible = eligibility(id).canReroll();
            TradeCatalog.Offer offer = catalog.offers().get(id);
            ItemStack icon = offer == null ? TradeIcons.forTrade(id) : offer.entry().icon;
            GuiDraw.frame(canvas, row, BORDER, eligible ? RAISED : BACKGROUND);
            GuiDraw.tradeIcon(canvas, icon, new Rect(row.x() + 2, row.y(), SLOT, row.height()), null, !eligible);
            int max = targets.getOrDefault(id, picksForJob().get(id));
            Component priceLabel = max == TradeSession.ANY_PRICE ? message("any") : message("browser.pick_price", max);
            if (offer != null && !offer.entry().paysEmeralds) {
                priceLabel = message("browser.sell_price", offer.entry().emeraldMin);
            }
            int priceWidth = Math.min(canvas.font().width(priceLabel) + GAP, TradeNameLayout.priceBudget(row.width()));
            int priceX = TradeNameLayout.priceX(row.x(), row.width(), priceWidth);
            Rect name = new Rect(TradeNameLayout.nameLeft(row.x()), row.y(),
                    TradeNameLayout.availableWidth(row.width(), priceWidth), row.height());
            GuiDraw.label(canvas, Component.literal(pickName(id)), name, eligible ? TEXT : MUTED, mouseX, mouseY);
            GuiDraw.label(canvas, priceLabel, new Rect(priceX, row.y(), priceWidth, row.height()), MUTED, mouseX, mouseY);
            if (!eligible && row.contains(mouseX, mouseY)) {
                canvas.tip(List.of(Component.literal(pickName(id)), ineligibleHint(eligibility(id))), mouseX, mouseY);
            }
        }
    }

    public List<Component> oddsLines() {
        if (estimate == null) {
            return List.of();
        }
        if (estimate.impossible()) {
            return List.of(message("odds.impossible"));
        }
        return List.of(message("odds.attempts", estimate.oneInText()), message("odds.time", estimate.timeText()));
    }

    private void paintStatus(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        StopReason result = villager == null ? null : JobSiteReroller.stopReasonFor(villager);
        if (result == StopReason.FOUND && (!hasMerchant() || !matchesCurrentOffers(false))) {
            result = null;
        }
        UiStatus tone = UiStatus.forRun(busy(), !targets.isEmpty(), result);
        Component heading;
        Component detail = null;
        if (saveFailed) {
            tone = UiStatus.ERROR;
            heading = message("ui.save_failed");
        } else if (TradeHall.isActive()) {
            tone = TradeHall.hudStatus();
            heading = TradeHall.hudTitle();
            detail = TradeHall.hudDetail();
        } else if (JobSiteReroller.isRunning()) {
            heading = JobSiteReroller.status();
        } else if (result != null) {
            heading = message(switch (result) {
                case FOUND -> "status.matched";
                case STOPPED, PANIC, GUI_CLOSED, ATTEMPT_CAP, DISABLED -> "status.stopped";
                default -> "status.failed";
            });
            if (result != StopReason.FOUND) {
                detail = message(result.id());
            }
        } else if (runHint != null && !targets.isEmpty() && step != Step.PRICE) {
            tone = UiStatus.ERROR;
            heading = message("status.blocked");
            detail = runHint;
        } else {
            heading = message(targets.isEmpty() ? "status.idle" : "status.ready");
            if (targets.isEmpty() && !pickIds.isEmpty()) {
                detail = targetHint();
            }
        }
        if (detail == null && !busy()) {
            List<Component> odds = oddsLines();
            if (odds.size() == 1) {
                detail = odds.get(0);
            } else if (odds.size() == 2) {
                detail = message("browser.odds_eta", odds.get(0), odds.get(1));
            }
        }
        GuiDraw.label(canvas, heading, new Rect(status.x(), status.y(), status.width(), LINE_HEIGHT),
                tone.color(), mouseX, mouseY);
        if (detail != null) {
            GuiDraw.label(canvas, detail, new Rect(status.x(), status.y() + LINE_HEIGHT, status.width(), LINE_HEIGHT),
                    MUTED, mouseX, mouseY);
        }
    }

    private boolean scroll(double x, double y, double delta) {
        if (delta == 0) {
            return false;
        }
        int direction = delta < 0 ? 1 : -1;
        if (job != null && pickList.contains(x, y)) {
            return changePickPage(direction);
        }
        return step != Step.PRICE && body.contains(x, y) && turn(direction);
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

    private record ItemGroup(Component name, ItemStack icon, List<TradeEntry> entries, RerollEligibility eligibility) {
    }

    private record BookRow(Component name, List<TradeEntry> entries) {
    }

    private record Choice(Component name, ItemStack icon, String badge, Component hint, boolean picked, boolean locked, Runnable action) {
    }
}
