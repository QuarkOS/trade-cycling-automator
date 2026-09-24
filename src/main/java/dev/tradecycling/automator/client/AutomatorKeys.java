package dev.tradecycling.automator.client;

import net.minecraft.client.KeyMapping;

public final class AutomatorKeys {
    public static KeyMapping TOGGLE;

    private AutomatorKeys() {
    }

    public static void register() {
        TOGGLE = McBridge.registerToggle();
    }
}
