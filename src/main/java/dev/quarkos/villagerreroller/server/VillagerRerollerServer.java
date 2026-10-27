//? if neoforge {
/*
package dev.quarkos.villagerreroller.server;

import dev.quarkos.villagerreroller.net.NeoForgeChannels;
import net.neoforged.bus.api.IEventBus;

public final class VillagerRerollerServer {
    private VillagerRerollerServer() {
    }

    public static void bootstrap(IEventBus modBus) {
        NeoForgeChannels.register(modBus);
        DedicatedProof.hook();
    }
}
*///?} elif forge {
/*
package dev.quarkos.villagerreroller.server;

import dev.quarkos.villagerreroller.net.ForgeChannels;

public final class VillagerRerollerServer {
    private VillagerRerollerServer() {
    }

    public static void bootstrap() {
        ForgeChannels.registerServer();
        DedicatedProof.hook();
    }
}
*///?} else {
package dev.quarkos.villagerreroller.server;

import dev.quarkos.villagerreroller.net.FabricChannels;
import net.fabricmc.api.ModInitializer;

public final class VillagerRerollerServer implements ModInitializer {
    @Override
    public void onInitialize() {
        bootstrap();
    }

    public static void bootstrap() {
        FabricChannels.registerServer();
        DedicatedProof.hook();
    }
}
//?}
