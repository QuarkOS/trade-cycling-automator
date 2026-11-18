package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.picker.HallHighlights;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.MerchantTradeButton;
import dev.quarkos.villagerreroller.picker.TradeBrowserScreen;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
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
    private static int checklistTick = Integer.MIN_VALUE;

    public static void bootstrap() {
        AutomatorConfig.get();
        LibrarianBookPrices.get();
        AutomatorKeys.register();
    }

    //? if fabric {
    @Override
    public void onInitializeClient() {
        bootstrap();
        dev.quarkos.villagerreroller.net.FabricClientChannels.register();
        dev.quarkos.villagerreroller.net.ServerRerollClient.bind(new dev.quarkos.villagerreroller.net.ServerRerollClient.Sender() {
            @Override
            public boolean canSend() {
                return dev.quarkos.villagerreroller.net.FabricClientChannels.canSend()
                        || dev.quarkos.villagerreroller.net.ServerRerollClient.available();
            }

            @Override
            public void send(byte[] data) {
                dev.quarkos.villagerreroller.net.FabricClientChannels.send(data);
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                LibrarianTracker.remember(entity);
            }
            return InteractionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> onScreen(client, screen));

        dev.quarkos.villagerreroller.picker.TradeHallHud.register();
        ClientTickEvents.END_CLIENT_TICK.register(VillagerReroller::onClientTick);
    }
    //?}

    public static void onClientTick(Minecraft client) {
        // Every release jar keeps this call. start() returns immediately unless
        // -Dvillager_reroller.harness=true, which is how tools/validate drives the jar.
        dev.quarkos.villagerreroller.harness.ServerJoinHarness.tick(client);
        dev.quarkos.villagerreroller.harness.ClientHarness121.start(client);
        HallHighlights.tick(client);
        MerchantTradeButton.tick(client);
        boolean pauseScreen = McBridge.currentScreen(client) instanceof PauseScreen;
        if (pauseScreen
                && (dev.quarkos.villagerreroller.picker.JobSiteReroller.isRunning()
                || dev.quarkos.villagerreroller.picker.TradeHall.isActive())) {
            if (dev.quarkos.villagerreroller.picker.TradeHall.isActive()) {
                dev.quarkos.villagerreroller.picker.TradeHall.requestStop();
            } else {
                dev.quarkos.villagerreroller.picker.JobSiteReroller.stop(dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.STOPPED);
            }
            McBridge.setScreen(client, null);
        }
        if (pauseScreen && HallHighlights.clear() && McBridge.currentScreen(client) instanceof PauseScreen) {
            McBridge.setScreen(client, null);
        }
        if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE != null && AutomatorKeys.TOGGLE.consumeClick()) {
            openBrowser(client);
        }
        if (AutomatorKeys.CONFIG != null && AutomatorKeys.CONFIG.consumeClick()
                && !(McBridge.currentScreen(client) instanceof ForgeConfigScreen)) {
            McBridge.setScreen(client, new ForgeConfigScreen(McBridge.currentScreen(client)));
        }
        if (AutomatorKeys.CHECKLIST != null && AutomatorKeys.CHECKLIST.consumeClick()) {
            openChecklist(client);
        }
        dev.quarkos.villagerreroller.picker.JobSiteReroller.tick(client);
        dev.quarkos.villagerreroller.picker.TradeHall.tick(client);
    }

    public static void openBrowser(Minecraft client) {
        if (JobSiteReroller.isRunning() || TradeHall.isActive()) {
            JobSiteReroller.onToggleKey();
            return;
        }
        Screen screen = McBridge.currentScreen(client);
        if (screen instanceof MerchantScreen merchant) {
            MerchantTradeButton.open(client, merchant);
        } else if (screen == null && client.level != null) {
            McBridge.setScreen(client, new TradeBrowserScreen(null));
        }
    }

    /**
     * Opens the checklist from the world, the trade screen, or the hall.
     * Pressing it again returns to the previous screen. One press is handled once.
     */
    public static void openChecklist(Minecraft client) {
        if (client == null) {
            return;
        }
        int tick = client.player == null ? 0 : client.player.tickCount;
        if (tick == checklistTick) {
            return;
        }
        checklistTick = tick;
        Screen screen = McBridge.currentScreen(client);
        if (screen != null && screen.getFocused() instanceof EditBox) {
            return;
        }
        if (screen instanceof dev.quarkos.villagerreroller.picker.MyVillagersScreen page) {
            McBridge.setScreen(client, page.parent());
            return;
        }
        if (screen == null
                || screen instanceof MerchantScreen
                || screen instanceof dev.quarkos.villagerreroller.picker.TradeHallScreen) {
            McBridge.setScreen(client, new dev.quarkos.villagerreroller.picker.MyVillagersScreen(screen));
        }
    }

    public static void onScreen(Minecraft client, Screen screen) {
        McBridge.hookScreenKeys(screen);
        if (screen instanceof MerchantScreen merchant) {
            MerchantTradeButton.attach(client, merchant);
        }
    }

    public static void remember(net.minecraft.world.entity.Entity entity) {
        LibrarianTracker.remember(entity);
    }
}
