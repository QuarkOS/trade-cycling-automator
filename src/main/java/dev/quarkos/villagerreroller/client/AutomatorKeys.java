package dev.quarkos.villagerreroller.client;

import net.minecraft.client.KeyMapping;

public final class AutomatorKeys {
    public static KeyMapping TOGGLE;
    public static KeyMapping CONFIG;

    private AutomatorKeys() {
    }

    public static void register() {
        TOGGLE = McBridge.registerToggle();
        CONFIG = McBridge.registerConfig();
    }
}
