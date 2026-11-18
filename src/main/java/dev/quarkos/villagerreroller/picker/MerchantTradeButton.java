package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.net.ServerRerollClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
//? if <1.20.2 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
//?}
//? if >=1.19.3 {
import net.minecraft.client.gui.components.Tooltip;
//?}
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

import static dev.quarkos.villagerreroller.picker.GuiDraw.message;

/** Connects the vanilla merchant's book button to the browser. */
public final class MerchantTradeButton {
    private static final int MERCHANT_WIDTH = 276;
    private static MerchantScreen attached;
    private static Button button;
    private static boolean inspected;

    private MerchantTradeButton() {
    }

    public static void attach(Minecraft client, MerchantScreen screen) {
        attached = screen;
        inspected = false;
        LibrarianTracker.bindNearest(client);
        int x = McBridge.merchantLeft(screen) + MERCHANT_WIDTH - 24;
        int y = McBridge.merchantTop(screen) + 4;
        //? if <1.19.3 {
        /*button = new ImageButton(x, y, 20, 18, 0, 0, 19,
                new Identifier("minecraft", "textures/gui/recipe_button.png"), 256, 256,
                pressed -> press(client, screen),
                (widget, pose, mouseX, mouseY) -> screen.renderTooltip(pose, widget.getMessage(), mouseX, mouseY),
                message("browser.title"));
        *///?} elif <1.20.2 {
        /*button = new ImageButton(x, y, 20, 18, 0, 0, 19,
                new Identifier("minecraft", "textures/gui/recipe_button.png"), 256, 256,
                pressed -> press(client, screen), message("browser.title"));
        *///?} else {
        button = new ImageButton(x, y, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                pressed -> press(client, screen), message("browser.title"));
        //?}
        setLabel(button, currentLabel());
        McBridge.buttons(screen).add(button);
    }

    public static void open(Minecraft client, MerchantScreen screen) {
        inspect(client, screen);
        McBridge.setScreen(client, new TradeBrowserScreen(screen, trader(client, screen)));
    }

    private static void press(Minecraft client, MerchantScreen screen) {
        if (stopFromMerchant()) {
            JobSiteReroller.onToggleKey();
        } else {
            open(client, screen);
        }
    }

    private static boolean stopFromMerchant() {
        return TradeHall.isActive() || ServerRerollClient.isRunning();
    }

    private static Villager trader(Minecraft client, MerchantScreen screen) {
        if (screen.getMenu().getTraderLevel() <= 0 && !screen.getMenu().getOffers().isEmpty()) {
            return null;
        }
        return LibrarianTracker.current(client);
    }

    public static void tick(Minecraft client) {
        if (attached == null) {
            return;
        }
        if (McBridge.currentScreen(client) != attached) {
            attached = null;
            button = null;
            return;
        }
        Component label = currentLabel();
        if (!label.equals(button.getMessage())) {
            setLabel(button, label);
        }
        inspect(client, attached);
    }

    private static void inspect(Minecraft client, MerchantScreen screen) {
        // The menu often arrives before its offers and XP.
        if (!inspected && !TradeBrowserScreen.busy() && !screen.getMenu().getOffers().isEmpty()
                && TradeHall.menuLocked(screen.getMenu())) {
            VillagerRosterClient.inspectOpenMerchant(client, trader(client, screen), screen.getMenu());
            inspected = true;
        }
    }

    private static Component currentLabel() {
        return message(stopFromMerchant() ? "button.stop" : "browser.title");
    }

    private static void setLabel(Button button, Component label) {
        button.setMessage(label);
        //? if >=1.19.3 {
        button.setTooltip(Tooltip.create(label));
        //?}
    }
}
