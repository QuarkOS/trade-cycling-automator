//? if forge {
/*
package dev.quarkos.villagerreroller;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(VillagerReroller.MOD_ID)
public final class ForgeVillagerReroller {
    public ForgeVillagerReroller() {
        dev.quarkos.villagerreroller.server.VillagerRerollerServer.bootstrap();
        if (FMLEnvironment.dist.isClient()) {
            ForgeClientHooks.init();
        }
    }
}
*///?} else {
package dev.quarkos.villagerreroller;

public final class ForgeVillagerReroller {
    private ForgeVillagerReroller() {
    }
}
//?}
