package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
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

/**
 * Per-world checklist of locked villagers and the wanted trades still missing.
 */
public final class MyVillagersScreen extends Screen {
    private static final int ROW = 26;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final List<TabHit> tabs = new ArrayList<>();
    private EditBox search;
    private Button hallButton;
    private Button clearButton;
    private String query = "";
    private Filter filter = Filter.ALL;
    private int scroll;
    private int neededScroll;
    private boolean dragging;
    private boolean draggingNeeded;
    private int wellX;
    private int wellY;
    private int wellW;
    private int wellH;
    private int needX;
    private int needY;
    private int needW;
    private int needH;
    private int visibleRows;
    private int maxScroll;
    private List<Component> hover = List.of();

    public MyVillagersScreen(Screen parent) {
        super(Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.title"));
        this.parent = parent;
    }

    public Screen parent() {
        return parent;
    }

    public void scrollTo(int index) {
        this.scroll = Math.max(0, index);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            McBridge.setScreen(this.minecraft, parent);
        }
    }

    @Override
    protected void init() {
        Frame frame = frame();
        int hallW = Math.min(84, Math.max(40, frame.w / 4));
        hallButton = addRenderableWidget(FitButton.create(
                frame.left + frame.w - hallW - 6,
                frame.top + 4,
                hallW,
                14,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall_queue", TradeHall.size()),
                pressed -> {
                    if (this.minecraft != null) {
                        McBridge.setScreen(this.minecraft, new TradeHallScreen());
                    }
                }));
        int searchW = Math.min(122, Math.max(48, frame.w - 210));
        if (frame.w < 280) {
            searchW = Math.max(40, frame.w / 3);
        }
        search = new EditBox(this.font, frame.left + 8, frame.top + 20, searchW, 14,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.search"));
        //? if >=1.19.3 {
        String hint = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.search").getString();
        String shown = GuiDraw.ellipsis(this.font, hint, searchW - 8);
        search.setHint(Component.literal(shown));
        if (!shown.equals(hint)) {
            search.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(hint)));
        }
        //?}
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            scroll = 0;
        });
        addRenderableWidget(search);
        int buttonY = frame.top + frame.h - 22;
        int doneW = Math.min(66, Math.max(36, (frame.w - 24) / 4));
        clearButton = addRenderableWidget(FitButton.create(
                frame.left + frame.w - doneW * 2 - 12,
                buttonY,
                doneW,
                16,
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.clear_dead"),
                pressed -> {
                    if (this.minecraft == null) {
                        return;
                    }
                    //? if >=1.21.9 {
                    if (this.minecraft.hasShiftDown()) {
                    //?} else {
                    /*if (hasShiftDown()) {
                    *///?}
                        VillagerRosterClient.clearWorld(this.minecraft);
                    } else {
                        VillagerRosterClient.pruneDeadLoadedVillagers(this.minecraft);
                    }
                    scroll = 0;
                }));
        addRenderableWidget(FitButton.create(
                frame.left + frame.w - doneW - 6,
                buttonY,
                doneW,
                16,
                Component.translatable("gui.done"),
                pressed -> onClose()));
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(pose);
        paint(canvas, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    *///?}

    //? if >=1.20 && <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.Canvas canvas = GuiDraw.of(graphics);
        paint(canvas, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!hover.isEmpty()) {
            canvas.tip(hover, mouseX, mouseY);
        }
    }
    *///?}

    private void paint(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        hover = List.of();
        hits.clear();
        tabs.clear();
        Frame frame = frame();
        if (hallButton != null) {
            hallButton.setMessage(Component.translatable("text." + VillagerReroller.MOD_ID + ".button.hall_queue", TradeHall.size()));
        }
        List<VillagerRoster.Entry> roster = roster();
        List<VillagerRoster.WantedCheck> checks = VillagerRoster.evaluateWanted(
                ProfessionPicks.load(PickFiles.configFile()), roster, null);
        int neededCount = 0;
        int lockedIn = 0;
        List<VillagerRoster.WantedCheck> needed = new ArrayList<>();
        for (VillagerRoster.WantedCheck check : checks) {
            if (check.state() == VillagerRoster.WantedState.LOCKED_IN) {
                lockedIn++;
            } else {
                neededCount++;
                needed.add(check);
            }
        }
        if (clearButton != null) {
            clearButton.active = !roster.isEmpty();
        }
        GuiDraw.beveled(canvas, frame.left, frame.top, frame.w, frame.h);
        int hallLeft = hallButton == null ? frame.left + frame.w - 8 : widgetX(hallButton);
        String title = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.title").getString();
        String keyName = AutomatorKeys.CHECKLIST == null
                ? "V"
                : AutomatorKeys.CHECKLIST.getTranslatedKeyMessage().getString();
        String keyLabel = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.key_hint", keyName).getString();
        int titleW = Math.min(canvas.font().width(title), Math.max(20, hallLeft - frame.left - 16));
        String titleTip = GuiDraw.fit(canvas, title, frame.left + 8, frame.top + 7, titleW, GuiDraw.TEXT);
        int cursor = frame.left + 8 + canvas.font().width(GuiDraw.ellipsis(canvas.font(), title, titleW)) + 4;
        if (cursor + canvas.font().width(keyLabel) + 8 < hallLeft) {
            cursor += GuiDraw.pill(canvas, cursor, frame.top + 5, keyLabel, 0xFFDCDCDC, 0xFF707070, 0xFF404040) + 3;
        }
        cursor = drawCountPills(canvas, cursor, frame.top + 5, hallLeft - 4, roster.size(), lockedIn, checks.size(), neededCount);
        if (titleTip != null && mouseX >= frame.left + 8 && mouseX <= frame.left + 8 + titleW && mouseY >= frame.top + 4 && mouseY <= frame.top + 16) {
            hover = List.of(Component.literal(titleTip));
        }

        layoutTabs(canvas, frame, frame.left + frame.w - 6, mouseX, mouseY);
        int bodyTop = frame.top + 36;
        int bodyBottom = frame.top + frame.h - 26;
        boolean banner = neededCount == 0 && !checks.isEmpty();
        int sectionH;
        if (neededCount > 0) {
            sectionH = Math.min(52, Math.max(28, bodyBottom - bodyTop - ROW - 4));
        } else if (banner) {
            sectionH = 16;
        } else {
            sectionH = 0;
        }
        needX = frame.left + 8;
        needY = bodyTop;
        needW = frame.w - 16;
        needH = sectionH;
        if (neededCount > 0) {
            paintNeeded(canvas, needed, lockedIn, checks.size(), mouseX, mouseY);
        } else if (banner) {
            canvas.fill(needX, needY, needX + needW, needY + needH, 0xFFD7ECD9);
            canvas.fill(needX, needY, needX + 3, needY + needH, 0xFF2E7D32);
            String line = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.all_locked", checks.size()).getString();
            String tip = GuiDraw.fit(canvas, line, needX + 6, needY + 4, needW - 12, 0xFF1B5E20);
            if (tip != null && inside(mouseX, mouseY, needX, needY, needW, needH)) {
                hover = List.of(Component.literal(tip));
            }
        }
        int gap = sectionH == 0 ? 0 : 2;
        wellX = frame.left + 8;
        wellY = bodyTop + sectionH + gap;
        wellW = frame.w - 16;
        wellH = Math.max(20, bodyBottom - wellY);
        paintRoster(canvas, roster, checks, mouseX, mouseY);
        String footer = roster.isEmpty()
                ? Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.tip").getString()
                : Component.translatable(
                        "text." + VillagerReroller.MOD_ID + ".checklist.showing",
                        scroll + 1,
                        Math.min(scroll + Math.max(visibleRows, 1), filtered(roster, checks).size()),
                        filtered(roster, checks).size()).getString();
        int footerW = frame.w - 160;
        if (footerW < 20) {
            footerW = frame.w / 2;
        }
        String footerTip = GuiDraw.fit(canvas, footer, frame.left + 8, frame.top + frame.h - 16, footerW, 0xFF484848);
        if (footerTip != null && mouseY >= frame.top + frame.h - 20 && mouseX < frame.left + 8 + footerW) {
            hover = List.of(Component.literal(footerTip));
        }
    }

    private int drawCountPills(GuiDraw.Canvas canvas, int x, int y, int limit, int locked, int found, int total, int needed) {
        String lockedText = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.locked_count", locked).getString();
        int fill = locked == 0 ? 0xFFE0E0E0 : 0xFFD8E2DC;
        int border = locked == 0 ? 0xFF808080 : 0xFF52796F;
        int color = locked == 0 ? 0xFF404040 : 0xFF1B4332;
        if (x + canvas.font().width(lockedText) + 8 < limit) {
            x += GuiDraw.pill(canvas, x, y, lockedText, fill, border, color) + 3;
        }
        if (total > 0 && locked > 0) {
            String wanted = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.wanted_count", found, total).getString();
            if (x + canvas.font().width(wanted) + 8 < limit) {
                x += GuiDraw.pill(canvas, x, y, wanted, 0xFFD7ECD9, 0xFF2E7D32, 0xFF1B5E20) + 3;
            }
        }
        if (needed > 0) {
            String missing = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.needed_count", needed).getString();
            if (x + canvas.font().width(missing) + 8 < limit) {
                x += GuiDraw.pill(canvas, x, y, missing, 0xFFFFF0C2, 0xFFD48806, 0xFF9C4A00) + 3;
            }
        }
        return x;
    }

    private void layoutTabs(GuiDraw.Canvas canvas, Frame frame, int limit, int mouseX, int mouseY) {
        int searchRight = search == null ? frame.left + 8 : widgetX(search) + widgetW(search);
        int x = searchRight + 3;
        int y = frame.top + 20;
        Filter[] values = Filter.values();
        int[] counts = {roster().size(), neededVillagers(roster()), professionCount(roster(), true), professionCount(roster(), false)};
        String[] labels = {
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.filter.all", counts[0]).getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.filter.needed", counts[1]).getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.filter.librarian", counts[2]).getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.filter.other", counts[3]).getString()
        };
        int remaining = Math.max(0, limit - x);
        int[] preferred = {44, 62, 74, 53};
        int[] widths = new int[values.length];
        int natural = 0;
        int gap = 2;
        for (int i = 0; i < values.length; i++) {
            int text = canvas.font().width(labels[i]) + 8;
            int want = i < preferred.length ? Math.max(preferred[i], text) : text;
            widths[i] = want;
            natural += want;
        }
        int gaps = gap * Math.max(0, values.length - 1);
        if (natural + gaps > remaining && natural > 0) {
            int budget = Math.max(values.length * 16, remaining - gaps);
            int used = 0;
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(16, widths[i] * budget / natural);
                used += widths[i];
            }
            if (used > budget) {
                widths[widths.length - 1] = Math.max(16, widths[widths.length - 1] - (used - budget));
            }
        }
        for (int i = 0; i < values.length; i++) {
            if (x + 16 > limit) {
                break;
            }
            int width = Math.min(widths[i], limit - x);
            boolean selected = filter == values[i];
            canvas.fill(x, y, x + width, y + 14, selected ? 0xFF2E7D32 : 0xFF000000);
            canvas.fill(x + 1, y + 1, x + width - 1, y + 13, selected ? 0xFFD7ECD9 : 0xFFC6C6C6);
            String shown = GuiDraw.ellipsis(canvas.font(), labels[i], width - 4);
            canvas.text(shown, x + 2, y + 3, selected ? 0xFF1B5E20 : GuiDraw.BODY);
            tabs.add(new TabHit(values[i], x, y, width, 14, labels[i], shown));
            if (!shown.equals(labels[i]) && inside(mouseX, mouseY, x, y, width, 14)) {
                hover = List.of(Component.literal(labels[i]));
            }
            x += width + 2;
        }
    }

    private void paintNeeded(GuiDraw.Canvas canvas, List<VillagerRoster.WantedCheck> needed, int lockedIn, int total, int mouseX, int mouseY) {
        canvas.fill(needX, needY, needX + needW, needY + needH, 0xFFF5E6C4);
        canvas.fill(needX, needY, needX + needW, needY + 1, 0xFF8A5600);
        canvas.fill(needX, needY, needX + 1, needY + needH, 0xFF8A5600);
        canvas.fill(needX, needY + needH - 1, needX + needW, needY + needH, 0xFFFFF8E7);
        canvas.fill(needX + needW - 1, needY, needX + needW, needY + needH, 0xFFFFF8E7);
        canvas.fill(needX + 1, needY + 1, needX + 4, needY + needH - 1, 0xFFD48806);
        String header = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.still_needed_header", needed.size()).getString();
        String right = lockedIn == 0
                ? Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.none_locked", total).getString()
                : Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.locked_in_count", lockedIn).getString();
        int rightW = canvas.font().width(right);
        int headerMax = Math.max(10, needW - rightW - 16);
        GuiDraw.fit(canvas, header, needX + 7, needY + 3, headerMax, 0xFF8A3E00);
        canvas.text(right, needX + needW - rightW - 6, needY + 3, lockedIn == 0 ? 0xFF8A3E00 : 0xFF1B5E20);
        canvas.fill(needX + 6, needY + 12, needX + needW - 4, needY + 13, 0xFFD4A856);
        int rowH = 11;
        int rows = Math.max(1, (needH - 16) / rowH);
        int max = Math.max(0, needed.size() - rows);
        neededScroll = Math.max(0, Math.min(neededScroll, max));
        canvas.scissor(needX + 4, needY + 14, needX + needW - 2, needY + needH - 1);
        for (int i = 0; i < rows && neededScroll + i < needed.size(); i++) {
            VillagerRoster.WantedCheck check = needed.get(neededScroll + i);
            int y = needY + 14 + i * rowH;
            int fill = i % 2 == 0 ? 0xFFFFE3A3 : 0xFFFFEAC0;
            canvas.fill(needX + 5, y, needX + needW - 3, y + rowH - 1, fill);
            boolean over = check.state() == VillagerRoster.WantedState.OVER_CAP;
            canvas.text("!", needX + 7, y + 1, over ? 0xFF8A5600 : 0xFF9C2B00);
            String name = check.displayName();
            String profession = "\u00b7 " + professionName(check.profession());
            String cap = check.targetMax() == Integer.MAX_VALUE
                    ? Component.translatable("text." + VillagerReroller.MOD_ID + ".any").getString()
                    : Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.want_cap", check.targetMax()).getString();
            String status = over
                    ? Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.over_cap_status", check.bestCost(), check.bestVillagerIndex()).getString()
                    : Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.missing_status").getString();
            int nameMax = Math.max(16, needW / 5);
            String nameShown = GuiDraw.ellipsis(canvas.font(), name, nameMax);
            canvas.text(nameShown, needX + 13, y + 1, 0xFF201000);
            int px = needX + 13 + canvas.font().width(nameShown) + 4;
            String professionShown = GuiDraw.ellipsis(canvas.font(), profession, Math.max(8, needW / 6));
            if (px + canvas.font().width(professionShown) < needX + needW - 8) {
                canvas.text(professionShown, px, y + 1, 0xFF6B5328);
                px += canvas.font().width(professionShown) + 4;
            }
            if (px + canvas.font().width(cap) + 8 < needX + needW / 2 + 40) {
                px += GuiDraw.pill(canvas, px, y, cap, 0xFFFFF8E7, 0xFFC77700, 0xFF7A4200) + 4;
            }
            String statusShown = GuiDraw.ellipsis(canvas.font(), status, Math.max(8, needX + needW - 6 - px));
            canvas.text(statusShown, px, y + 1, over ? 0xFF8A5600 : 0xFF9C2B00);
            if (inside(mouseX, mouseY, needX + 5, y, needW - 8, rowH)
                    && (!nameShown.equals(name) || !statusShown.equals(status) || !professionShown.equals(profession))) {
                hover = List.of(Component.literal(name + " " + profession), Component.literal(cap + "  " + status));
            }
        }
        canvas.unscissor();
    }

    private void paintRoster(GuiDraw.Canvas canvas, List<VillagerRoster.Entry> roster, List<VillagerRoster.WantedCheck> checks, int mouseX, int mouseY) {
        GuiDraw.recessed(canvas, wellX, wellY, wellW, wellH, 0xFF8E8E8E);
        List<Indexed> rows = filtered(roster, checks);
        visibleRows = Math.max(1, (wellH - 4) / ROW);
        maxScroll = Math.max(0, rows.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (rows.isEmpty()) {
            if (roster.isEmpty()) {
                paintEmpty(canvas, mouseX, mouseY);
            } else {
                String none = Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.no_match").getString();
                String tip = GuiDraw.fit(canvas, none, wellX + 8, wellY + wellH / 2, wellW - 16, GuiDraw.MUTED);
                if (tip != null && inside(mouseX, mouseY, wellX, wellY, wellW, wellH)) {
                    hover = List.of(Component.literal(tip));
                }
            }
        } else {
            canvas.scissor(wellX + 1, wellY + 1, wellX + wellW - 8, wellY + wellH - 1);
            for (int i = 0; i < visibleRows && scroll + i < rows.size(); i++) {
                Indexed indexed = rows.get(scroll + i);
                int y = wellY + 2 + i * ROW;
                paintVillager(canvas, indexed, i, y, checks, mouseX, mouseY);
            }
            canvas.unscissor();
        }
        int travel = Math.max(1, wellH - 29);
        int barY = wellY + 1 + (maxScroll == 0 ? 0 : (int) (scroll / (float) maxScroll * travel));
        canvas.scroller(wellX + wellW - 7, barY, maxScroll == 0);
    }

    private void paintVillager(GuiDraw.Canvas canvas, Indexed indexed, int row, int y, List<VillagerRoster.WantedCheck> checks, int mouseX, int mouseY) {
        VillagerRoster.Entry entry = indexed.entry;
        boolean within = false;
        boolean over = false;
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            VillagerRoster.WantedCheck check = find(checks, entry.profession(), offer.tradeId());
            if (check == null) {
                continue;
            }
            if (check.state() == VillagerRoster.WantedState.OVER_CAP && check.bestVillagerIndex() == indexed.index) {
                over = true;
            } else if (check.state() == VillagerRoster.WantedState.LOCKED_IN && check.bestVillagerIndex() == indexed.index) {
                within = true;
            }
        }
        int stripe = within ? 0xFF2E7D32 : over ? 0xFFC77700 : 0xFF707070;
        int fill = over && !within ? 0xFFE2D6BE : (row % 2 == 0 ? 0xFFD4D4D4 : 0xFFCACACA);
        int rowW = wellW - 14;
        canvas.fill(wellX + 1, y, wellX + 1 + rowW, y + ROW - 1, fill);
        canvas.fill(wellX + 1, y, wellX + 4, y + ROW - 1, stripe);
        canvas.fill(wellX + 1, y + ROW - 1, wellX + 1 + rowW, y + ROW, 0xFF707070);
        ItemStack icon = icon(entry);
        canvas.item(icon, wellX + 6, y + 4);
        String name = entry.customName() == null || entry.customName().isEmpty()
                ? professionName(entry.profession()) + " #" + indexed.index
                : "#" + indexed.index + " \"" + entry.customName() + "\"";
        String tier = " \u00b7 " + professionName(entry.profession()) + " \u00b7 " + VillagerRoster.tierName(entry.level());
        String place = placeLabel(entry);
        int textX = wellX + 24;
        int removeX = wellX + rowW - 16;
        int nameMax = Math.max(12, removeX - textX - canvas.font().width(tier) - 8);
        String nameShown = GuiDraw.ellipsis(canvas.font(), name, nameMax);
        canvas.text(nameShown, textX, y + 3, 0xFF181818);
        int after = textX + canvas.font().width(nameShown);
        String tierShown = GuiDraw.ellipsis(canvas.font(), tier, Math.max(8, removeX - after - 8));
        canvas.text(tierShown, after, y + 3, 0xFF484848);
        after += canvas.font().width(tierShown) + 4;
        if (after + 20 < removeX) {
            String placeShown = GuiDraw.ellipsis(canvas.font(), place, removeX - after - 2);
            GuiDraw.pill(canvas, after, y + 2, placeShown, 0xFFE4E4E4, 0xFF808080, 0xFF303030);
        }
        canvas.fill(removeX, y + 2, removeX + 13, y + 14, 0xFF000000);
        canvas.fill(removeX + 1, y + 3, removeX + 12, y + 13, 0xFFC6C6C6);
        canvas.text("\u00d7", removeX + 3, y + 3, 0xFF202020);
        hits.add(new Hit(removeX, y + 2, 13, 12, entry.uuid()));
        List<Component> lines = new ArrayList<>();
        int tradeX = textX;
        int tradeMax = removeX - 2;
        boolean truncated = false;
        for (VillagerRoster.LockedOffer offer : entry.trades()) {
            VillagerRoster.WantedCheck check = find(checks, entry.profession(), offer.tradeId());
            String piece = tradePiece(offer, check, indexed.index);
            int color = tradeColor(offer, check, indexed.index);
            lines.add(Component.literal(piece));
            if (truncated) {
                continue;
            }
            String drawn = piece;
            int width = canvas.font().width(drawn);
            if (tradeX + width > tradeMax) {
                drawn = GuiDraw.ellipsis(canvas.font(), piece, tradeMax - tradeX);
                truncated = true;
            }
            if (!drawn.isEmpty() && tradeX < tradeMax) {
                canvas.text(drawn, tradeX, y + 14, color);
                tradeX += canvas.font().width(drawn) + canvas.font().width(" \u00b7 ");
                if (!truncated && tradeX < tradeMax) {
                    canvas.text(" \u00b7", tradeX - canvas.font().width(" \u00b7 "), y + 14, 0xFF505050);
                }
            }
        }
        boolean hoverRow = inside(mouseX, mouseY, wellX + 1, y, rowW, ROW);
        if (hoverRow && (truncated || !nameShown.equals(name) || !tierShown.equals(tier))) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(name + tier + " " + place));
            tip.addAll(lines);
            hover = tip;
        }
    }

    private void paintEmpty(GuiDraw.Canvas canvas, int mouseX, int mouseY) {
        int cardX = wellX + 8;
        int cardY = wellY + 8;
        int cardW = Math.max(20, wellW - 22);
        int cardH = Math.max(20, wellH - 16);
        GuiDraw.beveled(canvas, cardX, cardY, cardW, cardH);
        int mid = cardX + cardW / 2;
        if (cardH > 36) {
            canvas.item(new ItemStack(Items.EMERALD), mid - 26, cardY + 6);
            canvas.item(new ItemStack(Items.ENCHANTED_BOOK), mid - 8, cardY + 6);
            canvas.item(new ItemStack(Items.BOOKSHELF), mid + 10, cardY + 6);
        }
        String[] lines = {
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.empty.title").getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.empty.step1").getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.empty.step2").getString(),
                Component.translatable("text." + VillagerReroller.MOD_ID + ".checklist.empty.step3").getString()
        };
        int[] colors = {0xFF202020, 0xFF484848, 0xFF484848, 0xFF1B5E20};
        int y = cardY + (cardH > 70 ? 28 : 8);
        for (int i = 0; i < lines.length && y < cardY + cardH - 12; i++) {
            int max = cardW - 12;
            String shown = GuiDraw.ellipsis(canvas.font(), lines[i], max);
            int dx = cardX + (cardW - canvas.font().width(shown)) / 2;
            canvas.text(shown, dx, y, colors[i]);
            if (!shown.equals(lines[i]) && inside(mouseX, mouseY, cardX, y - 1, cardW, 11)) {
                hover = List.of(Component.literal(lines[i]));
            }
            y += i == 0 ? 14 : 12;
        }
    }

    private String tradePiece(VillagerRoster.LockedOffer offer, VillagerRoster.WantedCheck check, int index) {
        String name = offer.displayName() == null || offer.displayName().isEmpty() ? offer.tradeId() : offer.displayName();
        if (check != null && check.bestVillagerIndex() == index && offer.paysEmeralds()) {
            if (check.state() == VillagerRoster.WantedState.OVER_CAP) {
                return "! " + name + " (" + offer.effectiveCost() + " em \u00b7 want \u2264" + check.targetMax() + ")";
            }
            if (check.state() == VillagerRoster.WantedState.LOCKED_IN) {
                String cap = check.targetMax() == Integer.MAX_VALUE ? "" : " \u2264" + check.targetMax();
                return "\u2605 " + name + " (" + offer.effectiveCost() + " em" + cap + ")";
            }
        }
        if (!offer.paysEmeralds()) {
            return offer.itemCount() + " " + name + " \u2192 " + offer.effectiveCost() + " em";
        }
        return name + " (" + offer.effectiveCost() + " em)";
    }

    private int tradeColor(VillagerRoster.LockedOffer offer, VillagerRoster.WantedCheck check, int index) {
        if (check != null && check.bestVillagerIndex() == index) {
            if (check.state() == VillagerRoster.WantedState.OVER_CAP) {
                return 0xFF7A4200;
            }
            if (check.state() == VillagerRoster.WantedState.LOCKED_IN) {
                return 0xFF1B5E20;
            }
        }
        return 0xFF303030;
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

    private int neededVillagers(List<VillagerRoster.Entry> roster) {
        List<VillagerRoster.WantedCheck> checks = VillagerRoster.evaluateWanted(
                ProfessionPicks.load(PickFiles.configFile()), roster, null);
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

    private Frame frame() {
        int w = Math.min(380, Math.max(80, this.width - 4));
        int h = Math.min(220, Math.max(80, this.height - 4));
        int left = Math.max(2, (this.width - w) / 2);
        int top = Math.max(2, (this.height - h) / 2);
        return new Frame(left, top, w, h);
    }

    private boolean click(double mouseX, double mouseY) {
        for (Hit hit : hits) {
            if (inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                if (this.minecraft != null) {
                    VillagerRosterClient.roster().remove(VillagerRosterClient.currentWorldKey(this.minecraft), hit.uuid);
                    VillagerRosterClient.roster().save(dev.quarkos.villagerreroller.config.VillagerRosterFiles.configFile());
                }
                return true;
            }
        }
        for (TabHit tab : tabs) {
            if (inside(mouseX, mouseY, tab.x, tab.y, tab.w, tab.h)) {
                filter = tab.filter;
                scroll = 0;
                return true;
            }
        }
        if (inside(mouseX, mouseY, wellX + wellW - 8, wellY, 8, wellH) && maxScroll > 0) {
            dragging = true;
            int travel = Math.max(1, wellH - 29);
            int next = (int) ((mouseY - wellY) / (double) travel * maxScroll);
            scroll = Math.max(0, Math.min(maxScroll, next));
            return true;
        }
        if (needH > 16 && inside(mouseX, mouseY, needX, needY, needW, needH)) {
            draggingNeeded = true;
            return true;
        }
        return false;
    }

    private boolean scrollBy(double mouseX, double mouseY, double vertical) {
        int delta = (int) Math.signum(vertical);
        if (delta == 0) {
            return false;
        }
        if (inside(mouseX, mouseY, wellX, wellY, wellW, wellH)) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - delta));
            return true;
        }
        if (inside(mouseX, mouseY, needX, needY, needW, needH)) {
            neededScroll = Math.max(0, neededScroll - delta);
            return true;
        }
        return false;
    }

    private boolean drag(double mouseY) {
        if (!dragging) {
            return false;
        }
        int travel = Math.max(1, wellH - 29);
        scroll = Math.max(0, Math.min(maxScroll, (int) ((mouseY - wellY) / (double) travel * maxScroll)));
        return true;
    }

    //? if <1.21.9 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && click(mouseX, mouseY)) {
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
        dragging = false;
        draggingNeeded = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AutomatorKeys.CHECKLIST != null && AutomatorKeys.CHECKLIST.matches(keyCode, scanCode)
                && !(getFocused() instanceof EditBox)) {
            VillagerReroller.openChecklist(this.minecraft);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && click(event.x(), event.y())) {
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
        dragging = false;
        draggingNeeded = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AutomatorKeys.CHECKLIST != null && AutomatorKeys.CHECKLIST.matches(event)
                && !(getFocused() instanceof EditBox)) {
            VillagerReroller.openChecklist(this.minecraft);
            return true;
        }
        return super.keyPressed(event);
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

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int widgetX(net.minecraft.client.gui.components.AbstractWidget widget) {
        //? if <1.19.3 {
        /*return widget.x;
        *///?} else {
        return widget.getX();
        //?}
    }

    private static int widgetW(net.minecraft.client.gui.components.AbstractWidget widget) {
        return widget.getWidth();
    }

    private enum Filter {
        ALL,
        NEEDED,
        LIBRARIAN,
        OTHER
    }

    private record Frame(int left, int top, int w, int h) {
    }

    private record Indexed(int index, VillagerRoster.Entry entry) {
    }

    private record Hit(int x, int y, int w, int h, String uuid) {
    }

    private record TabHit(Filter filter, int x, int y, int w, int h, String full, String shown) {
    }
}
