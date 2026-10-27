//? if fabric {
package dev.quarkos.villagerreroller.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
            return parent -> null;
        }
        return ForgeConfigScreen::new;
    }
}
//?}
