package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.InteractionResult;

public final class VillagerReroller implements ClientModInitializer {
    public static final String MOD_ID = "villager_reroller";

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
            //? if 1.21.1 {
            /*return;
            *///?} elif >=26.1 {
            /*return;
            *///?} else {
            if (screen instanceof MerchantScreen merchant) {
                dev.quarkos.villagerreroller.picker.TradePickerUi.attach(client, merchant);
            }
            //?}
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            //? if 1.21.1 {
            /*if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                CycleAutomator.toggle();
            }
            CycleAutomator.tick(client);
            *///?} elif >=26.1 {
            /*if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                CycleAutomator.toggle();
            }
            CycleAutomator.tick(client);
            *///?} else {
            if (McBridge.currentScreen(client) instanceof PauseScreen && dev.quarkos.villagerreroller.picker.JobSiteReroller.isRunning()) {
                dev.quarkos.villagerreroller.picker.JobSiteReroller.stop(dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.STOPPED);
                client.setScreen(null);
            }
            if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                dev.quarkos.villagerreroller.picker.JobSiteReroller.onToggleKey();
            }
            dev.quarkos.villagerreroller.picker.JobSiteReroller.tick(client);
            //?}
        });
    }
}
