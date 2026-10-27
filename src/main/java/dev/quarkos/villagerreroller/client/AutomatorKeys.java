package dev.quarkos.villagerreroller.client;

import net.minecraft.client.KeyMapping;

public final class AutomatorKeys {
    public static KeyMapping TOGGLE;
    public static KeyMapping CONFIG;
    public static KeyMapping CHECKLIST;

    private AutomatorKeys() {
    }

    public static void register() {
        TOGGLE = McBridge.registerToggle();
        CONFIG = McBridge.registerConfig();
        CHECKLIST = McBridge.registerChecklist();
    }
}
