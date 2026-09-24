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

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> McBridge.hookScreenKeys(screen));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (McBridge.currentScreen(client) == null && AutomatorKeys.TOGGLE.consumeClick()) {
                CycleAutomator.toggle();
            }
            CycleAutomator.tick(client);
        });
    }
}
