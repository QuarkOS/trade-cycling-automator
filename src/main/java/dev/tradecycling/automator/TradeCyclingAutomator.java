package dev.tradecycling.automator;

import dev.tradecycling.automator.client.AutomatorKeys;
import dev.tradecycling.automator.client.CycleAutomator;
import dev.tradecycling.automator.client.LibrarianTracker;
import dev.tradecycling.automator.client.McBridge;
import dev.tradecycling.automator.config.AutomatorConfig;
import dev.tradecycling.automator.price.LibrarianBookPrices;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.InteractionResult;

public final class TradeCyclingAutomator implements ClientModInitializer {
    public static final String MOD_ID = "trade-cycling-automator";

    @Override
    public void onInitializeClient() {
        AutomatorConfig.get();
        LibrarianBookPrices.get();
        AutomatorKeys.register();

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                LibrarianTracker.remember(entity);
            }
            return InteractionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            McBridge.hookScreenKeys(screen);
            //? if <1.21.11 {
            /*return;
            *///?} else {
            //? if >=26.1 {
            /*return;
            *///?} else {
            if (screen instanceof MerchantScreen merchant) {
                dev.tradecycling.automator.picker.TradePickerUi.attach(client, merchant);
            }
            //?}
            //?}
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            //? if <1.21.11 {
            /*if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                CycleAutomator.toggle();
            }
            CycleAutomator.tick(client);
            *///?} else {
            //? if >=26.1 {
            /*if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                CycleAutomator.toggle();
            }
            CycleAutomator.tick(client);
            *///?} else {
            if (McBridge.currentScreen(client) instanceof PauseScreen && dev.tradecycling.automator.picker.JobSiteReroller.isRunning()) {
                dev.tradecycling.automator.picker.JobSiteReroller.stop(dev.tradecycling.automator.picker.JobSiteReroller.StopReason.STOPPED);
                client.setScreen(null);
            }
            if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                dev.tradecycling.automator.picker.JobSiteReroller.onToggleKey();
            }
            dev.tradecycling.automator.picker.JobSiteReroller.tick(client);
            //?}
            //?}
        });
    }
}
