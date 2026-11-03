package dev.quarkos.villagerreroller.mixin;

import dev.quarkos.villagerreroller.picker.TradePickerUi;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MerchantScreen paints its background, trade icons, and experience bar from
 * {@code (width - imageWidth) / 2} every frame, and ignores {@code leftPos}.
 * Moving {@code leftPos} therefore slid the title, slots, and offer buttons
 * while the texture and the green experience bar stayed centered.
 * These redirects make that centering math follow {@code leftPos} and {@code topPos}.
 * The handler argument is {@link MerchantScreen} because the field read's owner
 * in bytecode is MerchantScreen, not Screen.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenShiftMixin extends AbstractContainerScreen<MerchantMenu> {
    static {
        TradePickerUi.merchantBackgroundFollowsLeftPos = true;
    }

    protected MerchantScreenShiftMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    //? if >=26.1 {
    /*@Redirect(method = {"extractBackground", "extractContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"extractBackground", "extractContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    *///?} elif >=1.21.9 {
    @Redirect(method = {"renderBg", "renderContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"renderBg", "renderContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    //?} else {
    /*@Redirect(method = {"renderBg", "render", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"renderBg", "render", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    *///?}
}
