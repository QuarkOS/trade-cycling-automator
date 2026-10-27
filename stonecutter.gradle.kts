plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

stonecutter parameters {
    // Root branch id is empty. Named branches use their id ("forge").
    val loaderName = branch.id.ifEmpty { "fabric" }
    constants.match(loaderName, "fabric", "forge", "neoforge")
    // NeoForge compiles the existing Forge-gated sources. The Forge entrypoint
    // stays out via `forge && !neoforge`. Package and call sites that are not
    // a rename are rewritten below, and only on this branch.
    if (loaderName == "neoforge") {
        constants.put("forge", true)
    }
    replacements {
        // Active sources are written for 1.21.11, which already calls the class Identifier.
        string(current.parsed < "1.21.11") {
            replace("net.minecraft.world.entity.npc.villager.", "net.minecraft.world.entity.npc.")
            replace("Identifier", "ResourceLocation")
            replace(".identifier()", ".location()")
        }
        string(current.parsed >= "26.1") {
            replace("net.fabricmc.fabric.api.client.keybinding.v1", "net.fabricmc.fabric.api.client.keymapping.v1")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("registerKeyBinding", "registerKeyMapping")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("renderWidget", "extractWidgetRenderState")
        }
        if (branch.id == "neoforge") {
            string(true) {
                replace("net.minecraftforge.fml.loading.FMLPaths", "net.neoforged.fml.loading.FMLPaths")
                replace("net.minecraftforge.fml.loading.FMLLoader", "net.neoforged.fml.loading.FMLLoader")
                replace("net.minecraftforge.fml.ModList", "net.neoforged.fml.ModList")
                replace("net.minecraftforge.client.gui.LoadingErrorScreen", "net.neoforged.neoforge.client.gui.LoadingErrorScreen")
                replace("return ModList.isLoaded(MOD_ID);", "return ModList.get().isLoaded(MOD_ID);")
                replace(
                    "import de.maxhenkel.tradecycling.ForgeTradeCyclingMod;",
                    "// NeoForge sends through NeoForgeTradeCycling instead of ForgeTradeCyclingMod.",
                )
                replace(
                    "ForgeTradeCyclingMod.CYCLE_TRADES_CHANNEL.send(new FriendlyByteBuf(Unpooled.buffer()), handler.getConnection());",
                    "dev.quarkos.villagerreroller.compat.NeoForgeTradeCycling.send(handler);",
                )
            }
            // FancyModLoader 10+ (Minecraft 1.21.9) moved versionInfo() onto the current loader instance.
            string(current.parsed >= "1.21.9") {
                replace("FMLLoader.versionInfo()", "FMLLoader.getCurrent().getVersionInfo()")
            }
        }
    }
}
