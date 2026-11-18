package dev.quarkos.villagerreroller.harness;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.SearchBox;
import dev.quarkos.villagerreroller.picker.TradeBrowserScreen;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeSession;
//? if >=1.21.9 {
import net.minecraft.client.input.InputWithModifiers;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

/** Drives the same widgets in the release-jar harness and the client gametests. */
public final class BrowserHarness {
    private BrowserHarness() {
    }

    public static TradeBrowserScreen open(Minecraft client) {
        click(client, "browser.title");
        return browser(client);
    }

    public static TradeBrowserScreen browser(Minecraft client) {
        if (McBridge.currentScreen(client) instanceof TradeBrowserScreen browser) {
            return browser;
        }
        throw new IllegalStateException("Expected the trade browser, saw " + McBridge.currentScreen(client));
    }

    public static SearchBox search(Minecraft client) {
        return browser(client).children().stream().filter(SearchBox.class::isInstance).map(SearchBox.class::cast)
                .findFirst().orElseThrow(() -> new IllegalStateException("No book search in the browser"));
    }

    public static void click(Minecraft client, String key) {
        clickLabel(client, GuiDraw.message(key).getString());
    }

    public static void clickLabel(Minecraft client, String label) {
        Button button = find(McBridge.currentScreen(client), label);
        if (button == null || !button.active) {
            throw new IllegalStateException("Button missing or disabled: " + label + " on " + McBridge.currentScreen(client));
        }
        press(button);
    }

    public static Button find(Screen screen, String label) {
        if (screen == null) {
            return null;
        }
        return McBridge.buttons(screen).stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.visible && button.getMessage().getString().equals(label))
                .findFirst().orElse(null);
    }

    private static void press(Button button) {
        //? if <1.21.9 {
        /*button.onPress();
        *///?} else {
        button.onPress(new InputWithModifiers() {
            @Override
            public int input() {
                return 0;
            }

            @Override
            public int modifiers() {
                return 0;
            }
        });
        //?}
    }

    public static void findItem(Minecraft client, String label) {
        if (find(browser(client), label) == null) {
            Button back = find(browser(client), GuiDraw.message("ui.previous").getString());
            while (back != null && back.active) {
                press(back);
            }
        }
        while (find(browser(client), label) == null) {
            Button next = find(browser(client), GuiDraw.message("ui.next").getString());
            if (next == null || !next.active) {
                throw new IllegalStateException("Missing item in browser: " + label);
            }
            press(next);
        }
        clickLabel(client, label);
    }

    /** Moves from the item grid to the price of one specific versioned trade. */
    public static void choose(Minecraft client, TradeEntry entry) {
        if (entry.id.startsWith("enchant:")) {
            findItem(client, new ItemStack(Items.ENCHANTED_BOOK).getHoverName().getString());
            search(client).setValue(entry.name);
            clickLabel(client, entry.name);
        } else {
            findItem(client, entry.name);
            if (find(browser(client), GuiDraw.message("browser.add").getString()) == null
                    && find(browser(client), GuiDraw.message("browser.added").getString()) == null) {
                clickLabel(client, entry.name);
            }
        }
    }

    public static void clearPicks(Minecraft client) {
        Button remove;
        while ((remove = find(browser(client), "×")) != null) {
            press(remove);
            if (find(browser(client), "×") == remove) {
                throw new IllegalStateException("Could not remove a saved browser pick");
            }
        }
    }

    public static void pick(Minecraft client, TradeEntry entry, int ceiling) {
        choose(client, entry);
        if (entry.paysEmeralds && ceiling == TradeSession.ANY_PRICE) {
            click(client, "any");
        } else if (entry.paysEmeralds) {
            for (int price = entry.emeraldMax; price > ceiling; price--) {
                clickLabel(client, "−");
            }
        }
        click(client, "browser.add");
        click(client, "button.back");
        if (entry.id.startsWith("enchant:")) {
            click(client, "button.back");
        }
    }

    /** Includes absent fixed offers and several different enchantments for a timely real reroll. */
    public static void pickMissing(Minecraft client, MerchantOffers current) {
        pickMissing(client, current, -1);
    }

    /** The Hall test supplies a distinct finite cap for the first book in each queue slot. */
    public static void pickMissing(Minecraft client, MerchantOffers current, int bookPriceOffset) {
        clearPicks(client);
        List<TradeEntry> missing = TradeSession.entries().stream().filter(entry ->
                current.stream().noneMatch(offer -> entry.matches(offer, TradeSession.ANY_PRICE))).toList();
        List<TradeEntry> fixed = missing.stream().filter(entry -> !entry.id.startsWith("enchant:")).toList();
        for (TradeEntry entry : fixed) {
            pick(client, entry, TradeSession.ANY_PRICE);
        }
        List<TradeEntry> books = missing.stream().filter(entry -> entry.id.startsWith("enchant:")).toList();
        if (bookPriceOffset >= 0 && !books.isEmpty()) {
            TradeEntry capped = books.get(0);
            pick(client, capped, Math.min(capped.emeraldMax, capped.emeraldMin + bookPriceOffset));
            books = books.subList(1, books.size());
        }
        if (fixed.isEmpty()) {
            for (TradeEntry entry : books.subList(0, Math.min(32, books.size()))) {
                pick(client, entry, TradeSession.ANY_PRICE);
            }
        }
        if (!TradeSession.hasSelection() || TradeSession.offerMatchesSelection(current)) {
            throw new IllegalStateException("The browser did not select a target that requires a reroll");
        }
    }
}
