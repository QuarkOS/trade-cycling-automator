plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

stonecutter parameters {
    // Root branch id is empty. Named branches use their id ("forge").
    val loaderName = branch.id.ifEmpty { "fabric" }
    constants.match(loaderName, "fabric", "forge")
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
    }
}
