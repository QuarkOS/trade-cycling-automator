package dev.quarkos.villagerreroller.config;

//? if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?} else {
import net.fabricmc.loader.api.FabricLoader;
//?}

import java.nio.file.Path;

/**
 * Per-world checklist of villagers whose trades are locked.
 * Stored beside the picks file in the loader config directory.
 */
public final class VillagerRosterFiles {
    public static final String FILE_NAME = "villager_reroller-villagers.json";

    private VillagerRosterFiles() {
    }

    public static Path configFile() {
        //? if forge {
        /*return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        *///?} else {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        //?}
    }
}
