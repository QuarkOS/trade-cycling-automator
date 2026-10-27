package dev.quarkos.villagerreroller.config;

//? if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?} else {
import net.fabricmc.loader.api.FabricLoader;
//?}

import java.nio.file.Path;

/**
 * Client file for the trade picker's saved targets. One file for every world.
 */
public final class PickFiles {
    public static final String FILE_NAME = "villager_reroller-picks.json";

    private PickFiles() {
    }

    public static Path configFile() {
        //? if forge {
        /*return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        *///?} else {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        //?}
    }
}
