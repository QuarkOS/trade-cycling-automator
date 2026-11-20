package dev.quarkos.villagerreroller.mixin;

import dev.quarkos.villagerreroller.picker.HallHighlights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class HallGlowingMixin {
    //? if forge && !neoforge && <1.20.5 {
    /*// These Forge release jars use SRG, while userdev uses Mojang names.
    @Inject(method = {"shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z",
            "m_91314_(Lnet/minecraft/world/entity/Entity;)Z"},
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    *///?} else {
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void villagerReroller$showHallOutline(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (HallHighlights.color(entity) != null) {
            result.setReturnValue(true);
        }
    }
}
