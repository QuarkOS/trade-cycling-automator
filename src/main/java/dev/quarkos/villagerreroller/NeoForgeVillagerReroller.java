//? if neoforge {
/*
package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.server.VillagerRerollerServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
//? if <1.20.4 {
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
//?}

@Mod(VillagerReroller.MOD_ID)
public final class NeoForgeVillagerReroller {
    //? if <1.20.4 {
    public NeoForgeVillagerReroller() {
        register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    //?} else {
    public NeoForgeVillagerReroller(IEventBus modBus) {
        register(modBus);
    }
    //?}

    private void register(IEventBus modBus) {
        VillagerRerollerServer.bootstrap(modBus);
        if (client()) {
            NeoForgeClientHooks.init(modBus);
        }
    }

    private static boolean client() {
        //? if <1.21.9 {
        return FMLEnvironment.dist.isClient();
        //?} else {
        return FMLEnvironment.getDist().isClient();
        //?}
    }
}
*///?}
