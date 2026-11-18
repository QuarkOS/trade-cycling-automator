package dev.quarkos.villagerreroller.mixin;

import dev.quarkos.villagerreroller.picker.HallHighlights;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HallOutlineColorMixin {
    //? if forge && !neoforge && <1.20.5 {
    /*@Inject(method = {"getTeamColor()I", "m_19876_()I"}, at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    *///?} else {
    @Inject(method = "getTeamColor()I", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void villagerReroller$hallOutlineColor(CallbackInfoReturnable<Integer> result) {
        Integer color = HallHighlights.color((Entity) (Object) this);
        if (color != null) {
            result.setReturnValue(color & 0xFFFFFF);
        }
    }
}
