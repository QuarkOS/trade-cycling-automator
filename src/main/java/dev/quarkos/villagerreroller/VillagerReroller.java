package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
//? if forge {
/*import net.minecraft.world.entity.Entity;
*///?} else {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
//?}

//? if forge {
/*public final class VillagerReroller {
*///?} else {
public final class VillagerReroller implements ClientModInitializer {
//?}
    public static final String MOD_ID = "villager_reroller";

    public static void bootstrap() {
        AutomatorConfig.get();
        LibrarianBookPrices.get();
        AutomatorKeys.register();
    }

    //? if fabric {
    @Override
    public void onInitializeClient() {
        bootstrap();

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                LibrarianTracker.remember(entity);
            }
            return InteractionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> onScreen(client, screen));

        ClientTickEvents.END_CLIENT_TICK.register(VillagerReroller::onClientTick);
    }
    //?}

    public static void onClientTick(Minecraft client) {
        //? if <1.21.5 {
        /*dev.quarkos.villagerreroller.harness.ClientHarness121.start(client);
        *///?}
        if (McBridge.currentScreen(client) instanceof PauseScreen && dev.quarkos.villagerreroller.picker.JobSiteReroller.isRunning()) {
            dev.quarkos.villagerreroller.picker.JobSiteReroller.stop(dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.STOPPED);
            McBridge.setScreen(client, null);
        }
        if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE != null && AutomatorKeys.TOGGLE.consumeClick()) {
            dev.quarkos.villagerreroller.picker.JobSiteReroller.onToggleKey();
        }
        dev.quarkos.villagerreroller.picker.JobSiteReroller.tick(client);
    }

    public static void onScreen(Minecraft client, Screen screen) {
        McBridge.hookScreenKeys(screen);
        if (screen instanceof MerchantScreen merchant) {
            dev.quarkos.villagerreroller.picker.TradePickerUi.attach(client, merchant);
        }
    }

    public static void remember(net.minecraft.world.entity.Entity entity) {
        LibrarianTracker.remember(entity);
    }
}
