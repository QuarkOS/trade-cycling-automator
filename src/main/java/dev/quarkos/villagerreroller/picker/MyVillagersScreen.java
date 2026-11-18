package dev.quarkos.villagerreroller.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import dev.quarkos.villagerreroller.picker.UiLayout.ChecklistSections;
import dev.quarkos.villagerreroller.picker.UiLayout.Rect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.19.3 {
/*import net.minecraft.core.Registry;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.quarkos.villagerreroller.picker.GuiDraw.MUTED;
import static dev.quarkos.villagerreroller.picker.GuiDraw.TEXT;
import static dev.quarkos.villagerreroller.picker.GuiDraw.message;
import static dev.quarkos.villagerreroller.picker.UiLayout.BUTTON_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;
import static dev.quarkos.villagerreroller.picker.UiLayout.LINE_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.PADDING;
import static dev.quarkos.villagerreroller.picker.UiLayout.ROW_HEIGHT;
import static dev.quarkos.villagerreroller.picker.UiLayout.SCROLLBAR_SPACE;

/** Per-world checklist of locked villagers and wanted trades. */
public final class MyVillagersScreen extends Screen {
    private static final int WANTED_ROW = BUTTON_HEIGHT;
    private static final int REMOVE_WIDTH = 48;
    private final Screen parent;
    private final ListScroll villagersBar = new ListScroll();
    private final ListScroll wantedBar = new ListScroll();
    private final List<Button> filterButtons = new ArrayList<>();
    private final List<Button> removeButtons = new ArrayList<>();
    private String[] removeIds;
    private String query = "";
    private Filter filter = Filter.ALL;
    private Button hallButton;
    private Button clearButton;
    private Rect frame;
    private Rect header;
    private Rect body;
    private Rect footer;
    private Rect villagersWell = new Rect(0, 0, 0, 0);
    private Rect wantedWell = new Rect(0, 0, 0, 0);
    private int villagersCount;
    private int villagersVisible;
    private int wantedCount;
    private int wantedVisible;
    private List<Component> hover = List.of();

    public MyVillagersScreen(Screen parent) {
        super(message("checklist.title"));
        this.parent = parent;
    }

    public Screen parent() {
        return parent;
    }

    public void scrollTo(int index) {
        villagersBar.setOffset(index);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            McBridge.setScreen(minecraft, parent);
        }
    }

    @Override
    protected void init() {
        frame = UiLayout.frame(width, height, 424, 280);
        Rect inner = frame.inset(PADDING);
        Rect top = new Rect(inner.x(), inner.y(), inner.width(), BUTTON_HEIGHT);
        int hallWidth = Math.min(104, top.width() / 3);
        header = new Rect(top.x(), top.y(), top.width() - hallWidth - GAP, top.height());
        hallButton = addRenderableWidget(FitButton.create(
                new Rect(top.right() - hallWidth, top.y(), hallWidth, BUTTON_HEIGHT),
                message("button.hall_queue", TradeHall.size()), pressed -> openHall()));
        FitButton.hint(hallButton, message("button.hall.tooltip"));

        int toolbarY = top.bottom() + GAP;
        int searchWidth = Math.min(112, top.width() / 4);
        Rect searchBounds = new Rect(top.x(), toolbarY, searchWidth, BUTTON_HEIGHT);
        SearchBox search = new SearchBox(font, searchBounds, message("checklist.search"), () -> {});
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            villagersBar.setOffset(0);
        });
        addRenderableWidget(search);
        Rect filters = new Rect(searchBounds.right() + GAP, toolbarY, inner.width() - searchWidth - GAP, BUTTON_HEIGHT);
        filterButtons.clear();
        for (Filter value : Filter.values()) {
            Button button = addRenderableWidget(FitButton.create(filters.column(value.ordinal(), Filter.values().length),
                    message("checklist.filter." + value.key, 0), pressed -> {
                        filter = value;
                        villagersBar.setOffset(0);
                    }));
            filterButtons.add(button);
        }

        footer = new Rect(inner.x(), inner.bottom() - BUTTON_HEIGHT, inner.width(), BUTTON_HEIGHT);
        int bodyY = filters.bottom() + GAP;
        body = new Rect(inner.x(), bodyY, inner.width(), footer.y() - GAP - bodyY);
        addRemoveButtons();
        Rect left = footer.column(0, 3);
        clearButton = addRenderableWidget(FitButton.create(
                new Rect(left.x(), left.y(), Math.min(86, left.width()), BUTTON_HEIGHT),
                message("checklist.clear_dead"), pressed -> clearDead()));
        FitButton.hint(clearButton, message("checklist.clear_dead.tooltip"));
        addRenderableWidget(FitButton.primary(footer.column(2, 3), Component.translatable("gui.done"), pressed -> onClose()));
    }

    private void openHall() {
        if (minecraft != null) {
            McBridge.setScreen(minecraft, parent instanceof TradeHallScreen ? parent : new TradeHallScreen(this));
        }
    }

    private void clearDead() {
        if (minecraft == null) {
            return;
        }
        //? if >=1.21.9 {
        if (minecraft.hasShiftDown()) {
        //?} else {
        /*if (hasShiftDown()) {
        *///?}
            VillagerRosterClient.clearWorld(minecraft);
        } else {
            VillagerRosterClient.pruneDeadLoadedVillagers(minecraft);
        }
        villagersBar.setOffset(0);
    }

    private void addRemoveButtons() {
        removeButtons.clear();
        removeIds = new String[Math.max(0, body.height() / ROW_HEIGHT)];
        for (int i = 0; i < removeIds.length; i++) {
            int row = i;
            Button button = addRenderableWidget(FitButton.create(new Rect(0, 0, REMOVE_WIDTH, BUTTON_HEIGHT),
                    message("button.remove"), pressed -> removeVillager(row)));
            button.visible = false;
            button.active = false;
            removeButtons.add(button);
        }
    }

    private void removeVillager(int row) {
        String id = removeIds[row];
        if (minecraft != null && id != null) {
            VillagerRosterClient.roster().remove(VillagerRosterClient.currentWorldKey(minecraft), id);
            VillagerRosterClient.roster().save(VillagerRosterFiles.configFile());
            removeIds[row] = null;
            removeButtons.get(row).active = false;
        }
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        paint(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        canvas.tip(hover, mouseX, mouseY);
    }
    *///?}

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        hover = List.of();
        GuiDraw.panel(canvas, frame);
        GuiDraw.line(canvas, title.getString(), header, TEXT);
        if (header.contains(mouseX, mouseY)) {
            Component key = AutomatorKeys.CHECKLIST == null
                    ? Component.literal("V") : AutomatorKeys.CHECKLIST.getTranslatedKeyMessage();
            hover = List.of(title, message("checklist.key_hint", key));
        }
        hallButton.setMessage(message("button.hall_queue", TradeHall.size()));

        List<VillagerRoster.Entry> roster = roster();
        List<VillagerRoster.WantedCheck> checks = VillagerRoster.evaluateWanted(
                ProfessionPicks.load(PickFiles.configFile()), roster, null);
        List<VillagerRoster.WantedCheck> needed = checks.stream()
                .filter(check -> check.state() != VillagerRoster.WantedState.LOCKED_IN).toList();
        int librarians = professionCount(roster, true);
        int[] counts = {roster.size(), neededVillagers(roster, checks), librarians, roster.size() - librarians};
        for (Filter value : Filter.values()) {
            Button button = filterButtons.get(value.ordinal());
            button.setMessage(message("checklist.filter." + value.key, counts[value.ordinal()]));
            FitButton.select(button, value == filter);
        }

        clearButton.active = !roster.isEmpty();
        for (Button button : removeButtons) {
            button.visible = false;
            button.active = false;
        }
        ChecklistSections sections = ChecklistSections.place(body, needed.size());
        paintWanted(canvas, sections.wanted(), needed, checks.size(), mouseX, mouseY);
        List<Indexed> rows = filtered(roster, checks);
        paintVillagers(canvas, sections.villagers(), roster.size(), rows, checks, mouseX, mouseY);
        if (!rows.isEmpty()) {
            Rect range = footer.column(1, 3);
            GuiDraw.label(canvas, message("ui.showing", villagersBar.offset() + 1,
                    Math.min(villagersBar.offset() + villagersVisible, rows.size()), rows.size()), range, MUTED, mouseX, mouseY);
        }
    }

    private void paintWanted(GuiDraw.Canvas canvas, Rect section, List<VillagerRoster.WantedCheck> needed,
                             int total, int mouseX, int mouseY) {
        int matched = total - needed.size();
        Component heading = total == 0 ? message("checklist.wanted")
                : message("checklist.wanted_progress", matched, total);
        Rect title = new Rect(section.x(), section.y(), section.width(), LINE_HEIGHT);
        GuiDraw.label(canvas, heading, title, matched == total && total > 0 ? UiStatus.READY.color() : TEXT, mouseX, mouseY);
        wantedWell = sectionWell(section);
        GuiDraw.well(canvas, wantedWell);
        wantedCount = needed.size();
        wantedVisible = wantedWell.height() / WANTED_ROW;
        wantedBar.clamp(ListScroll.maxOffset(wantedCount, wantedVisible));
        if (needed.isEmpty()) {
            Component label = message(total == 0 ? "checklist.no_wanted" : "checklist.all_matched");
            Component hint = message(total == 0 ? "checklist.pick_targets" : "checklist.all_locked", total);
            GuiDraw.empty(canvas, wantedWell, label, hint, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < wantedVisible && wantedBar.offset() + i < wantedCount; i++) {
            int index = wantedBar.offset() + i;
            Rect row = listRow(wantedWell, WANTED_ROW, i);
            VillagerRoster.WantedCheck check = needed.get(index);
            boolean overCap = check.state() == VillagerRoster.WantedState.OVER_CAP;
            boolean hovered = row.contains(mouseX, mouseY);
            GuiDraw.row(canvas, row, index, UiStatus.RUNNING, hovered);

            Rect text = new Rect(row.x() + PADDING, row.y(), row.width() - PADDING - GAP, row.height());
            int statusWidth = text.width() * 2 / 5;
            Rect status = new Rect(text.right() - statusWidth, text.y(), statusWidth, text.height());
            Rect name = new Rect(text.x(), text.y(), text.width() - statusWidth - GAP, text.height());
            GuiDraw.line(canvas, check.displayName(), name, TEXT);
            GuiDraw.line(canvas, message(overCap ? "checklist.over_cap" : "checklist.missing").getString(),
                    status, UiStatus.RUNNING.color());
            if (hovered) {
                Component price = check.targetMax() == Integer.MAX_VALUE ? message("any") : message("checklist.want_cap", check.targetMax());
                Component detail = overCap ? message("checklist.over_cap_status", check.bestCost(), check.bestVillagerIndex())
                        : message("checklist.missing_status");
                hover = List.of(message("ui.with_detail", check.displayName(), professionName(check.profession())), price, detail);
            }
        }
        GuiDraw.scrollbar(canvas, track(wantedWell), wantedBar, wantedCount, wantedVisible);
    }

    private void paintVillagers(GuiDraw.Canvas canvas, Rect section, int total, List<Indexed> rows,
                                List<VillagerRoster.WantedCheck> checks, int mouseX, int mouseY) {
        Rect title = new Rect(section.x(), section.y(), section.width(), LINE_HEIGHT);
        GuiDraw.label(canvas, message("checklist.locked_count", total), title, TEXT, mouseX, mouseY);
        villagersWell = sectionWell(section);
        GuiDraw.well(canvas, villagersWell);
        villagersCount = rows.size();
        villagersVisible = villagersWell.height() / ROW_HEIGHT;
        villagersBar.clamp(ListScroll.maxOffset(villagersCount, villagersVisible));
        if (rows.isEmpty()) {
            GuiDraw.empty(canvas, villagersWell, message(total == 0 ? "checklist.empty.title" : "checklist.no_match"),
                    message(total == 0 ? "checklist.record_trade" : "checklist.clear_filters"), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < villagersVisible && villagersBar.offset() + i < villagersCount; i++) {
            int index = villagersBar.offset() + i;
            paintVillager(canvas, listRow(villagersWell, ROW_HEIGHT, i), rows.get(index), i, checks, mouseX, mouseY);
        }
        GuiDraw.scrollbar(canvas, track(villagersWell), villagersBar, villagersCount, villagersVisible);
    }

    private void paintVillager(GuiDraw.Canvas canvas, Rect row, Indexed indexed, int visible,
                               List<VillagerRoster.WantedCheck> checks, int mouseX, int mouseY) {
        VillagerRoster.Entry villager = indexed.entry;
        List<Component> trades = new ArrayList<>();
        List<UiStatus> states = new ArrayList<>();
        UiStatus tone = UiStatus.IDLE;
        for (VillagerRoster.LockedOffer offer : villager.trades()) {
            VillagerRoster.WantedCheck check = find(checks, villager.profession(), offer.tradeId());
            UiStatus state = check == null || check.bestVillagerIndex() != indexed.index ? UiStatus.IDLE
                    : check.state() == VillagerRoster.WantedState.LOCKED_IN ? UiStatus.READY
                    : check.state() == VillagerRoster.WantedState.OVER_CAP ? UiStatus.RUNNING : UiStatus.IDLE;
            trades.add(tradePiece(offer, check, state));
            states.add(state);
            if (state == UiStatus.READY || tone == UiStatus.IDLE) {
                tone = state;
            }
        }
        boolean hovered = row.contains(mouseX, mouseY);
        GuiDraw.row(canvas, row, indexed.index - 1, tone, hovered);
        canvas.item(icon(villager), row.x() + GAP + 2, row.y() + 6);
        Rect remove = new Rect(row.right() - REMOVE_WIDTH - GAP, row.y() + GAP, REMOVE_WIDTH, BUTTON_HEIGHT);
        Button button = removeButtons.get(visible);
        removeIds[visible] = villager.uuid();
        FitButton.place(button, remove.x(), remove.y());
        button.visible = true;
        button.active = true;

        String profession = professionName(villager.profession());
        Component name = message("checklist.villager_name", indexed.index,
                villager.customName() == null || villager.customName().isEmpty() ? profession : villager.customName());
        Component tier = Component.translatable("merchant.level." + Math.max(1, Math.min(5, villager.level())));
        Component identity = message("ui.with_detail", name, tier);
        String place = placeLabel(villager);
        int textX = row.x() + 26;
        int textWidth = remove.x() - GAP - textX;
        int titleWidth = Math.min(textWidth, canvas.font().width(identity));
        GuiDraw.line(canvas, identity.getString(), new Rect(textX, row.y() + 3, titleWidth, LINE_HEIGHT), TEXT);
        if (titleWidth + GAP < textWidth) {
            int placeX = textX + titleWidth + GAP;
            GuiDraw.line(canvas, place, new Rect(placeX, row.y() + 3, textWidth - titleWidth - GAP, LINE_HEIGHT), MUTED);
        }
        paintTradePreview(canvas, textX, row.y() + 15, textWidth, trades, states);

        if (hovered && !remove.contains(mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(identity);
            tip.add(message("ui.with_detail", profession, place));
            tip.addAll(trades);
            hover = tip;
        }
    }

    private void paintTradePreview(GuiDraw.Canvas canvas, int x, int y, int width,
                                   List<Component> trades, List<UiStatus> states) {
        int right = x + width;
        String separator = " · ";
        int separatorWidth = canvas.font().width(separator);
        for (int i = 0; i < trades.size() && x < right; i++) {
            if (i > 0) {
                if (right - x <= separatorWidth) {
                    break;
                }
                canvas.text(separator, x, y, MUTED);
                x += separatorWidth;
            }
            String shown = GuiDraw.ellipsis(canvas.font(), trades.get(i).getString(), right - x);
            canvas.text(shown, x, y, states.get(i).color());
            if (!shown.equals(trades.get(i).getString())) {
                break;
            }
            x += canvas.font().width(shown);
        }
    }

    private Component tradePiece(VillagerRoster.LockedOffer offer, VillagerRoster.WantedCheck check, UiStatus state) {
        String name = offer.displayName() == null || offer.displayName().isEmpty() ? offer.tradeId() : offer.displayName();
        if (!offer.paysEmeralds()) {
            return message("checklist.trade.sold", offer.itemCount(), name, offer.effectiveCost());
        }
        if (state == UiStatus.RUNNING) {
            return message("checklist.trade.over_cap", name, offer.effectiveCost(), check.targetMax());
        }
        if (state == UiStatus.READY) {
            return check.targetMax() == Integer.MAX_VALUE ? message("checklist.trade.matched", name, offer.effectiveCost())
                    : message("checklist.trade.matched_cap", name, offer.effectiveCost(), check.targetMax());
        }
        return message("checklist.trade.price", name, offer.effectiveCost());
    }

    private static Rect sectionWell(Rect section) {
        int y = section.y() + LINE_HEIGHT + GAP;
        return new Rect(section.x(), y, section.width(), Math.max(0, section.bottom() - y));
    }

    private static Rect listRow(Rect well, int height, int row) {
        return new Rect(well.x(), well.y() + row * height, well.width() - SCROLLBAR_SPACE, height);
    }

    private static Rect track(Rect well) {
        return new Rect(well.right() - SCROLLBAR_SPACE, well.y(), SCROLLBAR_SPACE, well.height());
    }

    private String placeLabel(VillagerRoster.Entry entry) {
        boolean here = this.minecraft != null && this.minecraft.level != null
                && VillagerRosterClient.dimensionId(this.minecraft.level).equals(entry.dimension());
        if (!here) {
            String dim = entry.dimension() == null ? "" : entry.dimension();
            int colon = dim.indexOf(':');
            if (colon >= 0) {
                dim = dim.substring(colon + 1);
            }
            return "(" + dim + " " + entry.x() + ", " + entry.y() + ", " + entry.z() + ")";
        }
        if (this.minecraft.player == null) {
            return "(" + entry.x() + ", " + entry.y() + ", " + entry.z() + ")";
        }
        double dx = entry.x() + 0.5 - this.minecraft.player.getX();
        double dy = entry.y() - this.minecraft.player.getY();
        double dz = entry.z() + 0.5 - this.minecraft.player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
        return "(" + entry.x() + ", " + entry.y() + ", " + entry.z() + " \u00b7 " + dist + "m)";
    }

    private ItemStack icon(VillagerRoster.Entry entry) {
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            if (offer.tradeId() != null && offer.tradeId().startsWith("enchant:")) {
                return new ItemStack(Items.ENCHANTED_BOOK);
            }
        }
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            ItemStack stack = item(offer.itemId());
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return new ItemStack(Items.EMERALD);
    }

    private ItemStack item(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier id;
        try {
            //? if <1.21 {
            /*id = new Identifier(itemId);
            *///?} else {
            id = Identifier.parse(itemId);
            //?}
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
        //? if <1.19.3 {
        /*Item item = Registry.ITEM.get(id);
        *///?}
        //? if >=1.19.3 && <1.21.2 {
        /*Item item = BuiltInRegistries.ITEM.get(id);
        *///?}
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.get(id).map(holder -> holder.value()).orElse(Items.AIR);
        //?}
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private List<VillagerRoster.Entry> roster() {
        if (this.minecraft == null) {
            return List.of();
        }
        return VillagerRosterClient.roster().entries(VillagerRosterClient.currentWorldKey(this.minecraft));
    }

    private List<Indexed> filtered(List<VillagerRoster.Entry> roster, List<VillagerRoster.WantedCheck> checks) {
        List<Indexed> rows = new ArrayList<>();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int index = 1;
        for (VillagerRoster.Entry entry : roster) {
            if (filter == Filter.LIBRARIAN && !isLibrarian(entry.profession())) {
                index++;
                continue;
            }
            if (filter == Filter.OTHER && isLibrarian(entry.profession())) {
                index++;
                continue;
            }
            if (filter == Filter.NEEDED && !isOverCap(entry, checks, index)) {
                index++;
                continue;
            }
            if (!needle.isEmpty() && !matches(entry, needle)) {
                index++;
                continue;
            }
            rows.add(new Indexed(index, entry));
            index++;
        }
        return rows;
    }

    private boolean matches(VillagerRoster.Entry entry, String needle) {
        if (entry.customName() != null && entry.customName().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (professionName(entry.profession()).toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if ((entry.x() + "," + entry.y() + "," + entry.z()).contains(needle)) {
            return true;
        }
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            if (offer.displayName() != null && offer.displayName().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
            if (offer.tradeId() != null && offer.tradeId().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private int neededVillagers(List<VillagerRoster.Entry> roster, List<VillagerRoster.WantedCheck> checks) {
        int count = 0;
        int index = 1;
        for (VillagerRoster.Entry entry : roster) {
            if (isOverCap(entry, checks, index)) {
                count++;
            }
            index++;
        }
        return count;
    }

    private int professionCount(List<VillagerRoster.Entry> roster, boolean librarian) {
        int count = 0;
        for (VillagerRoster.Entry entry : roster) {
            if (isLibrarian(entry.profession()) == librarian) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLibrarian(String profession) {
        return profession != null && profession.endsWith("librarian");
    }

    private static boolean isOverCap(VillagerRoster.Entry entry, List<VillagerRoster.WantedCheck> checks, int index) {
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            VillagerRoster.WantedCheck check = find(checks, entry.profession(), offer.tradeId());
            if (check != null && check.state() == VillagerRoster.WantedState.OVER_CAP && check.bestVillagerIndex() == index) {
                return true;
            }
        }
        return false;
    }

    private static VillagerRoster.WantedCheck find(List<VillagerRoster.WantedCheck> checks, String profession, String tradeId) {
        for (VillagerRoster.WantedCheck check : checks) {
            if (check.profession().equals(profession) && check.tradeId().equals(tradeId)) {
                return check;
            }
        }
        return null;
    }

    private static String professionName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0 && colon + 1 < id.length()) {
            path = id.substring(colon + 1);
        }
        return VillagerRoster.prettyTrade("item:minecraft:" + path);
    }

    private boolean click(double mouseX, double mouseY) {
        Rect rosterTrack = track(villagersWell);
        Rect neededTrack = track(wantedWell);
        if (rosterTrack.contains(mouseX, mouseY)) {
            return villagersBar.clickTrack(mouseY, rosterTrack.y(), rosterTrack.height(), villagersCount, villagersVisible);
        }
        return neededTrack.contains(mouseX, mouseY)
                && wantedBar.clickTrack(mouseY, neededTrack.y(), neededTrack.height(), wantedCount, wantedVisible);
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        if (villagersWell.contains(mouseX, mouseY)) {
            return villagersBar.wheel(vertical, villagersCount, villagersVisible);
        }
        return wantedWell.contains(mouseX, mouseY) && wantedBar.wheel(vertical, wantedCount, wantedVisible);
    }

    private boolean drag(double mouseY) {
        return villagersBar.drag(mouseY, villagersWell.y(), villagersWell.height(), villagersCount, villagersVisible)
                || wantedBar.drag(mouseY, wantedWell.y(), wantedWell.height(), wantedCount, wantedVisible);
    }

    private boolean submit(int key) {
        if (GuiDraw.enter(key) && getFocused() == null) {
            onClose();
            return true;
        }
        return false;
    }

    //? if <1.21.9 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && click(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drag(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        villagersBar.stop();
        wantedBar.stop();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AutomatorKeys.CHECKLIST != null && AutomatorKeys.CHECKLIST.matches(keyCode, scanCode)
                && !(getFocused() instanceof EditBox)) {
            VillagerReroller.openChecklist(this.minecraft);
            return true;
        }
        return submit(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && click(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (drag(event.y())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        villagersBar.stop();
        wantedBar.stop();
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AutomatorKeys.CHECKLIST != null && AutomatorKeys.CHECKLIST.matches(event)
                && !(getFocused() instanceof EditBox)) {
            VillagerReroller.openChecklist(this.minecraft);
            return true;
        }
        return submit(event.key()) || super.keyPressed(event);
    }
    //?}

    //? if <1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scrollBy(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (scrollBy(mouseX, mouseY, vertical)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    //?}

    private enum Filter {
        ALL("all"),
        NEEDED("needed"),
        LIBRARIAN("librarian"),
        OTHER("other");

        private final String key;

        Filter(String key) {
            this.key = key;
        }
    }

    private record Indexed(int index, VillagerRoster.Entry entry) {
    }
}
