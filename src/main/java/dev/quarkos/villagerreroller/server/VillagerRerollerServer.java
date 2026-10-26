//? if forge {
/*package dev.quarkos.villagerreroller.server;

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
